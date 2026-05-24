package com.usst.superai.model.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode{
    SUCCESS(200,"OK"),
    PARAMS_ERROR(400,"参数错误"),
    NOT_LOGIN(401,"未登录"),
    NO_AUTH(403,"无权限"),
    DATA_NOT_EXIST(404,"数据不存在"),
    CONFLICT_ERROR(409,"数据冲突"),
    TOO_MANY_REQUESTS(429,"请求过于频繁"),
    SYSTEM_ERROR(500,"系统内部错误"),
    OPERATION_ERROR(501,"操作失败");

    private Integer code;
    private String msg;
}
