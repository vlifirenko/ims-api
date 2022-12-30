package com.ims.vos;

import io.undertow.server.handlers.form.FormData;
import io.undertow.util.AttachmentKey;

public class UserVo {

    public static final AttachmentKey<UserVo> USER_VO = AttachmentKey.create(UserVo.class);

    public String id;
    public String email;
    public String password;
    public String token;

    public UserVo(String id, String email, String password, String token) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.token = token;
    }

    public UserVo(String email, String password, String token) {
        this.email = email;
        this.password = password;
        this.token = token;
    }
}
