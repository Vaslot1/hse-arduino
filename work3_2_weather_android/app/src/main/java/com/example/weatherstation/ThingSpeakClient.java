package com.example.weatherstation;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class ThingSpeakClient {

    private static ThingSpeakClient me;
    private Context ctx;
    private RequestQueue queue;

    // TODO: подставить Write API Key канала 1 (ThingSpeak -> Channel -> API Keys)
    private static final String THINGSPEAK_URL =
            "https://api.thingspeak.com/update?api_key=YOUR_WRITE_API_KEY";
    private String tempdata = "&field1=";
    private String humdata = "&field2=";
    private String pressdata = "&field3=";
    private String lightdata = "&field4=";
    private String deviceId;
    private String token;
    private String TAG = "ThingSpeakClient";

    private ThingSpeakClient(Context ctx, String deviceId, String token) {
        this.ctx = ctx;
        this.deviceId = deviceId;
        this.token = token;
        createQueue();
    }

    public static final ThingSpeakClient getInstance(Context ctx, String deviceId,
                                                     String token) {
        if (me == null)
            me = new ThingSpeakClient(ctx, deviceId, token);
        return me;
    }

    private void createQueue() {
        if (queue == null)
            queue = Volley.newRequestQueue(ctx.getApplicationContext());
    }

    public void sendData(final double temp, final double press, final double hum,
                         final double lux) {
        // Все 4 поля одним GET-запросом: по методичке 4 отдельных запроса
        // обрабатываются плохо (доходит только 1 из 4)
        StringRequest request1 = new StringRequest(Request.Method.GET,
                THINGSPEAK_URL + tempdata + temp + humdata + hum
                        + pressdata + press + lightdata + lux,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
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
}
