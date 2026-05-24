package com.usst.superai.Tools;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistry {

    @Value("${app.filedir}")
    private String fileDir;

    @Value("${app.downloadPrefix}")
    private String downloadPrefix;
    @Bean
    public ToolCallbackProvider toolCallbackProvider() {
        TravelKeywordTool travelKeywordTool = new TravelKeywordTool();
        TravelSearchTool travelSearchTool =new TravelSearchTool();
        WeatherTools weatherTools = new WeatherTools();
        WebTools webTools = new WebTools();
        FileTools fileTools = new FileTools(fileDir,downloadPrefix);
        TerminateTools terminateTools = new TerminateTools();
        return MethodToolCallbackProvider.builder()
                .toolObjects(travelKeywordTool,travelSearchTool,weatherTools,webTools,fileTools,terminateTools)
                .build();
    }
}