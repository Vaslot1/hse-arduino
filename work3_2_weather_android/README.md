# Работа 3, пункт 2 — Android-приложение «Remote Weather Station» + ThingSpeak

Пункт 2 третьей лабораторной работы (`Raspberry_ru.pdf`, «Третий проект –
работа с облачной платформой интернета вещей»). Копия приложения из
`work2_5_weather_android`, которое теперь не только показывает данные из
MQTT-топика `channel1`, но и стримит их в облако ThingSpeak.

## Что изменилось относительно work2_5

- `app/build.gradle`: добавлена `com.android.volley:volley:1.2.1`
  (`INTERNET` в манифесте уже был).
- Новый класс `ThingSpeakClient.java` — синглтон, очередь запросов Volley,
  `sendData()` шлёт все 4 поля **одним** GET-запросом на
  `https://api.thingspeak.com/update` (по методичке 4 отдельных запроса
  работают плохо — доходит только 1 из 4).
- `MainActivity.java`: значения `temp/press/hum/lux` стали полями класса;
  в `updateView()` накапливаются суммы и счётчики; новый `initScheduler()`
  каждые 30 с считает средние и вызывает `ThingSpeakClient.sendData()`
  (вызов из `onCreate`).
- Небольшая страховка, которой нет в книге: если за интервал не пришло
  ни одного сообщения — отправка пропускается, иначе в канал улетел бы NaN.

## Что настроить перед запуском

1. `MQTTClient.java` → `MQTT_SERVER` — IP малины-брокера (`hostname -I`).
2. `ThingSpeakClient.java` → `YOUR_WRITE_API_KEY` — Write API Key канала 1
   (ThingSpeak → Channel → API Keys → Write API Key).

Телефон, Arduino и малина должны быть в одной WiFi-сети; у телефона должен
быть выход в интернет (запросы идут на api.thingspeak.com).

## Как проверить

- Logcat: `D/ThingSpeakClient: Response [<id записи>]` — ответ 0 означает
  отказ (неверный ключ / чаще чем раз в ~15 с на бесплатном аккаунте).
- ThingSpeak → канал 1 → Private View: графики должны расти раз в 30 с.

## Остальная цепочка

Arduino-скетч — `work2_3_meteo_arduino` (без изменений), брокер —
`work2_4_mosquitto`. MATLAB-скрипты для каналов и второе приложение —
в `work3_3_thingspeak` и `work3_5_forecast_android`.
