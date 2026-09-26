package com.hjq.CodeLess.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 当前登录用户修改密码请求。
 */
@Data
public class UserChangePasswordRequest implements Serializable {

    private String currentPassword;

    private String newPassword;

    private static final long serialVersionUID = 1L;
}
