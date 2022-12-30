package com.uploader.vos;

import java.util.Arrays;

public class FileVo {

    public String fileName;
    public String[] tags;

    public FileVo(String fileName, String[] tags) {
        this.fileName = fileName;
        this.tags = tags;
    }

    public FileVo(String[] tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return String.format("fileName:%s,tags:%s", fileName, tags != null ? Arrays.toString(tags) : "");
    }
}
