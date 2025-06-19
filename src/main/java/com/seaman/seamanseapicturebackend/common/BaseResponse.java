package com.seaman.seamanseapicturebackend.common;

import com.seaman.seamanseapicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 基本返回类
 *
 * @param <T> 返回数据类型
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}

