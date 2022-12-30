package com.ims.vos;

public class InstallFileVo {

    public String platform;
    public String platformVersion;
    public String appVersion;
    public String url;
    public String guid;

    public InstallFileVo(String url, String platform, String platformVersion, String appVersion, String guid) {
        this.url = url;
        this.platform = platform;
        this.platformVersion = platformVersion;
        this.appVersion = appVersion;
        this.guid = guid;
    }

    @Override
    public String toString() {
        return "{\"url\": \"" + this.url + "\"}";
    }
}
