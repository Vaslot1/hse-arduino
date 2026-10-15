# Работа 2, пункт 5 — Android-приложение «Remote Weather Station»

Пункт 5 второй лабораторной работы (`Raspberry_ru.pdf`). Мобильное
приложение — MQTT-subscriber: подключается к брокеру Mosquitto на малине,
подписывается на топик `channel1`, парсит JSON с датчиков и показывает
температуру, влажность, давление и освещённость.

## Структура

```
work2_5_weather_android/
├── settings.gradle / build.gradle / gradle.properties
└── app/
    ├── build.gradle                        # paho mqttv3 1.2.5 + android.service 1.1.1
    └── src/main/
        ├── AndroidManifest.xml             # permissions + MqttService
        ├── java/com/example/weatherstation/
        │   ├── MainActivity.java           # UI + парсинг JSON
        │   └── MQTTClient.java             # MQTT-клиент (paho MqttAndroidClient)
        └── res/
            ├── layout/activity_main.xml
            └── values/{strings,colors,themes}.xml
```

## Как открыть

1. Запустить Android Studio через `Android Studio.cmd` в корне
   `D:\Projects\hse-arduino`.
2. File → Open → выбрать папку `work2_5_weather_android`.
3. Дождаться Gradle-синхронизации.

## Что настроить перед запуском

В `MQTTClient.java` задать IP малины-брокера (из `hostname -I`):

```java
public static final String MQTT_SERVER = "tcp://192.168.1.106:1883";
```

Телефон, Arduino и малина должны быть в одной WiFi-сети.

## Отличия от первой работы / важные детали

- **AGP 8.7.3, targetSdk 30** — так требует методичка: paho 1.1.1 не имеет
  кода для API 31+. В `build.gradle` проекта стоит AGP 8.7.3 (в AGP 9 флаг
  `android.enableJetifier` удалён, а он нужен paho).
- `android.enableJetifier=true` в `gradle.properties` — «загадочная строка»
  из методички: переписывает support-зависимости paho на androidx.
- `androidx.localbroadcastmanager:localbroadcastmanager:1.0.0` —
  update 06.02.2026 из методички.
- Оба paho-артефакта есть на Maven Central, дополнительные репозитории
  не нужны.

## Ожидаемый лог (Logcat)

```
D/MQTTClient: Connecting to MQTT Server..
I/MQTTClient: Connected to MQTT server
D/MQTTClient: Subscribed to topic [channel1]
D/MQTTClient: Message arrived...  Payload [{"t":"26", "h":"12", "p":"753", "l":"485"}]
```

## Дополнительно (по методичке)

Можно добавить 2 `EditText` для ручного ввода топика и IP брокера —
тогда приложение сможет читать данные с чужих датчиков.
