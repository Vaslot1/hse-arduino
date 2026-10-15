#include <DHT.h>
#include <WiFiEsp.h>
#include <PubSubClient.h>
#include <BME280I2C.h>
#include <Wire.h>

char ssid[] = "wifi_SSID";       // your network SSID (name)
char pass[] = "wifi_PASSWORD";   // your network password
int status = WL_IDLE_STATUS;     // the Wifi radio's status

#define DHTPIN 5
#define DHTTYPE DHT11
#define TEMTPIN A0

// Emulate Serial1 on pins 6/7 if not present
#ifndef HAVE_HWSERIAL1
#include "SoftwareSerial.h"
SoftwareSerial Serial1(6, 7); // RX, TX
#endif

#define ESP_BAUDRATE 9600

WiFiEspClient client;
DHT dht(DHTPIN, DHTTYPE);
BME280I2C bme;

// Setup MQTT client
PubSubClient mqttClient(client);

IPAddress mqtt_server(10, 83, 178, 100); // your raspberry mqtt server IP
const char *topic = "channel1";

void setup() {
  Serial.begin(ESP_BAUDRATE);
  pinMode(LED_BUILTIN, OUTPUT);

  dht.begin();
  Serial.print("Searching for ESP8266...");
  // initialize serial for ESP module
  Serial1.begin(ESP_BAUDRATE);

  // initialize ESP module
  WiFi.init(&Serial1);

  // check for the presence of the shield
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("WiFi shield not present");
    // don't continue
    while (true);
  }

  // attempt to connect to WiFi network
  while (status != WL_CONNECTED) {
    Serial.print("Attempting to connect to WPA SSID: ");
    Serial.println(ssid);
    // Connect to WPA/WPA2 network
    status = WiFi.begin(ssid, pass);
  }

  Serial.println("You're connected to the network");

  initMQTT();

  Wire.begin();

  while (!bme.begin()) {
    Serial.println("Could not find BME280 sensor!");
    delay(1000);
  }

  switch (bme.chipModel()) {
    case BME280::ChipModel_BME280:
      Serial.println("Found BME280 sensor! Success.");
      break;
    case BME280::ChipModel_BMP280:
      Serial.println("Found BMP280 sensor! No Humidity available.");
      break;
    default:
      Serial.println("Found UNKNOWN sensor! Error!");
  }
}

void loop() {
  digitalWrite(LED_BUILTIN, LOW); // Led ON

  float h = dht.readHumidity();
  float t = dht.readTemperature();
  float temper(NAN), humidity(NAN), press(NAN);

  BME280::TempUnit tempUnit(BME280::TempUnit_Celsius);
  // Загадка проекта: давление в мм. рт. ст. вместо Па -
  // просто PresUnit_torr вместо PresUnit_Pa
  BME280::PresUnit presUnit(BME280::PresUnit_torr);

  bme.read(press, temper, humidity, tempUnit, presUnit);

  int lightVal = analogRead(TEMTPIN) * 0.9765625;
  Serial.print("Temperature: "); Serial.println((int)round(t));
  Serial.print("Humidity: "); Serial.println((int)round(h));
  Serial.print("TEMT: "); Serial.println(lightVal);
  Serial.print("Pressure: "); Serial.println((int)round(press));

  // Connect to MQTT server
  if (!mqttClient.connected()) {
    connect();
  }
  mqttClient.loop();

  // String-конкатенация не влезает в оставшуюся RAM (~600 байт) -
  // payload обрезался до "}". Собираем JSON через snprintf в буфер.
  char payload[64];
  snprintf(payload, sizeof(payload),
    "{\"t\":\"%d\", \"h\":\"%d\", \"p\":\"%d\", \"l\":\"%d\"}",
    (int)round(t), (int)round(h), (int)round(press), lightVal);

  boolean ok = mqttClient.publish(topic, payload);

  Serial.print("Result: "); Serial.println(payload);
  Serial.print("Publish: "); Serial.print(ok ? "OK" : "FAILED");
  Serial.print(", MQTT state: "); Serial.println(mqttClient.state());

  digitalWrite(LED_BUILTIN, HIGH);
  delay(30000);
}

void initMQTT() {
  mqttClient.setServer(mqtt_server, 1883);
}

void connect() {
  while (!mqttClient.connected()) {
    Serial.println("Connecting to MQTT Server....");
    boolean result = mqttClient.connect("euryeury23429");
    if (result) {
      Serial.println("Client connected");
    } else {
      Serial.println("Client not connected. Next attempt in 5 secs");
      delay(5000);
    }
  }
}
