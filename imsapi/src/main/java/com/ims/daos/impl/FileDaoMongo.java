package com.ims.daos.impl;

import com.ims.daos.FileDao;
import com.ims.helpers.DBPool;
import com.ims.vos.FileVo;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class FileDaoMongo implements FileDao {
    private final Logger LOGGER = LoggerFactory.getLogger(FileDaoMongo.class);

    private static final String COLLECTION = "files";
    private static final String FIELD_HASH = "hash";
    private static final String FIELD_PATH = "path";
    private DBCollection filesColl;

    public FileDaoMongo() throws UnknownHostException {
        DB db = DBPool.getDb();
        filesColl = db.getCollection(COLLECTION);
        filesColl.ensureIndex(FIELD_HASH);
    }

    @Override
    public String saveFile(FileVo file) {
        BasicDBObject object = new BasicDBObject(FIELD_HASH, file.hash)
                .append(FIELD_PATH, file.path);
        try {
            filesColl.insert(object);
            ObjectId id = (ObjectId) object.get("_id");
            return id.toString();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    @Override
    public void updateFile(FileVo file) {
        DBObject object = filesColl.findOne(new BasicDBObject("_id", new ObjectId(file.id)));
        filesColl.update(object, new BasicDBObject("$set", new BasicDBObject("path", file.path)));
    }

    @Override
    public FileVo getByHash(String hash) {
        DBObject query = new BasicDBObject(FIELD_HASH, hash);
        return dbObject2File(filesColl.findOne(query));
    }

    @Override
    public void deleteFile(FileVo file) {
        DBObject query = new BasicDBObject(FIELD_HASH, file.hash);
        filesColl.remove(filesColl.findOne(query));
    }

    private FileVo dbObject2File(DBObject obj) {
        if (obj == null) {
            return null;
        }
        return new FileVo(obj.get("_id").toString(),
                (String) obj.get("hash"),
                (String) obj.get("path")
        );
    }
}
