# Работа 2, пункт 4 — MQTT-брокер Mosquitto на Raspberry Pi

Пункт 4 второй лабораторной работы (`Raspberry_ru.pdf`). Поднимаем
MQTT-брокер (Mosquitto) на второй малине — или на той же единственной,
если малина одна (по методичке 2 малины не обязательны).

## Архитектура проекта

```
сенсоры → Arduino Uno + WiFi-shield (publisher, скетч из work2_3)
        → MQTT → Mosquitto-брокер (этот пункт, малина)
        → подписчики: Raspberry Pi OS-приложение (work2_6)
                      и Android-приложение (work2_5)
```

## Файлы

- `setup_mosquitto.sh` — все команды из методички одним скриптом;
- `mosquitto.conf` — 2 строки, которые надо добавить в
  `/etc/mosquitto/mosquitto.conf` (в книге опечатка: `/etc/mosquito/...`).

## Шаги вручную (как в методичке)

```bash
# IP малины - вписать в скетч Arduino и в MQTTClient.java
hostname -I

sudo apt update
sudo apt upgrade            # без апдейта Mosquitto работать не будет
sudo apt install mosquitto mosquitto-clients
sudo systemctl status mosquitto   # ищем active (running)
```

Для Mosquitto версии 2+ (Raspberry Pi OS Bookworm и новее) дополнительно:

```bash
sudo passwd root   # создать пароль root, чтобы править конфиг
```

затем зайти под root (logout → Other… → root) и добавить в
`/etc/mosquitto/mosquitto.conf` две строки:

```
listener 1883
allow_anonymous true
```

Перезагрузиться (или `sudo systemctl restart mosquitto`).

## Проверка

При работающем скетче на Arduino:

```bash
mosquitto_sub -d -t channel1
```

В терминале должны побежать сообщения вида:

```
Client mosqsub|993-raspberrypi received PUBLISH (d0, q0, r0, m0, 'channel1', ... (42 bytes))
{"t":"26", "h":"16", "p":"766", "l":"267"}
```

## Важно

Все устройства (Arduino с WiFi-шилдом, малина-брокер, малина/телефон с
приложениями) должны быть в одной сети — например, подключены к одной
точке доступа, разданной с телефона.
