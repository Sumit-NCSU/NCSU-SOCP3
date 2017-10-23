package com.example.srivassumit.referralsclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private SeekBar cookQueryBar, houseQueryBar, educateQueryBar, entertainQueryBar;
    private SeekBar cookingAnsBar, houseAnsBar, educateAnsBar, entertainAnsBar;
    private TextView txtCookingV, txtHouseV, txtEducationV, txtEntertainV;
    private TextView txtCookingQV, txtHouseQV, txtEducationQV, txtEntertainQV;
    private Button queryBtn;

    private static final String LOG_NAME="MainActivity";

    private final String defaultActor = "default/";
    private final String urlName = "query/";
    private final String hostName = "http://10.0.2.2:9000/";
    //private final String hostName = "https://referral-server-9x.herokuapp.com/";

    private double[] query = new double[4];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cookQueryBar = (SeekBar) findViewById(R.id.cookQuery);
        houseQueryBar = (SeekBar) findViewById(R.id.houseQuery);
        educateQueryBar = (SeekBar) findViewById(R.id.educateQuery);
        entertainQueryBar = (SeekBar) findViewById(R.id.entertainQuery);
        cookingAnsBar = (SeekBar) findViewById(R.id.cookAnswer);
        houseAnsBar = (SeekBar) findViewById(R.id.houseAnswer);
        educateAnsBar = (SeekBar) findViewById(R.id.educateAnswer);
        entertainAnsBar = (SeekBar) findViewById(R.id.entertainAnswer);

        txtCookingV = (TextView) findViewById(R.id.txtCookingV);
        txtHouseV = (TextView) findViewById(R.id.txtHouseV);
        txtEducationV = (TextView) findViewById(R.id.txtEducationV);
        txtEntertainV = (TextView) findViewById(R.id.txtEntertainV);
        txtCookingQV = (TextView) findViewById(R.id.txtCookingQV);
        txtHouseQV = (TextView) findViewById(R.id.txtHouseQV);
        txtEducationQV = (TextView) findViewById(R.id.txtEducationQV);
        txtEntertainQV = (TextView) findViewById(R.id.txtEntertainQV);

        queryBtn = (Button) findViewById(R.id.button);

        cookingAnsBar.setEnabled(false);
        houseAnsBar.setEnabled(false);
        educateAnsBar.setEnabled(false);
        entertainAnsBar.setEnabled(false);

        cookQueryBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                Log.i(LOG_NAME, " Cooking: "+String.valueOf(progress / 100.0));
                query[0] = (progress / 100.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                txtCookingQV.setText(String.valueOf(query[0]));
            }
        });

        houseQueryBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                Log.i(LOG_NAME, " House " + String.valueOf(progress / 100.0));
                query[1] = (progress / 100.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                txtHouseQV.setText(String.valueOf(query[1]));
            }
        });

        educateQueryBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                Log.i(LOG_NAME, " Educate "+ String.valueOf(progress / 100.0));
                query[2] = (progress / 100.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                txtEducationQV.setText(String.valueOf(query[2]));
            }
        });

        entertainQueryBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                Log.i(LOG_NAME, " Entertain " + String.valueOf(progress / 100.0));
                query[3] = (progress / 100.0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                txtEntertainQV.setText(String.valueOf(query[3]));
            }
        });

        queryBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                apiCallForQuery();
            }
        });

    }

    public void apiCallForQuery() {
        RequestQueue q = Volley.newRequestQueue(MainActivity.this);
        String queryParams = "";
        for (int i = 0; i < query.length; i++) {
            queryParams += "," + String.valueOf(query[i]);
        }
        queryParams = queryParams.replaceFirst(",","").trim();
        Log.i(LOG_NAME, "Query : " + queryParams);
        String url = hostName + defaultActor + urlName + queryParams;
        Log.i(LOG_NAME, "URL: " + url);
        try {
            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            Log.i(LOG_NAME, "Got response from Server: " + response);
                            double[] answer = handleJsonResponse(response);
                            updateUI(answer);
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.i(LOG_NAME, "Error while sending request to the server!");
                        }
                    });
            // Add the request to the RequestQueue.
            q.add(jsObjRequest);
        } catch (Exception e) {
            Log.i(LOG_NAME, "Erro :" + e.getMessage());
        }
    }

    public String method(String str) {
        if (str != null && str.length() > 0 && str.charAt(str.length() - 1) == ',') {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    public double[] handleJsonResponse(JSONObject response) {
        String status = "";
        double[] answerArray = new double[4];
        JSONArray array = new JSONArray();
        try {
            status = response.getString("status");
            if (status.equalsIgnoreCase("error")) {
                Log.i(LOG_NAME, "Error message from server: " + response.getString("message"));
                Toast.makeText(getApplicationContext(), "No Person found for that Query!",
                        Toast.LENGTH_SHORT).show();
            } else if (status.equalsIgnoreCase("success")) {
                array = response.getJSONArray("answer");
                answerArray[0] = array.getDouble(0);
                answerArray[1] = array.getDouble(1);
                answerArray[2] = array.getDouble(2);
                answerArray[3] = array.getDouble(3);
//                Log.i(LOG_NAME, "" + String.valueOf(answerArray));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return answerArray;
    }

    public void updateUI(double[] answer) {
        int progress = (int) (answer[0] * 100);
        cookingAnsBar.setProgress(progress);
        txtCookingV.setText(String.valueOf(progress/100.0));
        progress = (int) (answer[1] * 100);
        houseAnsBar.setProgress(progress);
        txtHouseV.setText(String.valueOf(progress/100.0));
        progress = (int) (answer[2] * 100);
        educateAnsBar.setProgress(progress);
        txtEducationV.setText(String.valueOf(progress/100.0));
        progress = (int) (answer[3] * 100);
        entertainAnsBar.setProgress(progress);
        txtEntertainV.setText(String.valueOf(progress/100.0));
    }
}
