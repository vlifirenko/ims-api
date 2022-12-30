package com.ims.dtos;

import io.undertow.server.handlers.form.FormData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class RegistrationInfoDto {
    private final Logger LOGGER = LoggerFactory.getLogger(RegistrationInfoDto.class);

    private HashMap<String, String> values = new HashMap<String, String>();

    public RegistrationInfoDto(FormData formData) {
        if (formData.getFirst("email") != null)
            values.put("email", formData.getFirst("email").getValue());
        if (formData.getFirst("password") != null)
            values.put("password", formData.getFirst("password").getValue());
    }

    public boolean isValid() {
        return values.containsKey("email") && values.containsKey("password");
    }

    public String getProperty(String name) {
        return values.get(name);
    }
}
