package com.example.weatherstation;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MQTTClient implements MqttCallback {

    private static final String TAG = "MQTTClient";

    // change to your mosquito raspberry pi IP
    public static final String MQTT_SERVER = "tcp://192.168.1.106:1883";

    private static MQTTClient me;
    private Context ctx;
    private MQTTListener listener;
    private MqttAndroidClient mqttClient;

    private MQTTClient(Context ctx) {
        this.ctx = ctx;
    }

    public static final MQTTClient getInstance(MainActivity ctx) {
        if (me == null)
            me = new MQTTClient(ctx);
        return me;
    }

    public void setListener(MQTTListener listener) {
        this.listener = listener;
    }

    public void connectToMQTT(String server) {
        Log.d(TAG, "Connecting to MQTT Server..");
        String clientId = MqttClient.generateClientId();
        Log.d(TAG, "Client Id [" + clientId + "]");
        mqttClient = new MqttAndroidClient(ctx, server, clientId);
        mqttClient.setCallback(this);
        try {
            IMqttToken mqttToken = mqttClient.connect();
            mqttToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG, "Connected to MQTT server");
                    listener.onConnected();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Log.e(TAG, "Failure");
                    exception.printStackTrace();
                    listener.onConnectionFailure(exception);
                }
            });
        } catch (MqttException mqe) {
            Log.e(TAG, "Unable to connect to MQTT Server");
            mqe.printStackTrace();
            listener.onError(mqe);
        }
    }

    public void subscribe(final String topic) {
        try {
            IMqttToken subToken = mqttClient.subscribe(topic, 1);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed to topic [" + topic + "]");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Log.e(TAG, "Error while subscribing to the topic ["
                            + topic + "]");
                    exception.printStackTrace();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
    }

    @Override
    public void messageArrived(String topic, MqttMessage message)
            throws Exception {
        Log.d(TAG, "Message arrived...");
        String payload = new String(message.getPayload());
        Log.d(TAG, "Payload [" + payload + "]");
        listener.onMessage(topic, message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public interface MQTTListener {
        public void onConnected();
        public void onConnectionFailure(Throwable t);
        public void onMessage(String topic, MqttMessage message);
        public void onError(MqttException mqe);
    }
}
