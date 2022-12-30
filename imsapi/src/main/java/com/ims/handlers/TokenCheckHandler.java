package com.ims.handlers;

import com.ims.Settings;
import com.ims.daos.UserDao;
import com.ims.daos.impl.UserDaoMongo;
import com.ims.vos.UserVo;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormEncodedDataDefinition;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.MultiPartParserDefinition;
import io.undertow.util.AttachmentKey;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Map;

public class TokenCheckHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(TokenCheckHandler.class);

    private HttpHandler handler;

    public TokenCheckHandler(HttpHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String token = null;
        String httpMethod = exchange.getRequestMethod().toString();
        if (httpMethod.equals("GET") || httpMethod.equals("DELETE")) {
            // GET
            Map<String, Deque<String>> parameters = exchange.getQueryParameters();
            if (parameters.get("token") == null) {
                exchange.setResponseCode(StatusCodes.UNAUTHORIZED);
                return;
            }
            token = parameters.get("token").getFirst();
        } else if (httpMethod.equals("POST") ||
                httpMethod.equals("PUT")) {
            // POST PUT DELETE
            if (exchange.getAttachment(FormDataParser.FORM_DATA) == null) {
                String mimeType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
                if (mimeType.startsWith(MultiPartParserDefinition.MULTIPART_FORM_DATA))
                    exchange.startBlocking();
                
                MultiPartParserDefinition multiPartParserDefinition = new MultiPartParserDefinition();
                multiPartParserDefinition.setMaxIndividualFileSize(Long.parseLong(Settings.getInstance().getProperty(Settings.PROPERTY_MAX_POST_SIZE)));
                multiPartParserDefinition.setDefaultEncoding("UTF-8");
                
                FormParserFactory formParserFactory = FormParserFactory.builder(false).addParsers(
                    new FormEncodedDataDefinition().setDefaultEncoding("UTF-8"),
                    multiPartParserDefinition
                ).build();
                FormDataParser parser = formParserFactory.createParser(exchange);
                parser.parse(this);
            } else {
                if (exchange.getAttachment(FormDataParser.FORM_DATA).getFirst("token") != null) {
                    token = exchange.getAttachment(FormDataParser.FORM_DATA).getFirst("token").getValue();
                } else {
                    exchange.setResponseCode(StatusCodes.UNAUTHORIZED);
                    return;
                }
            }
        }
        UserDao userDao = new UserDaoMongo();
        UserVo userVo = userDao.getByToken(token);
        if (userVo == null) {
            exchange.setResponseCode(StatusCodes.UNAUTHORIZED);
            return;
        }
        exchange.putAttachment(UserVo.USER_VO, userVo);
        handler.handleRequest(exchange);
    }
}
