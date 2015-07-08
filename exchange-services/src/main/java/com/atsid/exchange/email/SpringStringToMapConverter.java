package com.atsid.exchange.email;

import org.apache.commons.lang.StringUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring converter that converts from a string to a map.  String uses commas for separation of entries and colons
 * for key:value.
 */
@Component
public class SpringStringToMapConverter implements Converter<String, Map<String, String>> {
    @Override
    public Map<String, String> convert(String inputString) {
        Map<String, String> stringMap = new HashMap<>();

        if (StringUtils.isBlank(inputString)) {
            return stringMap;
        }

        for (String entry : inputString.split(",")) {
            String[] items = entry.split(":");

            if (items.length != 2) {
                throw new IllegalStateException(String.format("Cannot parse key:value string for %s", entry));
            }

            stringMap.put(items[0].trim(), items[1].trim());
        }

        return stringMap;
    }
}
