package com.ims.dtos;

import io.undertow.server.handlers.form.FormData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class ShareInfoDto {
    private final Logger LOGGER = LoggerFactory.getLogger(ShareInfoDto.class);

    private HashMap<String, String> values = new HashMap<String, String>();

    public ShareInfoDto(FormData formData) {
        if (formData.getFirst("emails") != null)
            values.put("emails", formData.getFirst("emails").getValue());
        if (formData.getFirst("guids") != null)
            values.put("guids", formData.getFirst("guids").getValue());
    }

    public boolean isValid() {
        return values.containsKey("emails") && values.containsKey("guids");
    }

    public String getProperty(String name) {
        return values.get(name);
    }
}
