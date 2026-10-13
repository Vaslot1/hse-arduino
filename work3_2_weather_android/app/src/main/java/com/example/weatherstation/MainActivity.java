package com.example.weatherstation;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity implements MQTTClient.MQTTListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private MQTTClient mqttClient;

    private TextView chView;
    private TextView tempView;
    private TextView pressView;
    private TextView humView;
    private TextView luxView;
    private String topicstr;

    // Работа 3: отправка усреднённых данных в ThingSpeak каждые TIMEOUT секунд
    private static final int TIMEOUT = 30;
    private int tempCounter = 0;
    private int pressCounter = 0;
    private int humCounter = 0;
    private int luxCounter = 0;
    private double totalTemp;
    private double totalPress;
    private double totalHum;
    private double totalLux;
    // deviceId и token в этой реализации не используются (нужны для bulk JSON)
    private static String DEVICE_ID = "3c1e4c0b24f645258400e6e553bcc817";
    private static String TOKEN = "04b92cb5070f48aba72f1e030325c6f9";

    public String temp;
    public String press;
    public String hum;
    public String lux;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        topicstr = "channel1";
        Log.d(TAG, "onCreate");
        initView();
        initMQTT();
        initScheduler();
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
            temp = obj.getString("t");
            press = obj.getString("p");
            hum = obj.getString("h");
            lux = obj.optString("l");
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

        totalTemp += Integer.parseInt(temp);
        tempCounter++;
        totalPress += Integer.parseInt(press);
        pressCounter++;
        totalHum += Integer.parseInt(hum);
        humCounter++;
        totalLux += Integer.parseInt(lux);
        luxCounter++;
    }

    private void initScheduler() {
        ScheduledExecutorService scheduler =
                Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                //Log.d(TAG, "Scheduler running...");
                if (tempCounter == 0 || pressCounter == 0
                        || humCounter == 0 || luxCounter == 0) {
                    return; // данных ещё не было - не шлём NaN в канал
                }
                double mTemp = totalTemp / tempCounter;
                double mPress = totalPress / pressCounter;
                double mHum = totalHum / humCounter;
                double mLux = totalLux / luxCounter;

                totalTemp = 0;
                totalPress = 0;
                totalHum = 0;
                totalLux = 0;
                tempCounter = 0;
                pressCounter = 0;
                humCounter = 0;
                luxCounter = 0;
                // call ThingSpeak
                ThingSpeakClient.getInstance(MainActivity.this,
                        DEVICE_ID, TOKEN).sendData(mTemp, mPress, mHum, mLux);
            }
        }, 30, TIMEOUT, TimeUnit.SECONDS);
    }
}
