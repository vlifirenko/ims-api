package com.ims.handlers;

import com.ims.daos.UserDao;
import com.ims.daos.impl.UserDaoMongo;
import com.ims.dtos.AuthInfoDto;
import com.ims.helpers.MD5Helper;
import com.ims.vos.UserVo;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(AuthHandler.class);

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        AuthInfoDto authInfo = new AuthInfoDto(exchange.getQueryParameters());
        if (!authInfo.isValid()) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            return;
        }
        UserDao userDao = new UserDaoMongo();
        UserVo user = userDao.getByEmail(authInfo.getProperty("email"));
        if (user != null && MD5Helper.getMD5(authInfo.getProperty("password")).equals(user.password)) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
            exchange.setResponseCode(StatusCodes.OK);
            exchange.getResponseSender().send("{\"token\": \"" + user.token + "\"}");
            return;
        }
        exchange.setResponseCode(StatusCodes.BAD_REQUEST);
        return;
    }
}
