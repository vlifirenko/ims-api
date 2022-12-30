package com.ims.helpers;

import com.ims.Settings;
import com.ims.vos.FileVo;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

public class SolrClientHelper {
    private final Logger LOGGER = LoggerFactory.getLogger(SolrClientHelper.class);

    public static final String PARAM_MONGO_ID = "id";

    private static SolrClientHelper instance;

    private SolrClient server;

    public static synchronized SolrClientHelper getInstance() {
        if (instance == null) {
            instance = new SolrClientHelper();
        }
        return instance;
    }

    public SolrClientHelper() {
        /*SSLSocketFactory sf = new SSLSocketFactory(new TrustSelfSignedStrategy());
        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        registry.register(new Scheme("https", 443, sf));
        PoolingClientConnectionManager cxMgr = new PoolingClientConnectionManager(registry);
        cxMgr.setMaxTotal(100);
        cxMgr.setDefaultMaxPerRoute(20);
        DefaultHttpClient client = new DefaultHttpClient(cxMgr);
        server = new LBHttpSolrServer(client, Settings.getInstance().getProperty(Settings.PROPERTY_SOLR_URL));*/

        server = new HttpSolrClient(Settings.getInstance().getProperty(Settings.PROPERTY_SOLR_URL));
    }

    public boolean insertDocument(String meta, String mongoId, FileVo fileVo, String userId) {
        JSONObject obj = new JSONObject(meta);
        ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");
        File f = new File(fileVo.path);
        try {
            String type = URLConnection.guessContentTypeFromName(f.getName());
            req.addFile(f, type);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        req.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);


        ModifiableSolrParams p = new ModifiableSolrParams();
        JSONArray tags = obj.getJSONArray("tags");
        for (int i = 0; i < tags.length(); i++) {
            p.add("literal.tags", tags.getString(i));
        }
        req.setParams(p);
        // req.setParam("literal.created", new Date());
        // if (!obj.isNull("extension")) {
        //     req.setParam("literal.extension", obj.getString("extension"));
        // }
        req.setParam("literal.title", obj.getString("title"));
        req.setParam("literal.id", mongoId);
        req.setParam("literal.user_id", userId);


        try {
            server.request(req);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean updateDocument(String meta, String mongoId, FileVo fileVo, String userId) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(SolrClientHelper.PARAM_MONGO_ID, mongoId);
        SolrDocumentList documents = searchDocuments(params, "*:*");
        for (SolrDocument doc : documents) {
            deleteDocument((String) doc.getFieldValue("id"));
        }
        insertDocument(meta, mongoId, fileVo, userId);
        return true;
    }

    public boolean deleteDocument(String id) {
        try {
            server.deleteById(id);
            server.commit();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public SolrDocumentList searchDocuments(Map<String, String> params, String query) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.setQuery(query);
        solrQuery.set("hl.simple.pre", "<em>");
        solrQuery.set("hl.simple.post", "</em>");
        solrQuery.set("hl.fl", "*");
        solrQuery.set("hl", true);
        if (params != null)
            for (Map.Entry<String, String> entry : params.entrySet()) {
                solrQuery.set(entry.getKey(), entry.getValue());
            }
        QueryResponse rsp;
        try {
            rsp = server.query(solrQuery);
            Iterator<SolrDocument> iter = rsp.getResults().iterator();
            while (iter.hasNext()) {
                SolrDocument resultDoc = iter.next();
                String id = (String) resultDoc.getFieldValue("id");
                if (rsp.getHighlighting().get(id) != null && rsp.getHighlighting().get(id).get("_text_") != null) {
                    resultDoc.setField("highlight", rsp.getHighlighting().get(id).get("_text_").get(0));
                }
            }
        } catch (SolrServerException e) {
            e.printStackTrace();
            return null;
        }
        return rsp.getResults();
    }
}
