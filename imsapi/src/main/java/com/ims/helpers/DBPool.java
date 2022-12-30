package com.ims.helpers;

import com.ims.Settings;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import java.net.UnknownHostException;

public class DBPool {

    private static DB db = null;

    public static DB getDb() throws UnknownHostException {
        if (db == null) {
            MongoClientURI mongoUri = new MongoClientURI(Settings.getInstance().getProperty(Settings.PROPERTY_MONGO_URI));
            MongoClient mongoClient = new MongoClient(mongoUri);
            db = mongoClient.getDB("ims");
        }
        return db;
    }
}
