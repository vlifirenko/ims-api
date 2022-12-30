package com.ims;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Settings {
    private final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    public static final String PROPERTY_HOST = "host";
    public static final String PROPERTY_PORT = "port";
    public static final String PROPERTY_UPDATES_PATH = "updates.path";
    public static final String PROPERTY_UPDATES_URL = "updates.url";
    public static final String PROPERTY_FILES_DIR = "files_dir";
    public static final String PROPERTY_MONGO_URI = "mongo.uri";
    public static final String PROPERTY_MAX_POST_SIZE = "max.post.size";
    public static final String PROPERTY_SOLR_URL = "solr.url";
    public static final String PROPERTY_THUMB_MAX_SIZE = "thumb.max.size";
    public static final String PROPERTY_THUMB_PATH = "thumb.path";

    private static Settings instance;
    private Properties properties;

    public static synchronized Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    public Settings() {
        if (properties == null)
            properties = new Properties();
    }

    public void setSettingsPath(String settingsUrl) {
        InputStream input = null;
        try {
            input = new FileInputStream(settingsUrl);
            properties.load(input);
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }
    }

    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

}
