package com.ims;

import com.ims.daos.FileDao;
import com.ims.daos.UserFileDao;
import com.ims.daos.impl.FileDaoMongo;
import com.ims.daos.impl.UserFileDaoMongo;
import com.ims.helpers.FileUtils;
import com.ims.helpers.SolrClientHelper;
import com.ims.helpers.StringHelper;
import com.ims.vos.FileVo;
import com.ims.vos.UserFileVo;
import com.ims.vos.UserVo;
import io.undertow.util.StatusCodes;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class ImsApiServerFilesTest {
    private final Logger LOGGER = LoggerFactory.getLogger(ImsApiServerFilesTest.class);

    private ImsApiServer subject;

    @Before
    public void before() throws Exception {
        subject = new ImsApiServer();
        subject.buildAndStartServer(8080, "127.0.0.1");
        String workingDir = System.getProperty("user.dir");
        Settings.getInstance().setSettingsPath(workingDir + File.separator + "ims.properties");
    }

    @After
    public void after() throws Exception {
        subject.stopServer();
    }

    @Test
    public void uploadFileTest() throws Exception {
        String token = ImsApiServerTest.registerUser().token;
        String guid = "guid";
        String hash = "hash";
        HttpResponse response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/meta", "file.json", "test_data_test_data", token, hash, "file.data");
        Assert.assertTrue("file don't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/file", "file.data", "test_data_test_data2", token, hash, null);
        Assert.assertTrue("file don't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        ImsApiServerTest.deleteUser(token);
    }

    @Test
    public void updateFileTest() throws NoSuchAlgorithmException, IOException {
        String token1 = ImsApiServerTest.registerUser().token;
        String token2 = ImsApiServerTest.registerUser().token;
        // user 1 upload file
        HttpResponse response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/guid1/meta", "file1.json", "test_data_test_data", token1, "hash1", "file1.data");
        Assert.assertTrue("file isn't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/guid1/file", "file1.data", "test_data_test_data2", token1, "hash1", null);
        Assert.assertTrue("file isn't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        // user 2 upload file
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/guid2/meta", "file1.json", "test_data_test_data", token2, "hash1", "file1.data");
        Assert.assertTrue("file isn't found", response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        // user 1 update file
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/guid1/meta", "file1.json", "test_data_test_data22", token1, "hash2", "file1.data");
        Assert.assertTrue("file isn't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/guid1/file", "file1.data", "test_data_test_data2", token1, "hash2", null);
        Assert.assertTrue("file isn't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        // user 2 get old file
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/guid2/meta", "file1.json", "test_data_test_data", token2, "hash1", "file1.data");
        Assert.assertTrue("file isn't found", response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        // check files
        FileDao fileDao = new FileDaoMongo();
        FileVo fileVo1 = fileDao.getByHash("hash1");
        FileVo fileVo2 = fileDao.getByHash("hash2");
        Assert.assertNotNull("file with hash1 is null", fileVo1);
        Assert.assertNotNull("file with hash2 is null", fileVo2);
        File file1 = new File(fileVo1.path);
        File file2 = new File(fileVo2.path);
        Assert.assertTrue("file with hash1 not found", file1.exists());
        Assert.assertTrue("file with hash2 not found", file2.exists());

        ImsApiServerTest.deleteUser(token1);
        ImsApiServerTest.deleteUser(token2);
    }

    @Test
    public void fileListTest() throws NoSuchAlgorithmException, IOException {
        UserVo user = ImsApiServerTest.registerUser();
        List<UserFileVo> fileVoList = new ArrayList<UserFileVo>();
        UserFileDao userFileDao = new UserFileDaoMongo();
        for (int i = 0; i < 5; i++) {
            UserFileVo userFileVo = new UserFileVo(user.id, "guid" + i, "{}", "metaname", "filename", "hash", new Date().getTime());
            fileVoList.add(userFileVo);
            userFileDao.saveUserFile(userFileVo);
        }
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files?token=" + user.token);
        HttpResponse response = httpclient.execute(httpGet);
        Assert.assertTrue("File list error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        LOGGER.info(EntityUtils.toString(response.getEntity()));
        httpclient.getConnectionManager().shutdown();
        for (UserFileVo userFileVo : fileVoList) {
            userFileDao.deleteUserFile(userFileVo);
        }
        ImsApiServerTest.deleteUser(user.token);
    }

    @Test
    public void fileListTestWithTag() throws NoSuchAlgorithmException, IOException {
        UserVo user = ImsApiServerTest.registerUser();
        List<UserFileVo> fileVoList = new ArrayList<UserFileVo>();
        UserFileDao userFileDao = new UserFileDaoMongo();
        for (int i = 0; i < 3; i++) {
            UserFileVo userFileVo = new UserFileVo(user.id, "guid_tag1_" + i, "{\"tags\":[\"tag1\"]}", "metaname", "filename", "hash", new Date().getTime());
            fileVoList.add(userFileVo);
            userFileDao.saveUserFile(userFileVo);
        }
        for (int i = 0; i < 3; i++) {
            UserFileVo userFileVo = new UserFileVo(user.id, "guid_tag2_" + i, "{\"tags\":[\"tag2\"]}", "metaname", "filename", "hash", new Date().getTime());
            fileVoList.add(userFileVo);
            userFileDao.saveUserFile(userFileVo);
        }
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files?token=" + user.token + "&tag=tag2");
        HttpResponse response = httpclient.execute(httpGet);
        Assert.assertTrue("File list error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        LOGGER.info(EntityUtils.toString(response.getEntity()));
        httpclient.getConnectionManager().shutdown();
        for (UserFileVo userFileVo : fileVoList) {
            userFileDao.deleteUserFile(userFileVo);
        }
        ImsApiServerTest.deleteUser(user.token);
    }

    @Test
    public void fileListTestWitSort() throws NoSuchAlgorithmException, IOException {
        UserVo user = ImsApiServerTest.registerUser();
        List<UserFileVo> fileVoList = new ArrayList<UserFileVo>();
        UserFileDao userFileDao = new UserFileDaoMongo();
        for (int i = 0; i < 5; i++) {
            UserFileVo userFileVo = new UserFileVo(user.id, "guid__" + i, "{\"title\":[\" " + StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10) + "\"]}", "metaname", "filename", "hash", new Date().getTime());
            fileVoList.add(userFileVo);
            userFileDao.saveUserFile(userFileVo);
        }
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files?token=" + user.token + "&sort.field=meta.title&sort.desc=1");
        HttpResponse response = httpclient.execute(httpGet);
        Assert.assertTrue("File list error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        LOGGER.info(EntityUtils.toString(response.getEntity()));
        httpclient.getConnectionManager().shutdown();
        for (UserFileVo userFileVo : fileVoList) {
            userFileDao.deleteUserFile(userFileVo);
        }
        ImsApiServerTest.deleteUser(user.token);
    }

    @Test
    public void getFileTest() throws NoSuchAlgorithmException, IOException {
        String token = ImsApiServerTest.registerUser().token;
        String guid = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10), password = "test_password";
        HttpResponse response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/meta", "file1.json", "test_data_test_data", token, "hash1", "file1.data");
        Assert.assertTrue("file isn't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/file", "file1.data", "test_data_test_data2", token, "hash1", null);
        Assert.assertTrue("file isn't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        EntityUtils.consumeQuietly(response.getEntity());
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("get file error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        String responseAsString = EntityUtils.toString(response.getEntity());
        final JSONObject obj = new JSONObject(responseAsString);
        String metaUrl = obj.getString("meta");
        String fileUrl = obj.getString("file");
        // download meta
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://" + metaUrl + "?token=" + token);
        response = httpclient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Meta download failed", entity);
        File outputFile = new File(FileUtils.getTempDir() + File.separator + "test.file");
        InputStream inputStream = entity.getContent();
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        int read = 0;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            fileOutputStream.write(bytes, 0, read);
        }
        fileOutputStream.close();
        Assert.assertTrue("meta download error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        Assert.assertTrue("meta file lenght is 0", outputFile.length() > 0);
        // download file
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://" + fileUrl + "?token=" + token);
        response = httpclient.execute(httpGet);
        entity = response.getEntity();
        Assert.assertNotNull("File download failed", entity);
        outputFile = new File(FileUtils.getTempDir() + File.separator + "test.file");
        inputStream = entity.getContent();
        fileOutputStream = new FileOutputStream(outputFile);
        read = 0;
        bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            fileOutputStream.write(bytes, 0, read);
        }
        fileOutputStream.close();
        Assert.assertTrue("file download error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        Assert.assertTrue("file file lenght is 0", outputFile.length() > 0);
        httpclient.getConnectionManager().shutdown();
        ImsApiServerTest.deleteUser(token);
        outputFile.delete();
    }

    @Test
    public void deleteFileTest() throws IOException, NoSuchAlgorithmException {
        UserVo user = ImsApiServerTest.registerUser();
        String guid = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10);
        String hash = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10);
        HttpResponse response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/meta", "file.json", "test_data_test_data", user.token, hash, "file.data");
        Assert.assertTrue("file don't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/file", "file.data", "test_data_test_data2", user.token, hash, null);
        Assert.assertTrue("file don't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        DefaultHttpClient httpclient = new DefaultHttpClient();
        HttpDelete httpDelete = new HttpDelete("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + user.token);
        response = httpclient.execute(httpDelete);
        Assert.assertTrue("delete file failed", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        ImsApiServerTest.deleteUser(user.token);
    }

    @Test
    public void ownerFileTest() throws NoSuchAlgorithmException, IOException {
        UserVo user1 = ImsApiServerTest.registerUser();
        UserVo user2 = ImsApiServerTest.registerUser();
        String guid = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10), password = "test_password";
        HttpResponse response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/meta", "file1.json", "test_data_test_data", user1.token, "hash1", "file1.data");
        Assert.assertTrue("file isn't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        response = ImsApiServerTest.sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/file", "file1.data", "test_data_test_data2", user1.token, "hash1", null);
        Assert.assertTrue("file isn't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        EntityUtils.consumeQuietly(response.getEntity());
        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + user2.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("error owner file", response.getStatusLine().getStatusCode() == StatusCodes.BAD_REQUEST);
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + user1.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("get file error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        String responseAsString = EntityUtils.toString(response.getEntity());
        final JSONObject obj = new JSONObject(responseAsString);
        String metaUrl = obj.getString("meta");
        String fileUrl = obj.getString("file");
        // download meta
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://" + metaUrl + "?token=" + user1.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("get meta error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://" + metaUrl + "?token=" + user2.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("error owner meta", response.getStatusLine().getStatusCode() == StatusCodes.BAD_REQUEST);
        // download file
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://" + fileUrl + "?token=" + user1.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("get file error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://" + fileUrl + "?token=" + user2.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("error owner file", response.getStatusLine().getStatusCode() == StatusCodes.BAD_REQUEST);

        httpclient.getConnectionManager().shutdown();
        ImsApiServerTest.deleteUser(user1.token);
        ImsApiServerTest.deleteUser(user2.token);
    }

    @Test
    public void shareFileTest() {
        //TODO shareFileTest
    }
}
