package com.ims.helpers;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class ThumbHelperTest {

    @Test
    public void generateThumbTest() throws Exception {
        String id = "test_id";
        String tempFilePath = FileUtils.getTempDir();
        int width = 200;
        int height = 100;
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        String url = tempFilePath + File.separator + "test_file.jpg";
        ImageIO.write(bi, "JPEG", new File(url));
        ThumbHelper.generateThumb(id, url);
    }
}
