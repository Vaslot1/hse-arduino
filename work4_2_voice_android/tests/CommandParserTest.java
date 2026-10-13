package com.example.voicehome;

import java.util.Arrays;
import java.util.List;

public final class CommandParserTest {
    private static int checks;

    private static void expect(String speech, String... expected) {
        List<String> actual = CommandParser.parse(speech);
        if (!actual.equals(Arrays.asList(expected))) {
            throw new AssertionError(speech + " -> " + actual + ", expected " + Arrays.asList(expected));
        }
        checks++;
    }

    public static void main(String[] args) {
        expect("Tell me the temperature", "temperature");
        expect("Pressure and humidity please", "pressure", "humidity");
        expect("sensor data", "temperature", "pressure", "humidity");
        expect("So, tell me the sensor data and also separately the values of temperature, pressure and humidity along with the weather forecast, and switch the lights on, color random",
                "temperature", "pressure", "humidity", "weather", "random");
        expect("Hello, please turn on the lights!", "FFFFFF");
        expect("turn on the light color red", "FF0000");
        expect("Turn the blue lights on", "0000FF");
        expect("LIGHTS OFF", "000000");
        expect("switch off the green light", "000000");
        expect("light on colour purple", "FF00FF");
        expect("set the lights to yellow", "FFFF00");
        expect("turn on the green lights", "00FF00");
        expect("turn on white lights", "FFFFFF");
        expect("weather forecast", "weather");
        expect("information about office conditions");
        expect("light condition");
        expect("officer in the lighthouse");
        expect("light green tea");
        expect("please turn on music");
        expect(" ");
        expect(null);
        if (!SpeechResponses.forCommand("pressure", "760").contains("millimeters of mercury")) {
            throw new AssertionError("Pressure must be spoken in mmHg");
        }
        if (!SpeechResponses.forCommand("000000", "000000").contains("off")) {
            throw new AssertionError("Off command must not be described as on");
        }
        String noHumidity = SpeechResponses.forCommand("humidity", "unavailable");
        if (!noHumidity.contains("does not measure humidity") || noHumidity.contains("percent")) {
            throw new AssertionError("BMP280 must not report missing humidity as a measurement");
        }
        if (!SpeechResponses.forCommand("humidity", "45").contains("45 percent")) {
            throw new AssertionError("BME280 humidity must remain a real measurement");
        }
        System.out.println("PASS: " + checks + " command phrases and 4 spoken responses");
    }
}
