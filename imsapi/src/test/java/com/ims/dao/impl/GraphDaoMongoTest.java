package com.ims.dao.impl;


import com.google.gson.Gson;
import com.ims.Settings;
import com.ims.daos.impl.GraphDaoMongo;
import com.ims.vos.GraphVo;
import com.mongodb.util.JSON;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraphDaoMongoTest {

    private GraphDaoMongo graphDao;

    @Before
    public void before() throws Exception {
        Settings.getInstance().setProperty(Settings.PROPERTY_MONGO_URI, "mongodb://localhost");
        graphDao = new GraphDaoMongo();
        Assert.assertNotNull("graphDao is null", graphDao);
    }

    @Test
    public void generateNodeTest() throws Exception {
        GraphVo graph = new GraphVo();
        List<String> file1 = new ArrayList<String>();
        file1.add("tag1");
        file1.add("tag2");
        file1.add("tag3");
        file1.add("tag4");
        List<String> file2 = new ArrayList<String>();
        file2.add("tag2");
        file2.add("tag4");
        file2.add("tag5");
        List<String> file3 = new ArrayList<String>();
        file3.add("tag1");
        file3.add("tag4");
        file3.add("tag5");
        List<String> file4 = new ArrayList<String>();
        file4.add("tag2");
        file4.add("tag3");
        file4.add("tag4");

        graphDao.generateNode(file1, graph);
        graphDao.generateNode(file2, graph);
        graphDao.generateNode(file3, graph);
        graphDao.generateNode(file4, graph);

        Assert.assertEquals("Error of count nodes", graph.nodes.size(), 5);
        Assert.assertEquals("Error of count links", graph.links.size(), 9);
        Assert.assertEquals("Node coefficient error", graph.nodes.get(0).coefficient, 1f, 0f);
        Assert.assertEquals("Node coefficient error", graph.nodes.get(3).coefficient, 3f, 0f);
        Assert.assertEquals("Link coefficient error", graph.links.get(0).coefficient, 1f, 0f);
        Assert.assertEquals("Link coefficient error", graph.links.get(4).coefficient, 5f, 0f);
    }
}
