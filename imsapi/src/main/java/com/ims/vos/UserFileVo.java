package com.ims.vos;

public class UserFileVo {

    public String id;
    public String uid;
    public String guid;
    public String metaName;
    public String meta;
    public String fileName;
    public String fileHash;
    public long created;

    public UserFileVo(String id, String uid, String guid, String meta, String metaName, String fileName, String fileHash, long created) {
        this.id = id;
        this.uid = uid;
        this.guid = guid;
        this.meta = meta;
        this.metaName = metaName;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.created = created;
    }

    public UserFileVo(String uid, String guid, String meta, String metaName, String fileName, String fileHash, long created) {
        this.uid = uid;
        this.guid = guid;
        this.meta = meta;
        this.metaName = metaName;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.created = created;
    }
}
