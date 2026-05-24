package com.usst.superai.Tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;

public class WebTools {

    private static final int TIMEOUT_MS = 100000;
    private static final int MAX_TEXT_LENGTH = 3000;

    @Tool(description = "读取网页正文文本，适合获取网页中的旅游攻略、地点介绍、注意事项等公开信息")
    public String strachWeb(@ToolParam(description = "需要读取的网页 URL") String url) {
        if (!StringUtils.hasText(url)) {
            return "网页读取失败：url 不能为空";
        }

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(TIMEOUT_MS)
                    .get();

            doc.select("script, style, noscript, iframe, svg").remove();

            String title = doc.title();
            String text = doc.body() == null ? "" : doc.body().text();
            text = text.replaceAll("\\s+", " ").trim();

            if (text.length() > MAX_TEXT_LENGTH) {
                text = text.substring(0, MAX_TEXT_LENGTH) + "...";
            }

            return "标题：" + title + "\n正文：" + text;
        } catch (Exception e) {
            return "网页读取失败：" + e.getClass().getSimpleName() + "，原因：" + e.getMessage();
        }
    }
}