package com.ims.vos;

public class FileVo {

    public String id;
    public String hash;
    public String path;

    public FileVo(String hash, String path) {
        this.hash = hash;
        this.path = path;
    }

    public FileVo(String id, String hash, String path) {
        this.id = id;
        this.hash = hash;
        this.path = path;
    }
}
