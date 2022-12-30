package com.ims.dao.impl;

import com.ims.Settings;
import com.ims.daos.impl.FileDaoMongo;
import com.ims.vos.FileVo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class FileDaoMongoTest {

    private FileDaoMongo fileDao;

    @Before
    public void before() throws Exception {
        Settings.getInstance().setProperty(Settings.PROPERTY_MONGO_URI, "mongodb://localhost");
        fileDao = new FileDaoMongo();
        Assert.assertNotNull("fileDao is null", fileDao);
    }

    @Test
    public void saveFileTest() throws Exception {
        String hash = "hash";
        String path = "path";
        FileVo file = new FileVo(hash, path);
        Assert.assertNotNull("save file error", fileDao.saveFile(file));
    }
}
