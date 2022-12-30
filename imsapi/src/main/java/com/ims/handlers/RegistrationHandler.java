package com.ims.handlers;

import com.ims.daos.UserDao;
import com.ims.daos.impl.UserDaoMongo;
import com.ims.dtos.RegistrationInfoDto;
import com.ims.helpers.MD5Helper;
import com.ims.vos.UserVo;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.SecureRandom;

public class RegistrationHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(RegistrationHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.getAttachment(FormDataParser.FORM_DATA) == null) {
            FormParserFactory formParserFactory = FormParserFactory.builder(true).build();
            FormDataParser parser = formParserFactory.createParser(exchange);
            parser.parse(this);
            return;
        }
        FormData formData = exchange.getAttachment(FormDataParser.FORM_DATA);
        RegistrationInfoDto registrationInfo = new RegistrationInfoDto(formData);
        if (!registrationInfo.isValid()) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("{\"error\": \"Поля должны быть заполнены\"}");
            return;
        }
        UserDao userDao = new UserDaoMongo();
        if (userDao.getByEmail(registrationInfo.getProperty("email")) != null) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("{\"error\": \"Email уже существует\"}");
            return;
        }
        String password = MD5Helper.getMD5(registrationInfo.getProperty("password"));
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        String randomNum = Integer.toString(prng.nextInt());
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] result = sha.digest(randomNum.getBytes());
        String token = MD5Helper.hexEncode(result);
        UserVo user = new UserVo(registrationInfo.getProperty("email"), password, token);
        if (userDao.saveUser(user) == null) {
            exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.setResponseCode(StatusCodes.OK);
        exchange.getResponseSender().send("{\"token\": \"" + token + "\"}");
    }
}
