# Работа 3, пункт 5 — Android-приложение «Weather Forecast»

Пункт 5 третьей лабораторной работы (`Raspberry_ru.pdf`). Отдельное
приложение: раз в 60 с читает последнюю запись канала 2 ThingSpeak
(словесный прогноз погоды, который считает MATLAB Analysis из пункта 3)
и показывает текст прогноза и время его создания.

## Структура

```
work3_5_forecast_android/
├── settings.gradle / build.gradle / gradle.properties / local.properties
└── app/
    ├── build.gradle                        # appcompat + constraintlayout + volley 1.2.1
    └── src/main/
        ├── AndroidManifest.xml             # permission INTERNET
        ├── java/com/example/weatherforecast/
        │   └── MainActivity.java           # scheduler 60 c -> GET feeds.json -> 2 TextView
        └── res/
            ├── layout/activity_main.xml    # toolbar + textView (дата) + textView2 (прогноз)
            └── values/{strings,colors,themes}.xml
```

## Что настроить перед запуском

В `MainActivity.java` подставить ID **канала 2** и его **Read API Key**:

```java
private static final String THINGSPEAK_URL =
        "https://api.thingspeak.com/channels/CHANNEL2_ID/feeds.json"
                + "?api_key=YOUR_READ_API_KEY&results=1";
```

Проверить URL можно прямо в браузере — в ответ придёт JSON вида
`{"channel": {...}, "feeds": [{"created_at": "...", "field1": "The weather will be rainy"}]}`.

## Отличия от кода методички

- Очередь Volley создаётся один раз в `onCreate` (в книге — на каждый тик
  планировщика).
- Пакет `com.example.weatherforecast`, `compileSdk/targetSdk 36`
  (в SDK установлена только platform-36).
- Остальное — как в книге: GET-запрос, парсинг `feeds[0].field1` +
  `created_at`, планировщик с задержкой 10 с и периодом 60 с.

## Как открыть

`Android Studio.cmd` → File → Open → `work3_5_forecast_android`.
Работает из любой сети (обращается в облако, а не в локальную сеть).
