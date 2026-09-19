package com.hjq.CodeLess.model.dto.app;

import java.io.Serializable;

import lombok.Data;

@Data
public class AppUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    private static final long serialVersionUID = 1L;
}

