package com.ims.daos.impl;

import com.ims.daos.UserFileDao;
import com.ims.helpers.DBPool;
import com.ims.vos.UserFileVo;
import com.mongodb.*;
import com.mongodb.util.JSON;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class UserFileDaoMongo implements UserFileDao {
    private final Logger LOGGER = LoggerFactory.getLogger(UserFileDaoMongo.class);

    public static final String COLLECTION = "user_files";
    public static final String FIELD_ID = "_id";
    public static final String FIELD_UID = "uid";
    public static final String FIELD_GUID = "guid";
    public static final String FIELD_META = "meta";
    public static final String FIELD_META_NAME = "meta_name";
    public static final String FIELD_FILE_NAME = "file_name";
    public static final String FIELD_FILE_HASH = "file_hash";
    public static final String FIELD_CREATED = "created";
    private DBCollection userFilesColl;

    public UserFileDaoMongo() throws UnknownHostException {
        DB db = DBPool.getDb();
        userFilesColl = db.getCollection(COLLECTION);
        userFilesColl.ensureIndex(FIELD_FILE_HASH);
        userFilesColl.ensureIndex(FIELD_UID);
    }

    @Override
    public String saveUserFile(UserFileVo userFile) {
        BasicDBObject object = new BasicDBObject(FIELD_UID, userFile.uid)
                .append(FIELD_GUID, userFile.guid)
                .append(FIELD_META, JSON.parse(userFile.meta))
                .append(FIELD_META_NAME, userFile.metaName)
                .append(FIELD_FILE_NAME, userFile.fileName)
                .append(FIELD_FILE_HASH, userFile.fileHash)
                .append(FIELD_CREATED, userFile.created);
        try {
            userFilesColl.insert(object);
            ObjectId id = (ObjectId) object.get("_id");
            return id.toString();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    @Override
    public void updateUserFile(UserFileVo userFile) {
        DBObject object = userFilesColl.findOne(new BasicDBObject("_id", new ObjectId(userFile.id)));
        userFilesColl.update(object, new BasicDBObject("$set",
                new BasicDBObject(FIELD_META, JSON.parse(userFile.meta))
                        .append(FIELD_FILE_NAME, userFile.fileName)
                        .append(FIELD_FILE_HASH, userFile.fileHash)));
    }

    @Override
    public List<UserFileVo> getUserFiles() {
        List<UserFileVo> userFiles = new ArrayList<UserFileVo>();
        DBCursor cursor = userFilesColl.find();
        while (cursor.hasNext()) {
            UserFileVo userFile = dbObject2UserFile(cursor.next());
            userFiles.add(userFile);
        }
        return userFiles;
    }

    @Override
    public List<UserFileVo> getUserFilesByHash(String hash) {
        List<UserFileVo> userFiles = new ArrayList<UserFileVo>();
        DBObject query = new BasicDBObject(FIELD_FILE_HASH, hash);
        DBCursor cursor = userFilesColl.find(query);
        while (cursor.hasNext()) {
            UserFileVo userFile = dbObject2UserFile(cursor.next());
            userFiles.add(userFile);
        }
        return userFiles;
    }

    @Override
    public List<UserFileVo> getFiles(String uid, String offset, String limit, String tag, String sortField, String sortDesc) {
        List<UserFileVo> userFiles = new ArrayList<UserFileVo>();
        DBCursor cursor;
        try {
            DBObject query;
            if (tag == null || tag.equals("")) {
                query = new BasicDBObject(FIELD_UID, uid);
            } else {
                query = new BasicDBObject(FIELD_UID, uid).append("meta.tags", tag);
            }
            cursor = userFilesColl.find(query);
            int sort;
            if (sortDesc != null && sortDesc.equals("1"))
                sort = 1;
            else
                sort = -1;
            if (sortField == null)
                cursor.sort(new BasicDBObject(FIELD_CREATED, sort));
            else
                cursor.sort(new BasicDBObject(sortField, sort));
            if (offset != null && !offset.equals("0"))
                cursor.skip(Integer.parseInt(offset));
            if (limit != null && !limit.equals("0"))
                cursor.limit(Integer.parseInt(limit));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
        while (cursor.hasNext()) {
            UserFileVo file = dbObject2UserFile(cursor.next());
            userFiles.add(file);
        }
        return userFiles;
    }

    @Override
    public void deleteUserFile(UserFileVo userFile) {
        DBObject query = new BasicDBObject(FIELD_GUID, userFile.guid);
        userFilesColl.remove(userFilesColl.findOne(query));
    }

    @Override
    public UserFileVo getUserFileByUid(String uid) {
        DBObject query = new BasicDBObject(FIELD_UID, uid);
        return dbObject2UserFile(userFilesColl.findOne(query));
    }

    @Override
    public UserFileVo getUserFileByGuid(String guid, String uid) {
        DBObject query = new BasicDBObject(FIELD_GUID, guid).append(FIELD_UID, uid);
        return dbObject2UserFile(userFilesColl.findOne(query));
    }

    @Override
    public UserFileVo getUserFileById(String id) {
        DBObject query = new BasicDBObject(FIELD_ID, new ObjectId(id));
        return dbObject2UserFile(userFilesColl.findOne(query));
    }

    @Override
    public UserFileVo getUserFileByHash(String hash) {
        DBObject query = new BasicDBObject(FIELD_FILE_HASH, hash);
        return dbObject2UserFile(userFilesColl.findOne(query));
    }

    private UserFileVo dbObject2UserFile(DBObject obj) {
        if (obj == null) {
            return null;
        }
        long created = 0;
        if (obj.get(FIELD_CREATED) != null)
            created = (Long) obj.get(FIELD_CREATED);
        if (obj.get(FIELD_META) != null)
            return new UserFileVo(obj.get("_id").toString(),
                    (String) obj.get(FIELD_UID),
                    (String) obj.get(FIELD_GUID),
                    obj.get(FIELD_META).toString(),
                    (String) obj.get(FIELD_META_NAME),
                    (String) obj.get(FIELD_FILE_NAME),
                    (String) obj.get(FIELD_FILE_HASH),
                    created
            );
        else
            return new UserFileVo(obj.get("_id").toString(),
                    (String) obj.get(FIELD_UID),
                    (String) obj.get(FIELD_GUID),
                    null,
                    (String) obj.get(FIELD_META_NAME),
                    (String) obj.get(FIELD_FILE_NAME),
                    (String) obj.get(FIELD_FILE_HASH),
                    created
            );
    }
}
