package com.ims.daos;

import com.ims.vos.UserVo;

import java.util.List;

public interface UserDao {

    public abstract String saveUser(UserVo user);

    public abstract List<UserVo> getUsers();

    public abstract UserVo getByEmail(String email);

    public abstract UserVo getByToken(String token);

    public abstract void deleteUser(UserVo user);
}
