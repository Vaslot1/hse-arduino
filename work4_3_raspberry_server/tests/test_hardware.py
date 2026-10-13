import sys
import unittest
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import hardware


class FakeI2C:
    def __init__(self, chip_id):
        self.chip_id = chip_id
        self.locked = False

    def try_lock(self):
        self.locked = True
        return True

    def unlock(self):
        self.locked = False

    def writeto_then_readfrom(self, address, register, output):
        if address != 0x76 or register != b'\xd0' or not self.locked:
            raise OSError('Wrong sensor address, register or unlocked bus')
        output[0] = self.chip_id


class FakeBME280:
    temperature = 23.0
    pressure = 1013.25
    relative_humidity = 45.0

    def __init__(self, i2c, address):
        if i2c.chip_id != 0x60 or address != 0x76:
            raise RuntimeError('Wrong BME280 driver or address')


class FakeBMP280:
    temperature = 24.0
    pressure = 1000.0

    def __init__(self, i2c, address):
        if i2c.chip_id != 0x58 or address != 0x76:
            raise RuntimeError('Wrong BMP280 driver or address')

    @property
    def relative_humidity(self):
        raise AssertionError('BMP280 cannot measure humidity')


class HardwareTests(unittest.TestCase):
    def setUp(self):
        self.assertTrue(callable(getattr(hardware, 'create_sensor', None)), 'Sensor autodetection is not implemented')
        self.drivers = patch.dict(sys.modules, {
            'adafruit_bme280': SimpleNamespace(basic=SimpleNamespace(Adafruit_BME280_I2C=FakeBME280)),
            'adafruit_bmp280': SimpleNamespace(Adafruit_BMP280_I2C=FakeBMP280),
        })
        self.drivers.start()
        self.addCleanup(self.drivers.stop)

    def test_bme280_uses_humidity_capable_driver(self):
        bus = FakeI2C(0x60)
        sensor, kind = hardware.create_sensor(bus, 0x76)
        reading = hardware.sample_sensor(sensor, kind)
        self.assertEqual((reading.temperature, reading.pressure_hpa, reading.humidity), (23.0, 1013.25, 45.0))
        self.assertFalse(bus.locked)

    def test_bmp280_reads_pressure_and_temperature_without_humidity(self):
        bus = FakeI2C(0x58)
        sensor, kind = hardware.create_sensor(bus, 0x76)
        reading = hardware.sample_sensor(sensor, kind)
        self.assertEqual((reading.temperature, reading.pressure_hpa, reading.humidity), (24.0, 1000.0, None))
        self.assertFalse(bus.locked)

    def test_unknown_chip_is_not_mistaken_for_supported_sensor(self):
        bus = FakeI2C(0xff)
        with self.assertRaisesRegex(RuntimeError, '0xff'):
            hardware.create_sensor(bus, 0x76)
        self.assertFalse(bus.locked)


if __name__ == '__main__':
    unittest.main()
