package com.ims.handlers;

import com.ims.daos.UserDao;
import com.ims.daos.UserFileDao;
import com.ims.daos.impl.UserDaoMongo;
import com.ims.daos.impl.UserFileDaoMongo;
import com.ims.dtos.ShareInfoDto;
import com.ims.vos.UserFileVo;
import com.ims.vos.UserVo;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.StatusCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShareHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(ShareHandler.class);

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        UserVo userVo = exchange.getAttachment(UserVo.USER_VO);
        FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);
        ShareInfoDto shareInfo = new ShareInfoDto(formData);
        if (!shareInfo.isValid()) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            return;
        }
        List<String> emails = Arrays.asList(formData.getFirst("emails").getValue().split(","));
        List<String> guids = Arrays.asList(formData.getFirst("guids").getValue().split(","));
        // check emails
        UserDao userDao = new UserDaoMongo();
        List<UserVo> shareUsers = new ArrayList<UserVo>();
        for (String email : emails) {
            UserVo shareUser = userDao.getByEmail(email);
            if (shareUser != null)
                shareUsers.add(shareUser);
        }
        if (shareUsers.size() == 0) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("{\"message\": \"emails not valid\"}");
            return;
        }
        // check guids
        List<UserFileVo> shareGuids = new ArrayList<UserFileVo>();
        UserFileDao userFileDao = new UserFileDaoMongo();
        for (String guid : guids) {
            UserFileVo shareGuid = userFileDao.getUserFileByGuid(guid, userVo.id);
            if (shareGuid != null)
                shareGuids.add(shareGuid);
        }
        if (shareGuids.size() == 0) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("{\"message\": \"guids not valid\"}");
            return;
        }
        for (UserFileVo shareGuid : shareGuids) {
            for (UserVo shareUser : shareUsers) {
                UserFileVo userFileVo = new UserFileVo(shareUser.id, shareGuid.guid, shareGuid.meta, shareGuid.metaName,
                        shareGuid.fileName, shareGuid.fileHash, shareGuid.created);
                userFileDao.saveUserFile(userFileVo);
            }
        }

        exchange.setResponseCode(StatusCodes.OK);
    }
}
