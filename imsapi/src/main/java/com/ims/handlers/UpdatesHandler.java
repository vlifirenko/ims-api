package com.ims.handlers;

import com.ims.Settings;
import com.ims.dtos.ClientInfoDto;
import com.ims.helpers.FileUtils;
import com.ims.vos.VersionVo;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class UpdatesHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(UpdatesHandler.class);

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        ClientInfoDto clientInfo = new ClientInfoDto(exchange.getQueryParameters());
        if (!clientInfo.isValid()) {
            exchange.setResponseCode(StatusCodes.BAD_REQUEST);
            return;
        }
        String settingsPath = Settings.getInstance().getProperty(Settings.PROPERTY_UPDATES_PATH);
        String settingsUrl = Settings.getInstance().getProperty(Settings.PROPERTY_UPDATES_URL);
        if (settingsPath == null || settingsUrl == null) {
            exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);
            return;
        }
        String path = settingsPath + clientInfo.getProperty("platform")
                + File.separator + clientInfo.getProperty("platformVersion");
        List<String> files = FileUtils.fileList(path);
        if (files == null) {
            exchange.setResponseCode(StatusCodes.NOT_FOUND);
            return;
        }
        List<VersionVo> versions = VersionVo.getVersionsFromFiles(files);
        Collections.sort(versions);
        VersionVo lastVersion = versions.get(versions.size() - 1);
        VersionVo appVersion = new VersionVo(clientInfo.getProperty("appVersion"));
        if (appVersion.equals(lastVersion) || appVersion.compareTo(lastVersion) == 1) {
            exchange.setResponseCode(StatusCodes.NOT_FOUND);
            return;
        }
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.setResponseCode(StatusCodes.OK);
        String result = "{\"url\": \"" + settingsUrl + clientInfo.getProperty("platform") +
                "/" + clientInfo.getProperty("platformVersion") + "/" + "setup_" + lastVersion + ".exe\"}";
        exchange.getResponseSender().send(result);
    }
}
