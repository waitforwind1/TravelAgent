package com.usst.superai.Exception;

import com.usst.superai.model.result.ErrorCode;
import lombok.Data;

@Data
public class BusinessException extends RuntimeException{
    // code  msg
    private Integer code;
    private String description;

    public BusinessException(ErrorCode errorCode, String description){
        super(errorCode.getMsg());
        this.description = description;
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode){
        super(errorCode.getMsg());
        this.code = errorCode.getCode();
    }
}
