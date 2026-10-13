package com.example.voicehome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Превращает английскую фразу в последовательность HTTP-команд. */
public final class CommandParser {
    private static final Map<String, String> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put("red", "FF0000");
        COLORS.put("green", "00FF00");
        COLORS.put("blue", "0000FF");
        COLORS.put("yellow", "FFFF00");
        COLORS.put("purple", "FF00FF");
        COLORS.put("white", "FFFFFF");
        COLORS.put("random", "random");
    }

    private CommandParser() { }

    private static boolean contains(String text, String words) {
        return Pattern.compile("\\b(?:" + words + ")\\b").matcher(text).find();
    }

    public static List<String> parse(String speech) {
        List<String> commands = new ArrayList<>();
        if (speech == null) return commands;
        String text = speech.toLowerCase(Locale.ROOT);
        boolean allData = contains(text, "sensor\\s+data");
        if (allData || contains(text, "temperature")) commands.add("temperature");
        if (allData || contains(text, "pressure")) commands.add("pressure");
        if (allData || contains(text, "humidity")) commands.add("humidity");
        if (contains(text, "weather")) commands.add("weather");
        if (contains(text, "lights?")) {
            if (contains(text, "off")) {
                commands.add("000000");
            } else if (contains(text, "on|set")) {
                String color = "FFFFFF";
                int lastPosition = -1;
                for (Map.Entry<String, String> entry : COLORS.entrySet()) {
                    Matcher matcher = Pattern.compile("\\b" + entry.getKey() + "\\b").matcher(text);
                    while (matcher.find()) {
                        if (matcher.start() > lastPosition) {
                            lastPosition = matcher.start();
                            color = entry.getValue();
                        }
                    }
                }
                commands.add(color);
            }
        }
        return commands;
    }
}
