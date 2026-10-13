"""Аппаратная часть. Импортируется только при запуске без --demo."""

from pathlib import Path

from server import SensorData


class RaspberryHardware:
    demo = False

    def __init__(self, address, count, brightness, backend):
        import board
        from adafruit_bme280 import basic as bme280

        self.i2c = board.I2C()
        self.spi = None
        self.pixels = None
        try:
            self.sensor = bme280.Adafruit_BME280_I2C(self.i2c, address=address)
            if backend == "spi":
                import neopixel_spi

                self.spi = board.SPI()
                self.pixels = neopixel_spi.NeoPixel_SPI(self.spi, count, brightness=brightness, auto_write=False, pixel_order=neopixel_spi.GRB)
            else:
                if not Path("/dev/pio0").exists():
                    raise RuntimeError("PIO device /dev/pio0 is missing. Use --led-backend spi with DIN on GPIO10 (physical pin 19), or see the PIO setup in README.md.")
                import adafruit_pixelbuf
                from adafruit_raspberry_pi5_neopixel_write import neopixel_write

                class Pi5Pixels(adafruit_pixelbuf.PixelBuf):
                    def _transmit(self, buffer):
                        neopixel_write(board.D18, buffer)

                self.pixels = Pi5Pixels(count, brightness=brightness, auto_write=False, byteorder="GRB")
            self.set_color((0, 0, 0))
        except Exception:
            self.close()
            raise

    def read_sensor(self):
        return SensorData(self.sensor.temperature, self.sensor.pressure, self.sensor.relative_humidity)

    def set_color(self, color):
        self.pixels.fill(color)
        self.pixels.show()

    def close(self):
        # Освобождаем шины, даже если отключённая лента не ответила при остановке.
        try:
            if self.pixels is not None:
                self.set_color((0, 0, 0))
        finally:
            try:
                if self.spi is not None:
                    self.spi.deinit()
            finally:
                self.i2c.deinit()
