package com.ims.handlers;

import com.ims.daos.UserFileDao;
import com.ims.daos.impl.UserFileDaoMongo;
import com.ims.vos.StatisticVo;
import com.ims.vos.UserFileVo;
import com.ims.vos.UserVo;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StatisticsHandler implements HttpHandler {
    private final Logger LOGGER = LoggerFactory.getLogger(StatisticsHandler.class);

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        UserVo userVo = exchange.getAttachment(UserVo.USER_VO);
        UserFileDao userFileDao = new UserFileDaoMongo();
        Map<String, Deque<String>> parameters = exchange.getQueryParameters();
        String tag = null;
        if (parameters.get("tag") != null) {
            tag = parameters.get("tag").getFirst();
        }
        List<UserFileVo> userFiles = userFileDao.getFiles(userVo.id, null, null, tag, null, null);
        List<StatisticVo> statistics = new ArrayList<StatisticVo>();
        for (UserFileVo userFileVo : userFiles) {
            boolean skipItem = false;
            for (int i = 0; i < statistics.size(); i++) {
                if (DateUtils.isSameDay(statistics.get(i).timeStamp, new Date(userFileVo.created))) {
                    statistics.get(i).createdCount++;
                    skipItem = true;
                    break;
                }
            }
            if (!skipItem)
                statistics.add(new StatisticVo(new Date(userFileVo.created)));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (statistics.size() > 0) {
            sb.append("{\"date\":\"").append(statistics.get(0).timeStamp.getTime()).append("\", \"createdCount\":")
                    .append(statistics.get(0).createdCount).append("}");
            for (int i = 1; i < statistics.size(); i++) {
                sb.append(",").append("{\"date\":\"").append(statistics.get(i).timeStamp.getTime())
                        .append("\", \"createdCount\":").append(statistics.get(i).createdCount).append("}");
            }
        }
        sb.append("]");
        exchange.setResponseCode(StatusCodes.OK);
        exchange.getResponseSender().send(sb.toString());
    }
}
