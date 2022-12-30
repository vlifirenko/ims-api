package com.ims.helpers;

import com.ims.Settings;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ThumbHelper {

    public static String getThumb(String id) {
        return null;
    }

    public static void generateThumb(String id, String url) {
        try {
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            img.createGraphics().drawImage(ImageIO.read(new File(url)).getScaledInstance(100, 100, Image.SCALE_SMOOTH), 0, 0, null);
            String dest = Settings.getInstance().getProperty(Settings.PROPERTY_THUMB_PATH) + File.separator + id + ".jpg";
            ImageIO.write(img, "jpg", new File(dest));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteThumb(String id) {
        String dest = Settings.getInstance().getProperty(Settings.PROPERTY_THUMB_PATH) + File.separator + id + ".jpg";
        File file = new File(dest);
        file.delete();
    }
}
