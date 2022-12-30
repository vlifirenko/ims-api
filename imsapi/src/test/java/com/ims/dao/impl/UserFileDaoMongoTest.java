package com.ims.dao.impl;

import com.ims.Settings;
import com.ims.daos.impl.UserFileDaoMongo;
import com.ims.vos.UserFileVo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class UserFileDaoMongoTest {

    private UserFileDaoMongo userFileDao;

    @Before
    public void before() throws Exception {
        Settings.getInstance().setProperty(Settings.PROPERTY_MONGO_URI, "mongodb://localhost");
        userFileDao = new UserFileDaoMongo();
        Assert.assertNotNull("userFileDao is null", userFileDao);
    }

    @Test
    public void saveUserFileTest() throws Exception {
        String uid = "uid";
        String guid = "guid";
        String metaPath = "meta_path";
        String metaName = "meta_name";
        String fileHash = "file_hash";
        String fileName = "file_name";
        UserFileVo userFile = new UserFileVo(uid, guid, metaPath, metaName, fileHash, fileName, new Date().getTime());
        Assert.assertNotNull("save user file error", userFileDao.saveUserFile(userFile));
    }

    @Test
    public void getUserFilesTest() throws Exception {
        saveUserFileTest();
        List<UserFileVo> userFiles = userFileDao.getUserFiles();
        Assert.assertNotNull("user file list is null", userFiles);
        Assert.assertTrue("user file list is empty", userFiles.size() > 0);
    }

}
