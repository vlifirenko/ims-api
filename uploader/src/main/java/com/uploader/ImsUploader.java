package com.uploader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ImsUploader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImsUploader.class);

    public static void main(final String[] args) throws Exception {
        String path = args[0];
        FileParser fileParser = new FileParser(path);
        fileParser.parse();
    }
}
