package com.ims.vos;

import java.util.ArrayList;
import java.util.List;

public class VersionVo implements Comparable {

    public String version;

    public VersionVo(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return this.version;
    }

    @Override
    public boolean equals(Object obj) {
        return this.version.equals(((VersionVo) obj).version);
    }

    @Override
    public int compareTo(Object o) {
        String parts1[] = getVersionParts(this.version),
                parts2[] = getVersionParts(((VersionVo) o).version);
        for (int i = 0; i < Math.min(parts1.length, parts2.length); i++) {
            int partComparison = compareVersionPart(parts1[i], parts2[i]);
            if (partComparison != 0) {
                return partComparison;
            }
        }
        if (parts1.length > parts2.length) {
            return 1;
        } else if (parts1.length < parts2.length) {
            return -1;
        } else {
            return 0;
        }
    }

    protected String[] getVersionParts(String version) {
        return version.split("\\.");
    }

    protected int compareVersionPart(String part1, String part2) {
        int versionPart1 = Integer.parseInt(part1),
                versionPart2 = Integer.parseInt(part2);

        if (versionPart1 > versionPart2) {
            return 1;
        } else if (versionPart1 < versionPart2) {
            return -1;
        } else {
            return 0;
        }
    }

    public static List<VersionVo> getVersionsFromFiles(List<String> files) {
        if (files == null)
            return null;
        List<VersionVo> versions = new ArrayList<VersionVo>();
        for (String file : files) {
            versions.add(new VersionVo(file.substring(file.indexOf("_") + 1, file.lastIndexOf("."))));
        }
        return versions;
    }
}
