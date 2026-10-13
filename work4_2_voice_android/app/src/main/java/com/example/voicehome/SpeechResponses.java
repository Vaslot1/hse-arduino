package com.example.voicehome;

/** Озвучивание использует команду конкретного запроса, без общей изменяемой переменной. */
public final class SpeechResponses {
    private SpeechResponses() { }

    public static String forCommand(String command, String body) {
        switch (command) {
            case "temperature": return "Current temperature is " + body + " degrees Celsius.";
            case "pressure": return "Current pressure is " + body + " millimeters of mercury.";
            case "humidity":
                return "unavailable".equals(body)
                        ? "This sensor does not measure humidity."
                        : "Current humidity is " + body + " percent.";
            case "weather": return "Current weather forecast is: " + body + ".";
            case "000000": return "The lights are off.";
            case "FF0000": return "The color is set to red.";
            case "00FF00": return "The color is set to green.";
            case "0000FF": return "The color is set to blue.";
            case "FFFF00": return "The color is set to yellow.";
            case "FF00FF": return "The color is set to purple.";
            case "FFFFFF": return "The lights are on. The color is set to white.";
            case "random": return "The color is set to a random color.";
            default: return body;
        }
    }
}
