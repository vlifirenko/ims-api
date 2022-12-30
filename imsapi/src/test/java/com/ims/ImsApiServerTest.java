package com.ims;

import com.ims.daos.UserDao;
import com.ims.daos.impl.UserDaoMongo;
import com.ims.helpers.FileUtils;
import com.ims.helpers.MD5Helper;
import com.ims.helpers.StringHelper;
import com.ims.vos.UserVo;
import io.undertow.util.StatusCodes;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
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
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class ImsApiServerTest {
    private final Logger LOGGER = LoggerFactory.getLogger(ImsApiServerTest.class);

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
    public void registrationAuthorizationUserTest() throws IOException {
        String email = "testemail";
        String password = "testpassword";
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost("http://127.0.0.1:8080/api/v1/register");
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("email", email));
        postParameters.add(new BasicNameValuePair("password", password));
        httppost.setEntity(new UrlEncodedFormEntity(postParameters));
        HttpResponse response = httpclient.execute(httppost);
        Assert.assertTrue("Registration error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        EntityUtils.consumeQuietly(response.getEntity());
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/auth?email=" + email + "&password=" + password);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("Authorization error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        UserDao userDao = new UserDaoMongo();
        UserVo user = userDao.getByEmail(email);
        userDao.deleteUser(user);
        httpclient.getConnectionManager().shutdown();
    }

    @Test
    public void deleteUserTest() throws NoSuchAlgorithmException, IOException {
        UserVo user = registerUser();
        //upload file
        String guid = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10);
        String hash = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10);
        HttpResponse response = sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/meta", "file.json", "test_data_test_data", user.token, hash, "file.data");
        Assert.assertTrue("file don't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        response = sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/file", "file.data", "test_data_test_data2", user.token, hash, null);
        Assert.assertTrue("file don't created", response.getStatusLine().getStatusCode() == StatusCodes.CREATED || response.getStatusLine().getStatusCode() == StatusCodes.FOUND);
        //get file
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + user.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("get file error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        //delete user
        httpclient = new DefaultHttpClient();
        HttpDelete httpDelete = new HttpDelete("http://127.0.0.1:8080/api/v1/user?token=" + user.token);
        response = httpclient.execute(httpDelete);
        Assert.assertTrue("delete user failed", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        //check deleted file
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + user.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("deleted user found", response.getStatusLine().getStatusCode() == StatusCodes.UNAUTHORIZED);

        httpclient.getConnectionManager().shutdown();
    }

    @Test
    public void updatesTest() throws IOException {
        String[] updatesName = {"setup_2.0.4.exe", "setup_0.9.1.exe", "setup_1.0.10.exe", "setup_1.0.9.exe",
                "setup_1.1.15.exe", "setup_1.1.5.exe"};
        String updatesFolder = Settings.getInstance().getProperty(Settings.PROPERTY_UPDATES_PATH);
        String data = "test_data";
        String platform = "win";
        String platformVersion = "7";
        updatesFolder += File.separator + platform + File.separator + platformVersion + File.separator;
        FileUtils.createFolder(updatesFolder);
        List<File> files = new ArrayList<File>();
        for (String updateName : updatesName) {
            File file = new File(updatesFolder + updateName);
            FileUtils.writeToFile(data, file.toString());
            files.add(file);
        }
        //get last update
        String appVersion = "1.0.10";
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet =
                new HttpGet("http://127.0.0.1:8080/api/v1/updates?platform=" + platform + "&platformVersion=" + platformVersion + "&appVersion=" + appVersion);
        HttpResponse response = httpclient.execute(httpGet);
        Assert.assertTrue("Get updates error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        String responseAsString = EntityUtils.toString(response.getEntity());
        String correctResponse = "{\"url\": \"win/7/setup_2.0.4.exe\"}";
        Assert.assertTrue("Get updates response isn't valid", responseAsString.equals(correctResponse));
        // if you have last version of update
        appVersion = "2.0.4";
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/updates?platform=" + platform + "&platformVersion=" + platformVersion + "&appVersion=" + appVersion);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("Get updates error", response.getStatusLine().getStatusCode() == StatusCodes.NOT_FOUND);
    }

    @Test
    public void shareTest() throws IOException, NoSuchAlgorithmException {
        UserVo user1 = registerUser();
        UserVo user2 = registerUser();
        // first user upload file
        String guid = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10);
        String hash = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10);
        HttpResponse response = sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/meta", "file.json", "{}", user1.token, hash, "file.data");
        response = sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/file", "file.data", "test_data_test_data2", user1.token, hash, null);
        // user 2 get file
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + user2.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("owner error", response.getStatusLine().getStatusCode() == StatusCodes.BAD_REQUEST);
        // user 1 share file with user 2
        EntityUtils.consumeQuietly(response.getEntity());
        HttpPost httppost = new HttpPost("http://127.0.0.1:8080/api/v1/files/share");
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("emails", user2.email));
        postParameters.add(new BasicNameValuePair("guids", guid));
        postParameters.add(new BasicNameValuePair("token", user1.token));
        httppost.setEntity(new UrlEncodedFormEntity(postParameters));
        response = httpclient.execute(httppost);
        Assert.assertTrue("Share error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        EntityUtils.consumeQuietly(response.getEntity());
        // user 2 get file
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + user2.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("owner error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        deleteUser(user1.token);
        deleteUser(user2.token);
    }

    @Test
    public void shareTest3Users() throws IOException, NoSuchAlgorithmException {
        UserVo user1 = registerUser();
        UserVo user2 = registerUser();
        UserVo user3 = registerUser();
        // first user upload file
        String guid = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10);
        String hash = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10);
        HttpResponse response = sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/meta", "file.json", "{}", user1.token, hash, "file.data");
        response = sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/file", "file.data", "test_data_test_data2", user1.token, hash, null);
        // user 2 create deduplicated copy
        response = sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/meta", "file.json", "{}", user2.token, hash, "file.data");
        response = sendFile("http://127.0.0.1:8080/api/v1/files/" + guid + "/file", "file.data", "test_data_test_data2", user2.token, hash, null);
        // user 2 share file with user 3
        EntityUtils.consumeQuietly(response.getEntity());
        HttpPost httppost = new HttpPost("http://127.0.0.1:8080/api/v1/files/share");
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("emails", user3.email));
        postParameters.add(new BasicNameValuePair("guids", guid));
        postParameters.add(new BasicNameValuePair("token", user2.token));
        httppost.setEntity(new UrlEncodedFormEntity(postParameters));
        HttpClient httpclient = new DefaultHttpClient();
        response = httpclient.execute(httppost);
        Assert.assertTrue("Share error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        EntityUtils.consumeQuietly(response.getEntity());
        // user 3 get file
        httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/files/" + guid + "?token=" + user3.token);
        response = httpclient.execute(httpGet);
        Assert.assertTrue("owner error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        deleteUser(user1.token);
        deleteUser(user2.token);
        deleteUser(user3.token);
    }

    @Test
    public void graphGexfTest() throws Exception {
        UserVo user = loginTestUser();
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet("http://127.0.0.1:8080/api/v1/graph?token=" + user.token + "&format=gexf");
        HttpResponse response = httpclient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        Assert.assertNotNull("Get graph gexf failed", entity);
        File outputFile = new File(FileUtils.getTempDir() + File.separator + "test.file");
        InputStream inputStream = entity.getContent();
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        int read;
        byte[] bytes = new byte[1024];
        while ((read = inputStream.read(bytes)) != -1) {
            fileOutputStream.write(bytes, 0, read);
        }
        fileOutputStream.close();
        Assert.assertTrue("Get graph gexf error", response.getStatusLine().getStatusCode() == StatusCodes.OK);
        Assert.assertTrue("meta file lenght is 0", outputFile.length() > 0);
        outputFile.delete();
        httpclient.getConnectionManager().shutdown();
    }

    public static UserVo registerUser() throws UnknownHostException, NoSuchAlgorithmException {
        String email = StringHelper.generateString(new Random(), "qwertyuiopasdfghjkl", 10), password = "test_password";
        UserDao userDao = new UserDaoMongo();
        password = MD5Helper.getMD5(password);
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        String randomNum = Integer.toString(prng.nextInt());
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] result = sha.digest(randomNum.getBytes());
        String token = MD5Helper.hexEncode(result);
        UserVo user = new UserVo(email, password, token);
        user.id = userDao.saveUser(user);
        return user;
    }

    public static UserVo loginTestUser() throws IOException {
        UserDao userDao = new UserDaoMongo();
        return userDao.getByEmail("test");
    }

    public static void deleteUser(String token) throws UnknownHostException {
        UserDao userDao = new UserDaoMongo();
        UserVo user = userDao.getByToken(token);
        userDao.deleteUser(user);
    }

    public static HttpResponse sendFile(String url, String fileName, String data, String token, String hash, String nextFilename) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        // download meta info
        HttpPost httppost = new HttpPost(url);
        String tempDir = FileUtils.getTempDir();
        File file = new File(tempDir + File.separator + fileName);
        FileUtils.writeToFile(data, file.toString());
        HttpEntity entity;
        if (nextFilename != null) {
            entity = MultipartEntityBuilder.create()
                    .addTextBody("token", token)
                    .addTextBody("hash", hash)
                    .addTextBody("filename", nextFilename)
                    .addBinaryBody("file", file,
                            ContentType.create("application/octet-stream"), file.getName()).build();
        } else {
            entity = MultipartEntityBuilder.create()
                    .addTextBody("token", token)
                    .addTextBody("hash", hash)
                    .addBinaryBody("file", file,
                            ContentType.create("application/octet-stream"), file.getName()).build();
        }
        httppost.setEntity(entity);
        System.out.println("executing request " + httppost.getRequestLine());
        HttpResponse response = httpclient.execute(httppost);
        httpclient.getConnectionManager().shutdown();
        return response;
    }
}
