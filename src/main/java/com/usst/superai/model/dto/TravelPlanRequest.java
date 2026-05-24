package com.usst.superai.model.dto;

import cn.hutool.core.date.DateTime;
import lombok.Data;

@Data
public class TravelPlanRequest {
    private String message;
    private String conversationId;
    private String city;
    private String startLocation;
    private String destinationArea;
    private String dateTime;
    private String budget;
    private String people;
    private String preference;
}
