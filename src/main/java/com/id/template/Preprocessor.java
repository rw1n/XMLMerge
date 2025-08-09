package com.id.template;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$%[^\\r\\n]*?\\$");

    public static String preprocess(String rawXml) {
        StringBuilder result = new StringBuilder();
        Matcher matcher = TEMPLATE_PATTERN.matcher(rawXml);

        int lastEnd = 0;
        while (matcher.find()) {
            String before = rawXml.substring(lastEnd, matcher.start());
            String template = matcher.group();

            result.append(before);
            result.append("<template original=\"")
                    .append(escapeXml(template))
                    .append("\"/>");

            lastEnd = matcher.end();
        }

        result.append(rawXml.substring(lastEnd));
        return result.toString();
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
