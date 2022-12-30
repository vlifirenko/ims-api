package com.ims.daos;

import com.ims.vos.UserFileVo;

import java.util.List;

public interface UserFileDao {

    public abstract String saveUserFile(UserFileVo userFile);

    public abstract void updateUserFile(UserFileVo userFile);

    public abstract List<UserFileVo> getUserFiles();

    public abstract List<UserFileVo> getUserFilesByHash(String hash);

    public abstract List<UserFileVo> getFiles(String uid, String offset, String limit, String tag, String sortField, String sortDesc);

    public abstract void deleteUserFile(UserFileVo userFile);

    public abstract UserFileVo getUserFileByUid(String uid);

    public abstract UserFileVo getUserFileByGuid(String guid, String uid);

    public abstract UserFileVo getUserFileByHash(String hash);

    public abstract UserFileVo getUserFileById(String id);
}
