"""Аппаратная часть. Импортируется только при запуске без --demo."""

from pathlib import Path
import logging
from time import sleep

from server import SensorData


def create_sensor(i2c, address):
    chip_id = bytearray(1)
    while not i2c.try_lock():
        sleep(0.001)
    try:
        i2c.writeto_then_readfrom(address, b'\xd0', chip_id)
    finally:
        i2c.unlock()
    if chip_id[0] == 0x60:
        from adafruit_bme280 import basic as bme280
        return bme280.Adafruit_BME280_I2C(i2c, address=address), 'BME280'
    if chip_id[0] == 0x58:
        import adafruit_bmp280
        return adafruit_bmp280.Adafruit_BMP280_I2C(i2c, address=address), 'BMP280'
    raise RuntimeError(f'Unsupported sensor chip ID {chip_id[0]:#x} at I2C address {address:#x}; expected BME280 (0x60) or BMP280 (0x58)')


def sample_sensor(sensor, kind):
    humidity = sensor.relative_humidity if kind == 'BME280' else None
    return SensorData(sensor.temperature, sensor.pressure, humidity)


class RaspberryHardware:
    demo = False

    def __init__(self, address, count, brightness, backend):
        import board

        self.i2c = board.I2C()
        self.spi = None
        self.pixels = None
        try:
            self.sensor, self.sensor_type = create_sensor(self.i2c, address)
            logging.getLogger('voicehome').info('Detected %s at I2C address %#x', self.sensor_type, address)
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
        return sample_sensor(self.sensor, self.sensor_type)

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
