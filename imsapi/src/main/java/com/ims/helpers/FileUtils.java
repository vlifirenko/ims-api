package com.ims.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {
    private final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    public static String readFromFile(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line = null;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    public static void writeToFile(String data, String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.createNewFile();
        }
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(data);
        bw.close();
    }

    public static List<String> fileList(String folderName) {
        File folder = new File(folderName);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return null;
        List<String> result = new ArrayList<String>();
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                result.add(listOfFile.getName());
            }
        }
        return result;
    }

    public static boolean createFolder(String path) {
        File file = new File(path);
        return file.exists() || file.mkdirs();
    }

    public static void delete(File file) throws IOException {
        if (file.isDirectory()) {
            if (file.list().length == 0) {
                file.delete();
            } else {
                String files[] = file.list();
                for (String temp : files) {
                    File fileDelete = new File(file, temp);
                    delete(fileDelete);
                }
                if (file.list().length == 0) {
                    file.delete();
                }
            }
        } else {
            file.delete();
        }
    }

    public static String getTempDir() throws IOException {
        File temp = File.createTempFile("temp-file-name", ".tmp");
        String tempFilePath = temp.getAbsolutePath().
                substring(0, temp.getAbsolutePath().lastIndexOf(File.separator));
        temp.delete();
        return tempFilePath;
    }

    public static String getMD5HashFile(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
        fis.close();
        return md5;
    }

}
