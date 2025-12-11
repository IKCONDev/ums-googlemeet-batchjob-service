package com.ikn.ums.googlemeet.utils;

import java.util.Map;

public class UrlBuilderUtil {

    /**
     * Replaces placeholders in the form {key} with values from the map.
     * Example:
     * template: "https://www.googleapis.com/calendar/v3/calendars/{calendarId}/events/{eventId}"
     * values:   calendarId -> "primary"
     *           eventId -> "abc123"
     */
    public static String buildUrl(String template, Map<String, String> values) {
        String finalUrl = template;

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            finalUrl = finalUrl.replace(placeholder, entry.getValue());
        }

        return finalUrl;
    }
}
