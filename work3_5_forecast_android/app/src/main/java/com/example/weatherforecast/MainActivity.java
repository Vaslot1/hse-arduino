package com.example.weatherforecast;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {

    private static final String TAG = "WeatherForecast";

    // TODO: подставить ID канала 2 (прогноз) и его Read API Key
    private static final String THINGSPEAK_URL =
            "https://api.thingspeak.com/channels/CHANNEL2_ID/feeds.json"
                    + "?api_key=YOUR_READ_API_KEY&results=1";

    private RequestQueue queue;
    private TextView textdate;
    private TextView textforecast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textdate = findViewById(R.id.textView);
        textforecast = findViewById(R.id.textView2);
        textdate.setText("");
        textforecast.setText("wait for it....");
        queue = Volley.newRequestQueue(this);
        initScheduler();
    }

    private void initScheduler() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                //Log.d(TAG, "Scheduler running...");
                StringRequest request1 = new StringRequest(Request.Method.GET,
                        THINGSPEAK_URL,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                try {
                                    JSONObject obj = new JSONObject(response);
                                    JSONArray feeds =
                                            obj.getJSONArray("feeds");
                                    JSONObject obj1 = feeds.getJSONObject(0);
                                    String thetime =
                                            obj1.getString("created_at");
                                    String theforecast =
                                            obj1.getString("field1");
                                    textdate.setText(thetime);
                                    textforecast.setText(theforecast);
                                } catch (JSONException jse) {
                                    jse.printStackTrace();
                                }
                                Log.d(TAG, "Response [" + response + "]");
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                error.printStackTrace();
                            }
                        });
                queue.add(request1);
            }
        }, 10, 60, TimeUnit.SECONDS);
    }
}
