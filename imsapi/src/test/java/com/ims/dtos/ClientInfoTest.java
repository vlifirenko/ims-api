package com.ims.dtos;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class ClientInfoTest {

    @Test
    public void testClientInfo() throws Exception {
        Map<String, Deque<String>> parameters = new HashMap<String, Deque<String>>();
        ArrayDeque<String> value = new ArrayDeque<String>();
        value.add("value");
        parameters.put("platform", value);
        parameters.put("platformVersion", value);
        parameters.put("appVersion", value);
        ClientInfoDto clientInfo = new ClientInfoDto(parameters);
        Assert.assertTrue("client info is not valid", clientInfo.isValid());
    }

    @Test
    public void testClientInfoFailed() throws Exception {
        Map<String, Deque<String>> parameters = new HashMap<String, Deque<String>>();
        ArrayDeque<String> value = new ArrayDeque<String>();
        value.add("value");
        parameters.put("platform", value);
        parameters.put("platformVersion", value);
        ClientInfoDto clientInfo = new ClientInfoDto(parameters);
        Assert.assertFalse("client invalid parameters is failed", clientInfo.isValid());
    }

}
