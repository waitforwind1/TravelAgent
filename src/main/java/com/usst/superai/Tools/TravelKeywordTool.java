package com.usst.superai.Tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

public class TravelKeywordTool {

    @Tool(description = "根据目的地和用户偏好生成适合搜索热门旅游内容的关键词")
    public List<String> generateTravelKeywords(
            @ToolParam(description = "目的地，例如 上海、杭州、苏州") String destination,
            @ToolParam(description = "用户偏好，例如 美食、拍照、情侣、亲子、citywalk、穷游") String preference
    ) {
        return List.of(
                destination + " " + preference + " 小红书攻略",
                destination + " " + preference + " 抖音热门",
                destination + " " + preference + " 周末去哪玩",
                destination + " " + preference + " citywalk",
                destination + " " + preference + " 美食打卡",
                destination + " " + preference + " 避坑攻略",
                destination + " " + preference + " 一日游路线",
                destination + " " + preference + " 两日游路线"
        );
    }
}