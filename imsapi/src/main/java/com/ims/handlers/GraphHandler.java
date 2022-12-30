package com.ims.handlers;

import com.google.gson.Gson;
import com.ims.daos.GraphDao;
import com.ims.daos.impl.GraphDaoMongo;
import com.ims.helpers.GephiWriter;
import com.ims.vos.GraphVo;
import com.ims.vos.UserVo;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Deque;
import java.util.Map;

public class GraphHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(GraphHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        UserVo userVo = exchange.getAttachment(UserVo.USER_VO);
        Map<String, Deque<String>> parameters = exchange.getQueryParameters();
        String format = null;
        if (parameters.get("format") != null) {
            format = parameters.get("format").getFirst();
        }
        if (format == null) {
            GraphDao graphDao = new GraphDaoMongo();
            GraphVo graph = graphDao.getGraph(userVo.id);
            Gson gson = new Gson();
            exchange.setResponseCode(StatusCodes.OK);
            exchange.getResponseSender().send(gson.toJson(graph));
            return;
        }
        if (format.equals("gexf")) {
            File graphFile;
            try {
                if (!GephiWriter.gexfFileExists(userVo)) {
                    GraphDao graphDao = new GraphDaoMongo();
                    GraphVo graphVo = graphDao.getGraph(userVo.id);
                    graphFile = GephiWriter.generateGexfFile(userVo, graphVo);
                } else {
                    String tempDir = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator");
                    graphFile = new File(tempDir + userVo.email + ".gexf");
                }
            } catch (IOException e) {
                e.printStackTrace();
                exchange.setResponseCode(StatusCodes.INTERNAL_SERVER_ERROR);
                return;
            }
            exchange.setResponseCode(StatusCodes.OK);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/octet-stream");
            exchange.getResponseHeaders().put(Headers.CONTENT_TRANSFER_ENCODING, "binary");
            exchange.setResponseContentLength(graphFile.length());
            exchange.getResponseHeaders().put(Headers.CONTENT_DISPOSITION, "attachment; filename=\"" + graphFile.getName() + "\"");
            exchange.startBlocking();
            OutputStream os = exchange.getOutputStream();
            byte[] bufferData = new byte[1024];
            int read;
            InputStream fis = new FileInputStream(graphFile);
            while ((read = fis.read(bufferData)) != -1) {
                os.write(bufferData, 0, read);
            }
            os.close();
            fis.close();
            return;
        }
        exchange.setResponseCode(StatusCodes.BAD_REQUEST);
        exchange.getResponseSender().send("{\"error\": \"Параметр format указан не верно\"}");
    }
}
