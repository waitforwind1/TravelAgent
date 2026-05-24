package com.usst.superai.model.dto;

public record ChatContinueRequestVO(
        // todo:要不要加agent状态
        String conversationId,
        String completionMessage
) {
}
