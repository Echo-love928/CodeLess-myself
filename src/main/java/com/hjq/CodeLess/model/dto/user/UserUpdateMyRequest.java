package com.hjq.CodeLess.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户修改个人资料请求。
 * 用户 id 与角色由服务端登录态决定，禁止由客户端传入。
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    private String userName;

    private String userAvatar;

    private String userProfile;

    private static final long serialVersionUID = 1L;
}
