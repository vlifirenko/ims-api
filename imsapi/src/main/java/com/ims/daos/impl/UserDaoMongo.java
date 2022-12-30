package com.ims.daos.impl;

import com.ims.daos.UserDao;
import com.ims.helpers.DBPool;
import com.ims.vos.UserVo;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class UserDaoMongo implements UserDao {
    private final Logger LOGGER = LoggerFactory.getLogger(UserDaoMongo.class);

    private static final String COLLECTION = "users";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PASSWORD = "password";
    private static final String FIELD_TOKEN = "token";
    private DBCollection usersColl;

    public UserDaoMongo() throws UnknownHostException {
        DB db = DBPool.getDb();
        usersColl = db.getCollection(COLLECTION);
        usersColl.ensureIndex(FIELD_EMAIL);
        usersColl.ensureIndex(FIELD_TOKEN);
    }

    @Override
    public String saveUser(UserVo user) {
        BasicDBObject userObj = new BasicDBObject(FIELD_EMAIL, user.email)
                .append(FIELD_PASSWORD, user.password)
                .append(FIELD_TOKEN, user.token);
        try {
            usersColl.insert(userObj);
            ObjectId id = (ObjectId) userObj.get("_id");
            return id.toString();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
    }

    @Override
    public List<UserVo> getUsers() {
        List<UserVo> users = new ArrayList<UserVo>();
        DBCursor cursor;
        try {
            cursor = usersColl.find();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
        while (cursor.hasNext()) {
            UserVo user = dbObject2User(cursor.next());
            users.add(user);
        }
        return users;
    }

    @Override
    public UserVo getByEmail(String email) {
        DBObject query = new BasicDBObject(FIELD_EMAIL, email);
        return dbObject2User(usersColl.findOne(query));
    }

    @Override
    public UserVo getByToken(String token) {
        DBObject query = new BasicDBObject(FIELD_TOKEN, token);
        return dbObject2User(usersColl.findOne(query));
    }

    @Override
    public void deleteUser(UserVo user) {
        DBObject query = new BasicDBObject(FIELD_EMAIL, user.email);
        usersColl.remove(usersColl.findOne(query));
    }

    private UserVo dbObject2User(DBObject obj) {
        if (obj == null) {
            return null;
        }
        return new UserVo(obj.get("_id").toString(),
                (String) obj.get("email"),
                (String) obj.get("password"),
                (String) obj.get("token")
        );
    }
}
