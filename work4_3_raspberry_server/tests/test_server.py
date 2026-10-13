import importlib
import socket
import sys
import threading
import unittest
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import urlopen

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


class ServerTests(unittest.TestCase):
    def setUp(self):
        try:
            self.module = importlib.import_module("server")
        except ModuleNotFoundError:
            self.fail("The lab 4 HTTP server has not been implemented")
        self.hardware = self.module.DemoHardware()
        self.server = self.module.make_server("127.0.0.1", 0, self.hardware)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base = f"http://127.0.0.1:{self.server.server_port}"
        self.addCleanup(self.stop_server)

    def stop_server(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=2)

    def get(self, path):
        try:
            response = urlopen(self.base + path, timeout=3)
        except HTTPError as error:
            response = error
        with response:
            return response.status, response.read().decode("utf-8")

    def test_measurements_and_pressure_units(self):
        self.assertEqual(self.get("/temperature"), (200, "23"))
        self.assertEqual(self.get("/pressure"), (200, "760"))
        self.assertEqual(self.get("/humidity"), (200, "45"))

    def test_weather_thresholds_use_hpa(self):
        for pressure, expected in [
            (1022.69, "the weather will be stable"),
            (1022.68, "the weather will be cloudy"),
            (1009.14, "the weather will be cloudy"),
            (1009.13, "the weather will be rainy"),
        ]:
            with self.subTest(pressure=pressure):
                self.hardware.data = self.module.SensorData(23, pressure, 45)
                self.assertEqual(self.get("/weather"), (200, expected))

    def test_colors_and_switch_off(self):
        self.assertEqual(self.get("/FF0000"), (200, "FF0000"))
        self.assertEqual(self.hardware.color, (255, 0, 0))
        self.assertEqual(self.get("/00ff00"), (200, "00FF00"))
        self.assertEqual(self.hardware.color, (0, 255, 0))
        self.assertEqual(self.get("/000000"), (200, "000000"))
        self.assertEqual(self.hardware.color, (0, 0, 0))

    def test_random_returns_actual_color(self):
        status, body = self.get("/random")
        self.assertEqual(status, 200)
        self.assertRegex(body, r"^[0-9A-F]{6}$")
        self.assertEqual(self.hardware.color, tuple(int(body[i:i + 2], 16) for i in (0, 2, 4)))

    def test_methodical_query_format(self):
        self.assertEqual(self.get("/?params=temperature"), (200, "23"))
        self.assertEqual(self.get("/?params=0000FF"), (200, "0000FF"))
        self.assertEqual(self.hardware.color, (0, 0, 255))

    def test_unknown_commands_do_not_change_lights(self):
        self.get("/FF0000")
        for path in ("/favicon.ico", "/not-temperature", "/FF0000junk", "/?params=red", "/?params=FF0000&params=000000"):
            self.assertEqual(self.get(path)[0], 404)
            self.assertEqual(self.hardware.color, (255, 0, 0))

    def test_sensor_failure_keeps_server_available(self):
        def disconnected():
            raise OSError("sensor disconnected")
        self.hardware.read_sensor = disconnected
        self.assertEqual(self.get("/temperature")[0], 503)
        self.assertEqual(self.get("/00FF00"), (200, "00FF00"))

    def test_demo_mode_is_visible(self):
        status, body = self.get("/health")
        self.assertEqual(status, 200)
        self.assertIn("DEMO", body)

    def test_idle_browser_connection_does_not_block_android(self):
        # Браузер может заранее открыть TCP-соединение, не послав запрос.
        with socket.create_connection(("127.0.0.1", self.server.server_port), timeout=2):
            self.assertEqual(self.get("/temperature"), (200, "23"))

    def test_response_tells_clients_not_to_reuse_closed_connection(self):
        # OkHttp 3.6.0 иначе возвращает закрытый HTTP/1.0 сокет в пул.
        with urlopen(self.base + "/temperature", timeout=3) as response:
            self.assertEqual(response.headers.get("Connection"), "close")


if __name__ == "__main__":
    unittest.main()
