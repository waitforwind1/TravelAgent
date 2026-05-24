package com.usst.superai.Tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.messaging.simp.stomp.StompReactorNettyCodec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WeatherTools {

    @Tool(description = "这是一个获取当前天气的工具")
    public String getWeather(@ToolParam(description = "当前城市，例如：上海、北京、广州") String city) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String currentTime = LocalDateTime.now().format(formatter);

        return String.format("目前位于%s市，现在是%s，当前温度是29摄氏度", city, currentTime);
    }

}

