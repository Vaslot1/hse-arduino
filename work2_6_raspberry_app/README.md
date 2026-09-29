# Работа 2, пункт 6 — Raspberry Pi OS-приложение метеостанции

Пункт 6 второй лабораторной работы (`Raspberry_ru.pdf`). В методичке это
«задание на десяточку» — малиновое приложение нужно написать
самостоятельно на Python с интерфейсом на tkinter (tkinter уже
предустановлен в Raspberry Pi OS).

`weather_station.py` — MQTT-subscriber: подписывается на топик `channel1`
на брокере Mosquitto и показывает данные с датчиков в окне — те же поля и
та же логика освещённости, что в Android-приложении из `work2_5`.

## Установка

```bash
sudo apt install python3-paho-mqtt python3-tk
```

или через виртуальное окружение (на Bookworm+ pip в системный Python
не ставится):

```bash
python3 -m venv venv --system-site-packages
venv/bin/pip install paho-mqtt
```

## Настройка

В начале `weather_station.py`:

```python
MQTT_SERVER = "192.168.1.106"  # IP малины-брокера
TOPIC = "channel1"
```

## Запуск

```bash
python3 weather_station.py
```

Приложение можно запускать и на комариной малине (брокере), и на второй
малине — по методичке 2 малины не обязательны. Для проверки без Arduino
можно публиковать тестовые сообщения с брокера:

```bash
mosquitto_pub -t channel1 -m '{"t":"26", "h":"16", "p":"766", "l":"267"}'
```
