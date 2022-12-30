package com.ims.helpers;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class FileUtilsTest {

    @Test
    public void testFileList() throws Exception {
        String tempFilePath = FileUtils.getTempDir();
        String filesFolder = tempFilePath + File.separator + "files" + File.separator + "win" + File.separator + "7";
        Assert.assertTrue("error create folder", FileUtils.createFolder(filesFolder));
        String[] fileNames = {"setup_2.0.4.exe", "setup_0.9.1.exe", "setup_1.0.10.exe", "setup_1.0.9.exe",
                "setup_1.1.15.exe", "setup_1.1.5.exe"};
        String data = "testData";
        for (String fn : fileNames) {
            FileUtils.writeToFile(data, filesFolder + File.separator + fn);
        }
        List<String> files = FileUtils.fileList(filesFolder);
        Assert.assertNotNull("list is null", files);
        Assert.assertFalse("size of list is 0", files.size() == 0);
        Assert.assertTrue("error count of files", files.size() == fileNames.length);
        FileUtils.delete(new File(tempFilePath + File.separator + "files"));
    }

}
