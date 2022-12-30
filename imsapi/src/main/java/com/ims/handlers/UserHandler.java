package com.ims.handlers;

import com.ims.daos.FileDao;
import com.ims.daos.UserDao;
import com.ims.daos.UserFileDao;
import com.ims.daos.impl.FileDaoMongo;
import com.ims.daos.impl.UserDaoMongo;
import com.ims.daos.impl.UserFileDaoMongo;
import com.ims.vos.FileVo;
import com.ims.vos.UserFileVo;
import com.ims.vos.UserVo;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class UserHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(UserHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        UserVo userVo = exchange.getAttachment(UserVo.USER_VO);
        String httpMethod = exchange.getRequestMethod().toString();
        if (httpMethod.equals("DELETE")) {
            UserDao userDao = new UserDaoMongo();
            UserFileDao userFileDao = new UserFileDaoMongo();
            FileDao fileDao = new FileDaoMongo();
            List<UserFileVo> userFiles = userFileDao.getFiles(userVo.id, null, null, null, null, null);
            for (UserFileVo userFileVo : userFiles) {
                if (userFileDao.getUserFilesByHash(userFileVo.fileHash).size() <= 1) {
                    FileVo fileVo = fileDao.getByHash(userFileVo.fileHash);
                    File file = new File(fileVo.path);
                    file.delete();
                    fileDao.deleteFile(fileVo);
                }
                userFileDao.deleteUserFile(userFileVo);
            }
            userDao.deleteUser(userVo);
            exchange.setResponseCode(StatusCodes.OK);
        } else if (httpMethod.equals("GET")) {
            exchange.getResponseSender().send("{\"email\": \"" + userVo.email + "\"}");
            exchange.setResponseCode(StatusCodes.OK);
        }
    }
}
