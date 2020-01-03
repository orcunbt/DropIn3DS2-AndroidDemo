package com.example.orcunbt.dropinui3ds2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.internal.HttpClient;
import com.braintreepayments.api.models.ThreeDSecureAdditionalInformation;
import com.braintreepayments.api.models.ThreeDSecurePostalAddress;
import com.braintreepayments.api.models.ThreeDSecureRequest;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    final String get_token = "https://hwsrv-610555.hostwindsdns.com/BTOrcun/clientToken.php";
    final String transaction_url = "https://hwsrv-610555.hostwindsdns.com/BTOrcun/createTransaction.php";
    private static final int DROP_IN_REQUEST = 1;
    String token, nonce, amount;
    Button dropInButton, testCardsButton;
    ProgressBar spinner;
    TextView eventTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI stuff
        dropInButton = (Button) findViewById(R.id.button);
        testCardsButton = (Button) findViewById(R.id.button2);
        spinner = (ProgressBar) findViewById(R.id.progressBar);
        eventTextView = (TextView) findViewById(R.id.textView2);
        eventTextView.setMovementMethod(new ScrollingMovementMethod());
        spinner.setVisibility(View.INVISIBLE);

        // 3DS2 test cards
        testCardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String url = "https://developers.braintreepayments.com/guides/3d-secure/testing-go-live/#testing";

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });

        // Launch Drop-In UI
        dropInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spinner.setVisibility(View.VISIBLE);
                // Invoke HTTP request to get a client-token and launch Drop-In UI
                new HttpRequest().execute();
            }
        });

    }

    // Initialize DropInRequest with 3DS2 details
    protected void onAuthorizationFetched() {
        spinner.setVisibility(View.INVISIBLE);
        ThreeDSecurePostalAddress address = new ThreeDSecurePostalAddress()
                .givenName("Jill") // ASCII-printable characters required, else will throw a validation error
                .surname("Doe") // ASCII-printable characters required, else will throw a validation error
                .phoneNumber("5551234567")
                .streetAddress("555 Smith St")
                .extendedAddress("#2")
                .locality("Chicago")
                .region("IL")
                .postalCode("12345")
                .countryCodeAlpha2("US");

        // For best results, provide as many additional elements as possible.
        ThreeDSecureAdditionalInformation additionalInformation = new ThreeDSecureAdditionalInformation()
                .shippingAddress(address);

        ThreeDSecureRequest threeDSecureRequest = new ThreeDSecureRequest()
                .amount("199.00")
                .email("test@email.com")
                .billingAddress(address)
                .versionRequested(ThreeDSecureRequest.VERSION_2)
                .additionalInformation(additionalInformation);

        DropInRequest dropInRequest = new DropInRequest()
                .requestThreeDSecureVerification(true)
                .disablePayPal()
                .clientToken(token)
                .threeDSecureRequest(threeDSecureRequest);
        startActivityForResult(dropInRequest.getIntent(this), DROP_IN_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DROP_IN_REQUEST) {
            if (resultCode == RESULT_OK) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                Log.d("Nonce", result.getPaymentMethodNonce().getNonce().toString());
                nonce = result.getPaymentMethodNonce().getNonce();

                //Toast toast = Toast.makeText(MainActivity.this, "Successfully generated a nonce. \n" + result.getPaymentMethodNonce().getNonce().toString(), Toast.LENGTH_SHORT);
                //toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 0);
                //toast.show();

                // Send the nonce to server
                sendPaymentDetails();

            } else if (resultCode == RESULT_CANCELED) {
                // the user canceled
            } else {
                // handle errors here, an exception may be available in
                Exception error = (Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR);
            }
        }
    }

    // Function for sending the nonce to server
    private void sendPaymentDetails() {
        spinner.setVisibility(View.VISIBLE);
        eventTextView.setText("Sending nonce " + nonce + " to server...");
        eventTextView.setVisibility(View.VISIBLE);

        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.POST, transaction_url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        spinner.setVisibility(View.INVISIBLE);
                        eventTextView.setText(Html.fromHtml(response));
                        if(response.contains("Successful"))
                        {
                            Toast toast = Toast.makeText(MainActivity.this, "Transaction successful", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 0);
                            toast.show();
                            Log.d("Success", "Final Response: " + response.toString());
                        }
                        else {
                            Toast toast = Toast.makeText(MainActivity.this, "Transaction failed", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 0);
                            toast.show();
                            Log.d("Fail", "Final Response: " + response.toString());
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("mylog", "Volley error : " + error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {

                // Add nonce to HTTP request to create a transaction on the server-side
                Map<String, String> params = new HashMap<String, String>();
                params.put("payment_method_nonce", nonce);
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/x-www-form-urlencoded");
                return params;
            }
        };

        // This prevents multiple HTTP requests
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(stringRequest);
    }

    // HttpRequest class to get a client-token
    private class HttpRequest extends AsyncTask {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected Object doInBackground(Object[] objects) {
            HttpClient client = new HttpClient();
            client.get(get_token, new HttpResponseCallback() {
                @Override
                public void success(String responseBody) {
                    Log.d("mylog", responseBody);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(MainActivity.this, "Successfully retrieved a client-token", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 0);
                            toast.show();
                        }
                    });
                    token = responseBody;
                    onAuthorizationFetched();
                }

                @Override
                public void failure(Exception exception) {
                    final Exception ex = exception;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast toast = Toast.makeText(MainActivity.this, "Failed to get a client-token: " + ex.toString(), Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 0);
                            toast.show();
                        }
                    });
                }
            });
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);

        }
    }
}
