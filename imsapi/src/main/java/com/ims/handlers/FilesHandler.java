package com.ims.handlers;

import com.ims.Settings;
import com.ims.daos.FileDao;
import com.ims.daos.UserFileDao;
import com.ims.daos.impl.FileDaoMongo;
import com.ims.daos.impl.UserFileDaoMongo;
import com.ims.helpers.*;
import com.ims.vos.FileVo;
import com.ims.vos.UserFileVo;
import com.ims.vos.UserVo;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.apache.commons.io.FilenameUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.*;

public class FilesHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(FilesHandler.class);

    private String[] pathParts;
    private UserVo userVo;

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // try{
        this.userVo = exchange.getAttachment(UserVo.USER_VO);
        pathParts = exchange.getRequestPath().split("/");
        String httpMethod = exchange.getRequestMethod().toString();
        if (httpMethod.equals("GET")) {
            if (pathParts.length == 5) {
                getFile(exchange);
                return;
            }
            if (pathParts.length == 4) {
                fileList(exchange);
                return;
            }
            if (pathParts[pathParts.length - 1].equals("preview")) {
                preview(exchange);
            }
            downloadFile(exchange);
            return;
        }
        FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);
        if (httpMethod.equals("POST")) {
            uploadFile(exchange, formData);
            return;
        }
        if (httpMethod.equals("DELETE")) {
            deleteFile(exchange);
        }
        // } catch(Exception e) {
        //    LOGGER.error(e);
        // }
    }

    private void preview(HttpServerExchange exchange) throws IOException {
        String guid = pathParts[pathParts.length - 1];
        String url = ThumbHelper.getThumb(guid);
        if (url == null) {
            exchange.setResponseCode(StatusCodes.NOT_FOUND);
            return;
        }
        File file = new File(url);
        exchange.setResponseCode(StatusCodes.OK);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
        exchange.getResponseHeaders().put(Headers.CONTENT_TRANSFER_ENCODING, "binary");
        exchange.setResponseContentLength(file.length());
        exchange.startBlocking();
        OutputStream os = exchange.getOutputStream();
        byte[] bufferData = new byte[1024];
        int read;
        InputStream fis = new FileInputStream(file);
        while ((read = fis.read(bufferData)) != -1) {
            os.write(bufferData, 0, read);
        }
        os.close();
        fis.close();
    }

    private void getFile(HttpServerExchange exchange) throws UnknownHostException {
        String guid = pathParts[pathParts.length - 1];
        UserFileDao userFileDao = new UserFileDaoMongo();
        UserFileVo userFile = userFileDao.getUserFileByGuid(guid, userVo.id);
        if (userFile == null) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            return;
        }
        FileDao fileDao = new FileDaoMongo();
        fileDao.getByHash(userFile.fileHash);
        String host = exchange.getHostAndPort();
        String result = "{\"meta\": \"" + host + "/api/v1/files/" + guid + "/meta\"," +
                "\"file\": \"" + host + "/api/v1/files/" + guid + "/file\"}";
        exchange.setResponseCode(StatusCodes.OK);
        exchange.getResponseSender().send(result);
    }

    private void downloadFile(HttpServerExchange exchange) throws IOException {
        String method = pathParts[pathParts.length - 1];
        String guid = pathParts[pathParts.length - 2];
        UserFileDao userFileDao = new UserFileDaoMongo();
        UserFileVo userFileVo = userFileDao.getUserFileByGuid(guid, userVo.id);
        if (userFileVo == null) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            return;
        }
        String fileName = userFileVo.fileName;
        if (method.equals("meta")) {
            if (fileName.contains(".")) fileName = fileName.substring(0, fileName.lastIndexOf('.')) + ".meta";
            byte[] bytes = userFileVo.meta.getBytes(Charset.forName("UTF-8"));
            exchange.setResponseCode(StatusCodes.OK);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
            exchange.getResponseHeaders().put(Headers.CONTENT_TRANSFER_ENCODING, "binary");
            exchange.setResponseContentLength(bytes.length);
            exchange.getResponseHeaders().put(Headers.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
            exchange.startBlocking();
            OutputStream os = exchange.getOutputStream();
            os.write(bytes);
            os.close();
            return;
        }
        FileDao fileDao = new FileDaoMongo();
        FileVo fileVo = fileDao.getByHash(userFileVo.fileHash);
        String path = fileVo.path;
        File file = new File(path);
        exchange.setResponseCode(StatusCodes.OK);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
        exchange.getResponseHeaders().put(Headers.CONTENT_TRANSFER_ENCODING, "binary");
        exchange.setResponseContentLength(file.length());
        exchange.getResponseHeaders().put(Headers.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        exchange.startBlocking();
        OutputStream os = exchange.getOutputStream();
        byte[] bufferData = new byte[1024];
        int read;
        InputStream fis = new FileInputStream(file);
        while ((read = fis.read(bufferData)) != -1) {
            os.write(bufferData, 0, read);
        }
        os.close();
        fis.close();
    }

    private void deleteFile(HttpServerExchange exchange) throws Exception {
        String guid = pathParts[pathParts.length - 1];
        if (guid == null) {
            exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.getResponseSender().send("{\"message\": \"guid is null\"}");
            return;
        }
        UserFileDao userFileDao = new UserFileDaoMongo();
        UserFileVo userFile = userFileDao.getUserFileByGuid(guid, userVo.id);
        if (userFile == null) {
            exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.getResponseSender().send("{\"message\": \"file with guid is not found\"}");
            return;
        }
        GephiWriter.delete(userVo);
        userFileDao.deleteUserFile(userFile);
        if (userFileDao.getUserFilesByHash(userFile.fileHash).size() == 0) {
            FileDao fileDao = new FileDaoMongo();
            FileVo fileVo = fileDao.getByHash(userFile.fileHash);
            java.io.File file = new java.io.File(fileVo.path);
            file.delete();
            fileDao.deleteFile(fileVo);
            SolrDocumentList documents = SolrClientHelper.getInstance().searchDocuments(null, "id:" + fileVo.id);
            for (SolrDocument doc : documents) {
                SolrClientHelper.getInstance().deleteDocument((String) doc.getFieldValue("id"));
            }
        }
        exchange.setResponseCode(StatusCodes.OK);
    }

    private void uploadFile(HttpServerExchange exchange, FormData formData) throws IOException {
        GephiWriter.delete(userVo);
        String hash = null;
        if (formData.getFirst("hash") != null) {
            hash = formData.getFirst("hash").getValue();
        }
        String uploadFilename = null;
        if (formData.getFirst("filename") != null) {
            uploadFilename = formData.getFirst("filename").getValue();
        }
        String guid = pathParts[pathParts.length - 2];
        String method = pathParts[pathParts.length - 1];
        ThumbHelper.deleteThumb(guid);
        FileDao fileDao = new FileDaoMongo();
        UserFileDao userFileDao = new UserFileDaoMongo();
        String uid = userVo.id;
        File file = formData.getFirst("file").getFile();
        String fileName = formData.getFirst("file").getFileName();
        if (method.equals("meta")) {
            // add metafile to db
            String meta = io.undertow.util.FileUtils.readFile(file);
            if (userFileDao.getUserFileByGuid(guid, userVo.id) == null) {
                // if upload
                UserFileVo userFileVo = new UserFileVo(uid, guid, meta, fileName, uploadFilename, hash, new Date().getTime());
                String id = userFileDao.saveUserFile(userFileVo);
                FileVo fileVo = fileDao.getByHash(hash);
                if (fileVo == null) {
                    exchange.setResponseCode(StatusCodes.CREATED);
                } else {
                    SolrClientHelper.getInstance().insertDocument(userFileVo.meta, id, fileVo, uid);
                    exchange.setResponseCode(StatusCodes.FOUND);
                }
            } else {
                // if update
                UserFileVo userFileVo = userFileDao.getUserFileByGuid(guid, userVo.id);
                userFileVo.fileHash = hash;
                userFileDao.updateUserFile(userFileVo);
                FileVo fv = fileDao.getByHash(hash);
                if (fv == null) {
                    exchange.setResponseCode(StatusCodes.CREATED);
                } else {
                    SolrClientHelper.getInstance().updateDocument(userFileVo.meta, fv.id, fileDao.getByHash(hash), uid);
                    exchange.setResponseCode(StatusCodes.FOUND);
                }
            }
            return;
        }
        if (method.equals("file")) {
            if (hash == null)
                hash = MD5Helper.checkSumFromFile(file.getPath());
            String meta = formData.getFirst("meta").getValue();
            FileVo fv = fileDao.getByHash(hash);
            if (fileDao.getByHash(hash) != null) {
                // update meta
                UserFileVo userFile = userFileDao.getUserFileByHash(hash);
                userFile.meta = meta;
                userFile.metaName = fileName;
                userFile.fileName = uploadFilename;
                SolrClientHelper.getInstance().updateDocument(userFile.meta, fv.id, fileDao.getByHash(hash), uid);
                exchange.setResponseCode(StatusCodes.FOUND);
                return;
            }
            if (formData.getFirst("meta") != null) {
                // add meta
                UserFileVo userFileVo = new UserFileVo(uid, guid, meta, fileName, uploadFilename, hash, new Date().getTime());
                userFileDao.saveUserFile(userFileVo);
            }
            String filesDir = Settings.getInstance().getProperty(Settings.PROPERTY_FILES_DIR);
            FileVo fileVo = new FileVo(hash, fileName);
            // add file to db
            String id = fileDao.saveFile(fileVo);
            FileUtils.createFolder(filesDir);
            java.io.File dest = new java.io.File(filesDir + java.io.File.separator +
                    id + "." + FilenameUtils.getExtension(fileName));
            io.undertow.util.FileUtils.copyFile(file, dest);
            fileVo.path = dest.getPath();
            fileVo.id = id;
            fileDao.updateFile(fileVo);
            UserFileVo userFileVo = userFileDao.getUserFileByGuid(guid, userVo.id);
            userFileVo.fileName = fileName;
            userFileDao.updateUserFile(userFileVo);
            SolrClientHelper.getInstance().insertDocument(userFileVo.meta, userFileVo.id, fileVo, uid);
            exchange.setResponseCode(StatusCodes.CREATED);
        }
    }

    private void fileList(HttpServerExchange exchange) throws UnknownHostException {
        String offset = null, limit = null, tag = null, sortField = null, sortDesc = null;
        Map<String, Deque<String>> parameters = exchange.getQueryParameters();
        if (parameters.get("offset") != null) {
            offset = parameters.get("offset").getFirst();
        }
        if (parameters.get("limit") != null) {
            limit = parameters.get("limit").getFirst();
        }
        if (parameters.get("tag") != null) {
            tag = parameters.get("tag").getFirst();
        }
        if (parameters.get("sortField") != null) {
            sortField = parameters.get("sortField").getFirst();
        }
        if (parameters.get("sortDesc") != null) {
            sortDesc = parameters.get("sortDesc").getFirst();
        }
        if (sortField == null && sortDesc != null) {
            exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.getResponseSender().send("{\"message\": \"sorting field is missing\"}");
            return;
        }

        UserFileDao userFileDao = new UserFileDaoMongo();
        List<UserFileVo> userFiles;
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (tag != null) {
            userFiles = new ArrayList<UserFileVo>();
            SolrDocumentList documents = SolrClientHelper.getInstance().searchDocuments(null, "_text_:" + tag);
            for (SolrDocument doc : documents) {
                UserFileVo userFile = userFileDao.getUserFileById((String) doc.getFieldValue("id"));
                userFiles.add(userFile);
                if (userFiles.size() > 1) {
                    sb.append(",");
                }
                sb.append("{\"guid\":\"")
                        .append(userFile.guid)
                        .append("\", \"meta\":")
                        .append(userFile.meta)
                        .append(", \"created\":")
                        .append(userFile.created)
                        .append(", \"highlight\":").append("\"").append(doc.getFieldValue("highlight").toString().replaceAll("[\\t\\n\\r]", " ")).append("\"")
                        .append("}");
            }
        } else {
            userFiles = userFileDao.getFiles(userVo.id, offset, limit, tag, sortField, sortDesc);
            if (userFiles.size() > 0) {
                sb.append("{\"guid\":\"")
                        .append(userFiles.get(0).guid)
                        .append("\", \"meta\":")
                        .append(userFiles.get(0).meta)
                        .append(", \"created\":")
                        .append(userFiles.get(0).created)
                        .append("}");
                for (int i = 1; i < userFiles.size(); i++) {
                    sb.append(",")
                            .append("{\"guid\":\"")
                            .append(userFiles.get(i).guid)
                            .append("\", \"meta\":")
                            .append(userFiles.get(i).meta)
                            .append(", \"created\":")
                            .append(userFiles.get(i).created)
                            .append("}");
                }
            }
        }
        sb.append("]");
        exchange.setResponseCode(StatusCodes.OK);
        exchange.getResponseSender().send(sb.toString());
    }
}
