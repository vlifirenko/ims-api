package com.uploader;

import com.google.gson.Gson;
import com.uploader.helpers.MD5Helper;
import com.uploader.vos.FileVo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

public class FileParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileParser.class);

    private String rootPath;
    private String token;
    private final String apiRoot;

    public FileParser(String rootPath) {
        this.rootPath = rootPath;
        String userDir = System.getProperty("user.home");
        InputStream input = null;
        Properties properties = new Properties();
        try {
            input = new FileInputStream(userDir + File.separator + "uploader.properties");
            properties.load(input);
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }
        String email = properties.getProperty("email");
        String password = properties.getProperty("password");
        apiRoot = properties.getProperty("api.root");
        try {
            auth(email, password);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    public void parse() {
        try {
            getFiles(rootPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFiles(String path) throws IOException {
        String tags = path.substring(rootPath.length());
        String tagsArr[] = null;
        if (tags.length() > 0) {
            if (tags.contains(File.separator))
                tagsArr = tags.substring(1).split(Pattern.quote(System.getProperty("file.separator")));
            else
                tagsArr = new String[]{tags};
        }
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return;
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                FileVo fileVo = new FileVo(listOfFile.getAbsolutePath(), tagsArr);
                upload(fileVo);
            } else if (listOfFile.isDirectory()) {
                getFiles(listOfFile.getAbsolutePath());
            }
        }

    }

    private void auth(String email, String password) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpGet httpGet = new HttpGet(apiRoot + "/auth?email=" + email + "&password=" + password);
        HttpResponse response = httpclient.execute(httpGet);
        JSONObject obj = new JSONObject(EntityUtils.toString(response.getEntity()));
        token = obj.getString("token");
        httpclient.getConnectionManager().shutdown();
    }

    private void upload(FileVo fileVo) throws IOException {
        String guid = UUID.randomUUID().toString();
        File file = new File(fileVo.fileName);
        FileInputStream fis = new FileInputStream(file);
        String hash = MD5Helper.checkSumFromFile(file.getPath());
        fis.close();
        sendFile(apiRoot + "/files/" + guid + "/file", file, token, hash, fileVo);
    }

    public static void sendFile(String url, File file, String token, String hash, FileVo fileVo) throws IOException {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        HttpPost httppost = new HttpPost(url);
        HttpEntity entity;
        entity = MultipartEntityBuilder.create()
                .addTextBody("token", token)
                .addTextBody("hash", hash)
                .addTextBody("meta", fileVo.tags != null ?
                        String.format("{\"tags\":%s, \"title\":\"%s\"}", new Gson().toJson(fileVo.tags), file.getName()) :
                        String.format("{\"tags\":[], \"title\":\"%s\"}", file.getName()), ContentType.APPLICATION_JSON)
                .addBinaryBody("file", file,
                        ContentType.create("application/octet-stream"), file.getName()).build();
        httppost.setEntity(entity);
        System.out.println("executing request " + httppost.getRequestLine());
        httpclient.execute(httppost);
        httpclient.getConnectionManager().shutdown();
    }
}
