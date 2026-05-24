package com.usst.superai.Exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class AgentException extends RuntimeException{

    public AgentException(String msg) {
        super(msg);
    }

}
