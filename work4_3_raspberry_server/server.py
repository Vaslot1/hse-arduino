#!/usr/bin/env python3
"""Работа 4: HTTP-команды для BME280/BMP280 и WS2812B на Raspberry Pi 5."""

import argparse
import logging
import random
import re
import threading
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, unquote, urlsplit
from typing import Optional

LOG = logging.getLogger("voicehome")
COMMANDS = {"temperature", "pressure", "humidity", "weather", "random", "health"}
COLOR = re.compile(r"[0-9a-fA-F]{6}")


@dataclass(frozen=True)
class SensorData:
    temperature: float
    pressure_hpa: float
    humidity: Optional[float]


class DemoHardware:
    """Явный тестовый режим: никаких обращений к GPIO и реальным датчикам."""

    demo = True

    def __init__(self):
        self.data = SensorData(23.0, 1013.25, 45.0)
        self.color = (0, 0, 0)

    def read_sensor(self):
        return self.data

    def set_color(self, color):
        self.color = color
        LOG.info("DEMO LED: %s", color)

    def close(self):
        self.set_color((0, 0, 0))


def weather_forecast(pressure_hpa):
    # В методичке ошибочно написано «Паскалей»: эти значения в hPa.
    if pressure_hpa >= 1022.69:
        return "the weather will be stable"
    if pressure_hpa >= 1009.14:
        return "the weather will be cloudy"
    return "the weather will be rainy"


def parse_command(path):
    url = urlsplit(path)
    if url.path == "/":
        commands = parse_qs(url.query).get("params", [])
        command = commands[0] if len(commands) == 1 else ""
    else:
        command = unquote(url.path[1:])
    if command in COMMANDS:
        return command
    if COLOR.fullmatch(command):
        return command.upper()
    return None


def make_server(host, port, hardware):
    hardware_lock = threading.Lock()

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            command = parse_command(self.path)
            if command is None:
                self.reply(404, "Unknown command. Use /temperature, /pressure, /humidity, /weather, /RRGGBB or /random.")
                return
            try:
                with hardware_lock:
                    if command == "health":
                        body = "OK — DEMO: simulated sensor and lights" if hardware.demo else "OK — real hardware"
                    elif command == "random" or COLOR.fullmatch(command):
                        color = tuple(random.randrange(256) for _ in range(3)) if command == "random" else tuple(int(command[i:i + 2], 16) for i in (0, 2, 4))
                        hardware.set_color(color)
                        body = "{:02X}{:02X}{:02X}".format(*color)
                    else:
                        data = hardware.read_sensor()
                        values = {
                            "temperature": str(round(data.temperature)),
                            "pressure": str(round(data.pressure_hpa / 1.33322387415)),
                            "humidity": "unavailable" if data.humidity is None else str(round(data.humidity)),
                            "weather": weather_forecast(data.pressure_hpa),
                        }
                        body = values[command]
                self.reply(200, body)
            except Exception:
                LOG.exception("Hardware error for %s", command)
                self.reply(503, "Hardware unavailable. Check the sensor, LED wiring and server log.")

        def reply(self, status, body):
            content = body.encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(content)))
            self.send_header("Cache-Control", "no-store")
            # OkHttp 3.6.0 должен исключить закрываемый HTTP/1.0 сокет из пула.
            self.send_header("Connection", "close")
            self.end_headers()
            try:
                self.wfile.write(content)
            except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                LOG.info("Client disconnected")

        def log_message(self, format_string, *args):
            LOG.info("%s %s", self.client_address[0], format_string % args)

    class Server(ThreadingHTTPServer):
        def get_request(self):
            connection, address = super().get_request()
            connection.settimeout(10)
            return connection, address

    # Пустые соединения браузера не блокируют телефон; I2C и лента защищены lock.
    server = Server((host, port), Handler)
    server.hardware_lock = hardware_lock
    return server


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--pixels", type=int, default=8)
    parser.add_argument("--brightness", type=float, default=0.1)
    parser.add_argument("--address", type=lambda value: int(value, 0), default=0x76)
    parser.add_argument("--led-backend", choices=("spi", "pio"), default="spi")
    parser.add_argument("--demo", action="store_true", help="Simulated sensor and lights; works without Raspberry Pi libraries")
    args = parser.parse_args()
    if not 1 <= args.pixels <= 1024:
        parser.error("--pixels must be between 1 and 1024")
    if not 0 <= args.brightness <= 1:
        parser.error("--brightness must be between 0 and 1")
    if args.address not in (0x76, 0x77):
        parser.error("--address must be 0x76 or 0x77")
    if not 1 <= args.port <= 65535:
        parser.error("--port must be between 1 and 65535")
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    try:
        if args.demo:
            hardware = DemoHardware()
        else:
            from hardware import RaspberryHardware
            hardware = RaspberryHardware(args.address, args.pixels, args.brightness, args.led_backend)
    except Exception as error:
        parser.exit(1, f"Hardware initialization failed: {error}\nSee README.md. No automatic switch to demo mode.\n")
    server = None
    try:
        server = make_server(args.host, args.port, hardware)
        LOG.info("Listening on %s:%s — %s", args.host, args.port, "DEMO" if args.demo else args.led_backend)
        server.serve_forever()
    except KeyboardInterrupt:
        LOG.info("Stopping")
    finally:
        if server is not None:
            server.server_close()
            with server.hardware_lock:
                hardware.close()
        else:
            hardware.close()


if __name__ == "__main__":
    main()
