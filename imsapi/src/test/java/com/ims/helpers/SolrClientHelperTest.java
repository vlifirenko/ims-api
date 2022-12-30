package com.ims;

import com.ims.Settings;
import com.ims.helpers.FileUtils;
import com.ims.helpers.SolrClientHelper;
import com.ims.vos.FileVo;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class SolrClientHelperTest {

    private final Logger LOGGER = LoggerFactory.getLogger(ImsApiServerFilesTest.class);

    private static long SOFT_COMMIT_DELAY = 1000L;

    @Before
    public void before() throws Exception {
        String workingDir = System.getProperty("user.dir");
        Settings.getInstance().setSettingsPath(workingDir + File.separator + "ims.properties");
    }

    @After
    public void after() throws Exception {
        // TODO delete everything from Solr!
    }

    @Test
    public void solrInsertTest() throws Exception {
        String meta = "{\"tags\" : [\"tag1\"], \"title\" : \"ttl\", \"extension\" : \"txt\"}";
        String mongoId = "solrInsertTest";
        String testMsg = "test_message solrInsertTest";
        String userId = "user_id";
        String tempDir = FileUtils.getTempDir();
        File file = new File(tempDir + File.separator + "test1.txt");
        FileUtils.writeToFile(testMsg, file.toString());
        FileVo fv = new FileVo("hash", file.toString());
        fv.id = mongoId;
        Assert.assertTrue("Solr insert document failed", SolrClientHelper.getInstance().insertDocument(meta, mongoId, fv, userId));
        Thread.sleep(SOFT_COMMIT_DELAY * 2);
        SolrDocumentList documents = SolrClientHelper.getInstance().searchDocuments(null, "id:" + mongoId);
        Assert.assertTrue("documents size is 0", documents.getNumFound() > 0);
        // for (SolrDocument doc : documents) {
        //     SolrClientHelper.getInstance().deleteDocument((String) doc.getFieldValue("id"));
        // }
        Assert.assertTrue("File delete error", file.delete());
    }

    @Test
    public void solrUpdateTest() throws Exception {
        String meta = "{\"tags\" : [\"tag1\"], \"title\" : \"ttl\", \"extension\" : \"doc\"}";
        String mongoId = "solrUpdateTest";
        String testMsg = "test_message solrUpdateTest";
        String userId = "user_id";
        String tempDir = FileUtils.getTempDir();
        File file = new File(tempDir + File.separator + "test.txt");
        FileUtils.writeToFile(testMsg, file.toString());
        FileVo fv = new FileVo("hash", file.toString());
        SolrClientHelper.getInstance().insertDocument(meta, mongoId, fv, userId);
        String updatedMeta = "{\"tags\" : [\"tag22\"], \"title\" : \"new_ttl\", \"extension\" : \"docx\"}";
        SolrClientHelper.getInstance().updateDocument(updatedMeta, mongoId, fv, userId);
        Thread.sleep(SOFT_COMMIT_DELAY * 2);
        Map<String, String> params = new HashMap<String, String>();
        params.put(SolrClientHelper.PARAM_MONGO_ID, mongoId);
        params.put("meta", updatedMeta);
        SolrDocumentList documents = SolrClientHelper.getInstance().searchDocuments(params, "*:*");
        Assert.assertTrue("Error update", documents.getNumFound() > 0);
        // for (SolrDocument d : documents) {
        //     SolrClientHelper.getInstance().deleteDocument((String) d.getFieldValue("id"));
        // }
        Assert.assertTrue("File delete error", file.delete());
    }

    @Test
    public void solrDeleteTest() throws Exception {
        String meta = "{\"tags\" : [\"tag1\"], \"title\" : \"ttl\", \"extension\" : \"doc\"}";
        String mongoId = "solrDeleteTest";
        String testMsg = "test_message solrDeleteTest";
        String userId = "user_id";
        String tempDir = FileUtils.getTempDir();
        File file = new File(tempDir + File.separator + "test.txt");
        FileUtils.writeToFile(testMsg, file.toString());
        FileVo fv = new FileVo("hash", file.toString());
        SolrClientHelper.getInstance().insertDocument(meta, mongoId, fv, userId);
        Thread.sleep(SOFT_COMMIT_DELAY * 2);
        Map<String, String> params = new HashMap<String, String>();
        params.put(SolrClientHelper.PARAM_MONGO_ID, mongoId);
        SolrDocumentList documents = SolrClientHelper.getInstance().searchDocuments(params, "*:*");
        Assert.assertTrue("documents size is 0", documents.getNumFound() > 0);
        // for (SolrDocument doc : documents) {
        //     Assert.assertTrue("delete error", SolrClientHelper.getInstance().deleteDocument((String) doc.getFieldValue("id")));
        // }
        documents = SolrClientHelper.getInstance().searchDocuments(params, "*:*");
        Assert.assertTrue("documents size is not 0", documents.getNumFound() == 0);
    }

    @Test
    public void solrSearchTest() throws Exception {
        String meta = "{\"tags\" : [\"tag1\"], \"title\" : \"ttl\", \"extension\" : \"txt\"}";
        String mongoId = "solrSearchTest";
        String testMsg = "test_message solrSearchTest";
        String userId = "user_id";
        String tempDir = FileUtils.getTempDir();
        File file = new File(tempDir + File.separator + "test1.txt");
        FileUtils.writeToFile(testMsg, file.toString());
        FileVo fv = new FileVo("hash", file.toString());
        fv.id = mongoId;
        Assert.assertTrue("Solr insert document failed", SolrClientHelper.getInstance().insertDocument(meta, mongoId, fv, userId));
        Thread.sleep(SOFT_COMMIT_DELAY * 2);
        SolrDocumentList documents = SolrClientHelper.getInstance().searchDocuments(null, "_text_:" + testMsg);
        Assert.assertTrue("documents size is 0", documents.getNumFound() > 0);
        // for (SolrDocument doc : documents) {
        //     SolrClientHelper.getInstance().deleteDocument((String) doc.getFieldValue("id"));
        // }
        Assert.assertTrue("File delete error", file.delete());
    }

    @Test
    public void solrSearchByName() throws Exception {
        String testName = "test_name";
        String meta = "{\"tags\" : [\"tag11\"], \"title\" : \"" + testName + "\", \"extension\" : \"txt\"}";
        String mongoId = "solrSearchByName";
        String testMsg = "test_message solrSearchByName";
        String userId = "user_id";
        String tempDir = FileUtils.getTempDir();
        File file = new File(tempDir + File.separator + "test1.txt");
        FileUtils.writeToFile(testMsg, file.toString());
        FileVo fv = new FileVo("hash", file.toString());
        fv.id = mongoId;
        Assert.assertTrue("Solr insert document failed", SolrClientHelper.getInstance().insertDocument(meta, mongoId, fv, userId));
        Thread.sleep(SOFT_COMMIT_DELAY * 2);
        SolrDocumentList documents = SolrClientHelper.getInstance().searchDocuments(null, "title:" + testName);
        Assert.assertTrue("documents size is 0", documents.getNumFound() > 0);
        // for (SolrDocument doc : documents) {
        //     SolrClientHelper.getInstance().deleteDocument((String) doc.getFieldValue("id"));
        // }
        Assert.assertTrue("File delete error", file.delete());
    }

    @Test
    public void solrSearchByTag() throws Exception {
        String testTag = "test_tag";
        String meta = "{\"tags\" : [\"" + testTag + "\", \"test_tag1\"], \"title\" : \"ttl\", \"extension\" : \"txt\"}";
        String mongoId = "solrSearchByTag";
        String testMsg = "test_message solrSearchByTag";
        String userId = "user_id";
        String tempDir = FileUtils.getTempDir();
        File file = new File(tempDir + File.separator + "test1.txt");
        FileUtils.writeToFile(testMsg, file.toString());
        FileVo fv = new FileVo("hash", file.toString());
        fv.id = mongoId;
        Assert.assertTrue("Solr insert document failed", SolrClientHelper.getInstance().insertDocument(meta, mongoId, fv, userId));
        Thread.sleep(SOFT_COMMIT_DELAY * 2);
        Map<String, String> params = new HashMap<String, String>();
        SolrDocumentList documents = SolrClientHelper.getInstance().searchDocuments(null, "tags:" + testTag);
        Assert.assertTrue("documents size is 0", documents.getNumFound() > 0);
        // for (SolrDocument doc : documents) {
        //     SolrClientHelper.getInstance().deleteDocument((String) doc.getFieldValue("id"));
        // }
        Assert.assertTrue("File delete error", file.delete());
    }
}
