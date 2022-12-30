package com.ims.helpers;

import com.ims.vos.InstallFileVo;

import java.util.ArrayList;
import java.util.List;

public class DebugHelper {

    public static List<InstallFileVo> testFileList() {
        List<InstallFileVo> installFileVos = new ArrayList<InstallFileVo>();
        installFileVos.add(new InstallFileVo("win/7/0.9.1.txt", "win", "7", "0.9.1", "guid1"));
        installFileVos.add(new InstallFileVo("win/7/1.0.txt", "win", "7", "1.0", "guid2"));
        installFileVos.add(new InstallFileVo("win/7/1.0.1.txt", "win", "7", "1.0.1", "guid3"));
        installFileVos.add(new InstallFileVo("macos/1/1.9.1.txt", "macos", "1", "1.9.1", "guid4"));
        installFileVos.add(new InstallFileVo("macos/1/1.0.txt", "macos", "1", "1.0", "guid5"));
        installFileVos.add(new InstallFileVo("macos/1/2.0.1.txt", "macos", "1", "2.0.1", "guid6"));
        return installFileVos;
    }

}
