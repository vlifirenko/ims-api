package com.ims.dtos;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class ClientInfoDto {
    private final Logger LOGGER = LoggerFactory.getLogger(ClientInfoDto.class);

    private HashMap<String, String> values = new HashMap<String, String>();

    public ClientInfoDto(Map<String, Deque<String>> parameters) {
        for (Map.Entry<String, Deque<String>> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Deque<String> value = entry.getValue();
            values.put(key, StringEscapeUtils.escapeHtml3(value.getFirst()));
        }
    }

    public boolean isValid() {
        return values.containsKey("platform") && values.containsKey("platformVersion") && values.containsKey("appVersion");
    }

    public String getProperty(String name) {
        return values.get(name);
    }
}
