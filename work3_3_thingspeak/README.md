# Работа 3 — метеостанция + облако ThingSpeak

Третий проект методички (`Raspberry_ru.pdf`, «Работа с облачной платформой
интернета вещей»). Схема и скетч Arduino — те же, что в работе 2.

## Как устроен конвейер

```
Arduino (work2_3) --JSON--> MQTT Mosquitto на малине (work2_4)
       |
       v
work3_2 (Android) --GET /update--> ThingSpeak канал 1 (4 поля, каждые 30 с)
                                       |
                            MATLAB Analysis (каждые 5 мин, TimeControl)
                                       |
                                       v
                              ThingSpeak канал 2 (текст прогноза)
                                       |
                                       v
                          work3_5 (Android) читает feeds.json
```

## Что уже подготовлено

- `work3_2_weather_android` — приложение-источник (MQTT → ThingSpeak)
- `work3_3_thingspeak` — MATLAB-скрипты для облака + эта инструкция
- `work3_5_forecast_android` — приложение-читатель прогноза

Оба Android-проекта собираются (проверено `assembleDebug`).

---

## Шаг 1. Аккаунт и канал 1 (в браузере)

1. https://thingspeak.com/ → **Get Started For Free** → регистрация
   MathWorks (по методичке — через сеть, раздаваемую с телефона).
2. **Channels → My Channels → New Channel**.
3. Заполнить: Name (напр. `Remote Weather Station`), Description,
   и включить 4 поля:
   - Field 1: `Temperature`
   - Field 2: `Humidity`
   - Field 3: `Pressure`
   - Field 4: `Illumination`
4. Внизу **Save Channel**.
5. Откроется страница канала. ID канала — в адресной строке:
   `thingspeak.com/channels/<ID>/private_show`.
6. Закладка **API Keys** → скопировать **Write API Key**
   (и пригодится **Read API Key** — он ниже, в секции Read API Keys).

## Шаг 2. Приложение-источник (work3_2)

Открыть `work3_2_weather_android` в Android Studio (`Android Studio.cmd` →
File → Open). Подставить:

- `ThingSpeakClient.java`: `YOUR_WRITE_API_KEY` → Write API Key канала 1.
- `MQTTClient.java`: `MQTT_SERVER` → `tcp://<IP малины>:1883`
  (IP из `hostname -I` на малине).

Пермишен `INTERNET` и зависимость Volley уже добавлены. Подробности —
в `work3_2_weather_android/README.md`.

## Шаг 3. Канал 2 + MATLAB Analysis + TimeControl

1. **New Channel**: одно поле Field 1 (назвать напр. `Forecast`) → Save.
   Запомнить **ID канала 2** и его **Write API Key**.
2. Верхнее меню **Apps → MATLAB Analysis** → выбрать **Custom
   (no starter code)** → **Create**.
3. Name — любое. В окно **MATLAB Code** вставить содержимое
   `forecast_analysis.m`, подставив:
   - `readChId` — ID канала 1 (число, без кавычек)
   - `writeChId` — ID канала 2
   - `writeKey` — Write API Key канала 2 (в одинарных кавычках)
   - `readKey` — Read API Key канала 1
4. **Save and Run** → в блоке Output ниже должно напечататься
   `The weather will be ...` (или `no data yet`, если канал 1 пуст).
5. На той же странице прокрутить вниз → ссылка **TimeControl** →
   заполнить форму:
   - Name: любое
   - TimeZone: `(UTC+03:00) Moscow`
   - Frequency: **Recurring**
   - Recurrence: **Minute**, Every: **5** minutes
   - Action: **MATLAB Analysis** → выбрать созданный скрипт
   - **Save TimeControl**

Проверка: страница канала 2 → **Channel Stats** → счётчик `Entries`
растёт раз в ~5 минут. График канала 2 останется пустым — **это
нормально**: в поле лежит текст, а графики рисуют только числа.

## Шаг 4. MATLAB Visualization (общий график)

1. Открыть **канал 1** → зелёная кнопка **MATLAB Visualization**
   (справа наверху) → **Custom** → **Create**.
2. Name: `Remote Weather Station Data`. Вставить
   `channel_visualization.m`, подставив `readChId` (ID канала 1)
   и `readKey` (Read API Key канала 1).
3. **Save and Run** → под редактором появится график с 4 наборами точек.
4. Чтобы он висел на странице канала: на странице канала кнопка
   **Add Visualizations** → выбрать созданную → она станет пятым
   графиком на Private View.

## Шаг 5. Приложение-прогноз (work3_5)

В `work3_5_forecast_android/.../MainActivity.java` подставить в
`THINGSPEAK_URL`:

- `CHANNEL2_ID` → ID канала 2 (число)
- `YOUR_READ_API_KEY` → **Read API Key канала 2** (не write!)

Проверка URL в браузере до запуска приложения:

```
https://api.thingspeak.com/channels/<ID_канала_2>/feeds.json?api_key=<READ_KEY>&results=1
```

- Вернулся JSON `"feeds": [{"created_at": "...", "field1": "The weather..."}]` — ок.
- Вернулось `-1` — ключ не подходит для чтения канала (проверь, что это
  именно Read API Key канала 2).
- Вернулось `0` или ошибка — проверь ID канала.

## День лабораторной — порядок запуска

1. Собрать схему работы 2, залить `work2_3_meteo_arduino`
   (ssid/пароль WiFi, IP малины).
2. Малина: `mosquitto` + `mosquitto_sub -d -t channel1`
   (настройки — `work2_4_mosquitto`).
3. Телефон в той же WiFi-сети, с интернетом → запустить `work3_2`.
   В Logcat: `D/ThingSpeakClient: Response [<число>]` каждые 30 с.
   `Response [0]` = ключ неверный или чаще ~15 с.
4. Канал 1 → Private View: графики растут каждые 30 с.
5. Через ~5 мин прогноз появляется в канале 2 (TimeControl).
6. Запустить `work3_5` на телефоне — покажет прогноз и время.
   Работает из любой сети.

## Частые проблемы (уже встречались)

| Симптом | Причина |
|---|---|
| `MATLAB code contains an error` при вставке скрипта | Кириллица в комментариях — файлы уже переведены на ASCII |
| `URL is incorrectly formed` в строке thingSpeakWrite | Нельзя передавать `string` в `'Values'` и пустой `'TimeStamps'` — в файле уже исправлено (`{text}`, TimeStamps убран) |
| Браузер показывает `-1` на feeds.json | Не тот ключ: нужен Read API Key именно этого канала |
| График канала 2 пустой, но Entries растёт | Норма: в поле текст, графики строят только числа |
| Прогноз всегда `rainy`/`no data yet` | В канале 1 пусто — приложение-источник не стримит |
| Приложение висит на «wait for it» | Ответ не JSON (ключ/ID неверные) — сначала проверь URL в браузере |
