package com.usst.superai.Exception;

import com.usst.superai.model.result.BaseResponse;
import com.usst.superai.model.result.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        return BaseResponse.error(
                e.getCode(),
                null,
                e.getMessage(),
                e.getDescription()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public BaseResponse<?> httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        return BaseResponse.error(
                ErrorCode.PARAMS_ERROR.getCode(),
                null,
                ErrorCode.PARAMS_ERROR.getMsg(),
                "请求体格式错误，请检查 JSON 格式"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public BaseResponse<?> illegalArgumentExceptionHandler(IllegalArgumentException e) {
        return BaseResponse.error(
                ErrorCode.PARAMS_ERROR.getCode(),
                null,
                ErrorCode.PARAMS_ERROR.getMsg(),
                e.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<?> exceptionHandler(Exception e) {
        log.error("系统异常", e);
        return BaseResponse.error(
                ErrorCode.SYSTEM_ERROR.getCode(),
                null,
                ErrorCode.SYSTEM_ERROR.getMsg(),
                "系统异常，请稍后重试"
        );
    }
}