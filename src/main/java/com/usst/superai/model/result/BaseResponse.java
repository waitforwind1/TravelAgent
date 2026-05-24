package com.usst.superai.model.result;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BaseResponse<T> {
    private T data;
    private Integer code;
    private String msg;
    private String description;

    public static <T> BaseResponse<T> error(int code, T data,String msg ,String description){
        return new BaseResponse<>(data, code, msg, description);
    }

    public static <T> BaseResponse<T> error(int code,T data,String msg){
        return new BaseResponse<>(data, code,msg,"");
    }

    public static <T> BaseResponse<T> success(T data, String description){
        return new BaseResponse<>(data,200, "ok", description);
    }

    public static <T> BaseResponse<T> success(T data){
        return new BaseResponse<>(data,200,"ok","");
    }
}
