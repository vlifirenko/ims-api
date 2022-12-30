package com.ims.daos.impl;

import com.ims.daos.GraphDao;
import com.ims.helpers.DBPool;
import com.ims.vos.GraphVo;
import com.ims.vos.LinkVo;
import com.ims.vos.NodeVo;
import com.mongodb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.*;

public class GraphDaoMongo implements GraphDao {
    private final Logger LOGGER = LoggerFactory.getLogger(GraphDaoMongo.class);

    public static final String FIELD_TAGS = "tags";
    private DBCollection userFilesColl;

    public GraphDaoMongo() throws UnknownHostException {
        DB db = DBPool.getDb();
        userFilesColl = db.getCollection(UserFileDaoMongo.COLLECTION);
        userFilesColl.ensureIndex(UserFileDaoMongo.FIELD_FILE_HASH);
        userFilesColl.ensureIndex(UserFileDaoMongo.FIELD_UID);
    }

    @Override
    public GraphVo getGraph(String uid) {
        DBCursor cursor;
        try {
            cursor = userFilesColl.find(new BasicDBObject(UserFileDaoMongo.FIELD_UID, uid));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
        GraphVo graph = new GraphVo();
        while (cursor.hasNext()) {
            DBObject meta = (DBObject) cursor.next().get(UserFileDaoMongo.FIELD_META);
            BasicDBList dbList = (BasicDBList) meta.get(FIELD_TAGS);
            List<String> tags = new ArrayList<String>();
            for (Object tag : dbList) {
                tags.add(tag.toString());
            }
            generateNode(tags, graph);
        }
        if (graph.nodes.size() > 0) {
            float maxNodesCoefficient = 0f;
            for (NodeVo nodeVo : graph.nodes) {
                if (nodeVo.coefficient > maxNodesCoefficient)
                    maxNodesCoefficient = nodeVo.coefficient;
            }
            for (int i = 0; i < graph.nodes.size(); i++) {
                graph.nodes.get(i).coefficient /= maxNodesCoefficient;
            }
        }
        if (graph.links.size() > 0) {
            float maxLinksCoefficient = 0f;
            for (LinkVo linkVo : graph.links) {
                if (linkVo.coefficient > maxLinksCoefficient)
                    maxLinksCoefficient = linkVo.coefficient;
            }
            for (int i = 0; i < graph.links.size(); i++) {
                graph.links.get(i).coefficient /= maxLinksCoefficient;
            }
        }
        return graph;
    }

    public void generateNode(List<String> tags, GraphVo graph) {
        for (String tag : tags) {
            // generate nodes
            NodeVo node = new NodeVo();
            node.name = tag;
            node.color = generateColor();
            if (!graph.nodes.contains(node)) {
                graph.nodes.add(node);
            } else {
                int index = graph.nodes.indexOf(node);
                NodeVo nd = graph.nodes.get(index);
                nd.coefficient++;
                graph.nodes.set(index, nd);
            }
            // generate links
            for (String tag2 : tags) {
                if (tag.equals(tag2))
                    continue;
                LinkVo link = new LinkVo();
                link.node1 = tag2;
                link.node2 = tag;
                if (!graph.links.contains(link)) {
                    graph.links.add(link);
                } else {
                    int index = graph.links.indexOf(link);
                    LinkVo ln = graph.links.get(index);
                    ln.coefficient++;
                    graph.links.set(index, ln);
                }
            }
        }
    }

    public String generateColor() {
        Random rand = new Random();
        float h = rand.nextFloat();
        float s = 1;
        float l = 0.65f;
        Map<String, Integer> rgb = hslToRgb(h, s, l);
        return String.format("#%s%s%s", Integer.toHexString(rgb.get("r")), Integer.toHexString(rgb.get("g")),
                Integer.toHexString(rgb.get("b")));
    }

    private Map<String, Integer> hslToRgb(float h, float s, float l) {
        float r, g, b;
        if (s == 0) {
            r = g = b = (int) l;
        } else {
            float q = l < 0.5f ? l * (1f + s) : l + s - l * s;
            float p = 2 * l - q;
            r = hue2rgb(p, q, h + 1f / 3f);
            g = hue2rgb(p, q, h);
            b = hue2rgb(p, q, h - 1f / 3f);
        }
        Map<String, Integer> rgb = new HashMap<String, Integer>();
        rgb.put("r", Math.round(r * 255f));
        rgb.put("g", Math.round(g * 255f));
        rgb.put("b", Math.round(b * 255f));
        return rgb;
    }

    private float hue2rgb(float p, float q, float t) {
        if (t < 0f) t += 1f;
        if (t > 1f) t -= 1f;
        if (t < 1f / 6f) return p + (q - p) * 6f * t;
        if (t < 1f / 2f) return q;
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f;
        return p;
    }
}
