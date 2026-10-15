package com.example.weatherstation;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity implements MQTTClient.MQTTListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private MQTTClient mqttClient;

    private TextView chView;
    private TextView tempView;
    private TextView pressView;
    private TextView humView;
    private TextView luxView;
    private String topicstr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        topicstr = "channel1";
        Log.d(TAG, "onCreate");
        initView();
        initMQTT();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onConnected() {
        mqttClient.subscribe(topicstr);
    }

    @Override
    public void onConnectionFailure(Throwable t) {
    }

    @Override
    public void onMessage(String topic, MqttMessage message) {
        // Parse the payload
        Log.d(TAG, "Parsing message...");
        String payload = new String(message.getPayload());
        Log.d(TAG, "Payload [" + payload + "]");
        try {
            JSONObject obj = new JSONObject(payload);
            String temp = obj.getString("t");
            String press = obj.getString("p");
            String hum = obj.getString("h");
            String lux = obj.optString("l");
            updateView(topic, temp, press, hum, lux);
        } catch (JSONException jse) {
            jse.printStackTrace();
        }
    }

    @Override
    public void onError(MqttException mqe) {
    }

    private void initMQTT() {
        mqttClient = MQTTClient.getInstance(this);
        mqttClient.setListener(this);
        mqttClient.connectToMQTT(MQTTClient.MQTT_SERVER);
    }

    private void initView() {
        chView = findViewById(R.id.topic);
        chView.setText("Topic: ");
        tempView = findViewById(R.id.temp);
        tempView.setText("Temperature: ");
        pressView = findViewById(R.id.press);
        pressView.setText("Pressure: ");
        humView = findViewById(R.id.hum);
        humView.setText("Humidity: ");
        luxView = findViewById(R.id.lux);
        luxView.setText("Illumination: ");
    }

    private void updateView(String topic, String temp, String press, String hum,
                            String lux) {
        chView.setText("Topic: " + topic);
        tempView.setText("Temperature: " + temp + " C");
        pressView.setText("Pressure: " + press + " mmHg");
        humView.setText("Humidity: " + hum + " %");

        int luxV = Integer.valueOf(lux) * 5 / 1024;
        if (luxV >= 0 && luxV <= 0.4) {
            luxView.setText("Illumination: " + luxV
                    + ". It is light. Resistance value is " + lux);
        } else if (luxV > 0.4 && luxV <= 2) {
            luxView.setText("Illumination: " + luxV
                    + ". It is bright. Resistance value is " + lux);
        } else {
            luxView.setText("Illumination: " + luxV
                    + ". It is dark. Resistance value is " + lux);
        }
    }
}
