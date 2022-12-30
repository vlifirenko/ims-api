package com.ims.daos;

import com.ims.vos.FileVo;

import java.util.List;

public interface FileDao {

    public abstract String saveFile(FileVo file);

    public abstract void updateFile(FileVo file);

    public abstract FileVo getByHash(String hash);

    public abstract void deleteFile(FileVo file);
}
