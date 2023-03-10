package com.ims.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class StringHelper {
    private final Logger LOGGER = LoggerFactory.getLogger(StringHelper.class);

    public static String generateString(Random rng, String characters, int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = characters.charAt(rng.nextInt(characters.length()));
        }
        return new String(text);
    }

}
