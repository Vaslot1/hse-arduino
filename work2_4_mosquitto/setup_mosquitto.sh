#!/bin/bash
# Работа 2, пункт 4 - установка и проверка Mosquitto на Raspberry Pi OS.
# Запускать на малине, которая будет MQTT-брокером:  bash setup_mosquitto.sh

set -e

# 0) IP этой малины - его нужно вписать в скетч Arduino (mqtt_server)
#    и в MQTTClient.java мобильного приложения (MQTT_SERVER)
hostname -I

# 1) Обновление ОС - без этого Mosquitto работать не будет (по методичке)
sudo apt update
sudo apt upgrade

# 2) Установка брокера и клиентских утилит
sudo apt install mosquitto mosquitto-clients

# 3) Mosquitto >= 2 по умолчанию требует авторизацию и не слушает сеть.
#    Добавляем listener 1883 + allow_anonymous true в конфиг
#    (правка требует root; пароль root создаётся командой sudo passwd root)
CONF=/etc/mosquitto/mosquitto.conf
if ! grep -q "allow_anonymous" "$CONF"; then
    printf '\nlistener 1883\nallow_anonymous true\n' | sudo tee -a "$CONF"
fi

# 4) Перезапуск и проверка статуса - должно быть "active (running)"
sudo systemctl restart mosquitto
sudo systemctl status mosquitto

# 5) Проверка обмена: при работающем скетче на Arduino в этом терминале
#    должны побежать JSON-сообщения с датчиков:
#    {"t":"26", "h":"16", "p":"766", "l":"267"}
echo 'Запускаю подписку на топик channel1 (Ctrl+C для выхода)...'
mosquitto_sub -d -t channel1
