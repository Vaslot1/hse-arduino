# Работа 2, пункт 3 — Arduino-скетч метеостанции (MQTT publisher)

Пункт 3 второй лабораторной работы (`Raspberry_ru.pdf`, «Второй проект –
удалённая метеостанция»). Скетч для Arduino Uno + WiFi-шилд Troyka WiFi
(ESP8266): читает датчики и публикует JSON в MQTT-топик `channel1` на
брокере Mosquitto (Raspberry Pi).

## Железо (схему собирает напарник)

- DHT11 (температура/влажность) → цифровой пин **5**
- Light Sensor (Flying Fish MH-Sensor) → аналоговый пин **A0** (D0 не используется)
- BME280/BMP280 (давление) → I2C: **A4 = SDA, A5 = SCL** (библиотека BME280I2C + Wire)
- Troyka WiFi (ESP8266) → SoftwareSerial на пинах **6 (RX) / 7 (TX)**, питание **5V**

## Библиотеки (уже есть в `sketchbook/libraries`)

`DHT sensor library`, `WiFiEsp`, `PubSubClient`, `BME280` (BME280I2C.h),
`SoftwareSerial` и `Wire` (встроенные).

## Что поменять перед заливкой

```cpp
char ssid[] = "wifi_SSID";        // имя WiFi-сети
char pass[] = "wifi_PASSWORD";    // пароль WiFi
IPAddress mqtt_server(192, 168, 1, 106); // IP малины с Mosquitto (hostname -I)
```

## Загадка проекта (решена)

В книге стояло `BME280::PresUnit_Pa` (давление в Паскалях). Чтобы получать
мм. рт. ст. без формул — достаточно `BME280::PresUnit_torr`
(в `BME280.cpp`: `final /= 133.32236534674`). В скетче уже исправлено.

## Как это работает

Каждые 30 секунд скетч публикует в топик `channel1` JSON:

```json
{"t":"26", "h":"16", "p":"766", "l":"267"}
```

где `t` — температура °C (DHT11), `h` — влажность %, `p` — давление мм.рт.ст.
(BME280), `l` — освещённость 0..1023 (фоторезистор).

Проверка на малине-брокере: `mosquitto_sub -d -t channel1`.
