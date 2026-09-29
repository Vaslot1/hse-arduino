#!/usr/bin/env python3
"""
Работа 2, пункт 6 - Raspberry Pi OS-приложение удалённой метеостанции.

MQTT-subscriber с GUI на tkinter: подписывается на топик channel1 на
брокере Mosquitto и показывает температуру, влажность, давление и
освещённость, приходящие с Arduino (JSON: {"t","h","p","l"}).

Установка зависимостей на Raspberry Pi OS:
    sudo apt install python3-paho-mqtt python3-tk
    # или в venv: pip install paho-mqtt

Запуск:
    python3 weather_station.py
"""

import json
import tkinter as tk

import paho.mqtt.client as mqtt

MQTT_SERVER = "192.168.1.106"  # IP малины-брокера (hostname -I на ней)
MQTT_PORT = 1883
TOPIC = "channel1"


class WeatherStationApp:
    def __init__(self, root):
        self.root = root
        root.title("Remote Weather Station")

        title = tk.Label(root, text="Remote Weather Station",
                         bg="#6851A5", fg="white", font=("Sans", 16, "bold"))
        title.pack(fill="x")

        self.topic_var = tk.StringVar(value="Topic: ")
        self.temp_var = tk.StringVar(value="Temperature: ")
        self.press_var = tk.StringVar(value="Pressure: ")
        self.hum_var = tk.StringVar(value="Humidity: ")
        self.lux_var = tk.StringVar(value="Illumination: ")

        for var, color in (
            (self.topic_var, "#D32F2F"),
            (self.temp_var, "#388E3C"),
            (self.press_var, "#9C27B0"),
            (self.hum_var, "#03A9F4"),
            (self.lux_var, "#F57C00"),
        ):
            tk.Label(root, textvariable=var, fg=color,
                     font=("Sans", 14), wraplength=420,
                     justify="center").pack(pady=8)

        # Совместимость paho-mqtt 1.x (apt на Bookworm) и 2.x (Trixie/pip)
        try:
            self.client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION1)
        except (AttributeError, TypeError):
            self.client = mqtt.Client()

        self.client.on_connect = self.on_connect
        self.client.on_message = self.on_message
        self.client.connect(MQTT_SERVER, MQTT_PORT, keepalive=60)
        self.client.loop_start()

        root.protocol("WM_DELETE_WINDOW", self.on_close)

    def on_connect(self, client, userdata, flags, rc):
        client.subscribe(TOPIC)

    def on_message(self, client, userdata, msg):
        payload = msg.payload.decode("utf-8")
        print("Payload [" + payload + "]")
        try:
            data = json.loads(payload)
        except json.JSONDecodeError:
            return
        # GUI можно трогать только из главного потока tkinter
        self.root.after(0, self.update_view, msg.topic, data)

    def update_view(self, topic, data):
        self.topic_var.set("Topic: " + topic)
        self.temp_var.set("Temperature: " + str(data["t"]) + " C")
        self.press_var.set("Pressure: " + str(data["p"]) + " mmHg")
        self.hum_var.set("Humidity: " + str(data["h"]) + " %")

        lux = int(data["l"])
        lux_v = lux * 5 // 1024
        if 0 <= lux_v <= 0.4:
            state = "It is light."
        elif lux_v <= 2:
            state = "It is bright."
        else:
            state = "It is dark."
        self.lux_var.set(
            "Illumination: {}. {} Resistance value is {}".format(
                lux_v, state, lux))

    def on_close(self):
        self.client.loop_stop()
        self.client.disconnect()
        self.root.destroy()


if __name__ == "__main__":
    root = tk.Tk()
    root.geometry("480x400")
    WeatherStationApp(root)
    root.mainloop()
