package com.usst.superai.model.dto;

public record TravelExportResponse(
    String conversationId,
    String content,
    String filePath
) {
}
