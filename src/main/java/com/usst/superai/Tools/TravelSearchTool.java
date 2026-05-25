package com.usst.superai.Tools;

import cn.hutool.core.util.URLUtil;
import com.usst.superai.model.TravelHotInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

public class TravelSearchTool {

    @Tool(description = "根据目的地和关键词搜索公开网页中的旅游热门信息，适合获取小红书、抖音、攻略网站中的公开旅游线索")
    public List<TravelHotInfo> searchTravelHotInfo(
            @ToolParam(description = "目的地，例如 上海、杭州、南京") String destination,
            @ToolParam(description = "旅游需求关键词，例如 citywalk、情侣、亲子、美食、拍照、周末") String keyword
    ) {
        List<TravelHotInfo> resultList = new ArrayList<>();

        resultList.addAll(searchByBaidu(destination + " " + keyword + " 小红书 旅游攻略"));
        resultList.addAll(searchByBaidu(destination + " " + keyword + " 抖音 旅行"));
        resultList.addAll(searchByBaidu(destination + " " + keyword + " 马蜂窝 攻略"));
        resultList.addAll(searchByBaidu(destination + " " + keyword + " 携程 攻略"));

        return resultList;
    }

    private List<TravelHotInfo> searchByBaidu(String query) {
        List<TravelHotInfo> list = new ArrayList<>();

        try {
            String url = "https://www.baidu.com/s?wd=" + URLUtil.encode(query);

            Document document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .get();

            for (Element element : document.select("div.result, div.c-container")) {
                String title = element.text();

                Element linkElement = element.selectFirst("a");
                String link = linkElement == null ? "" : linkElement.attr("href");

                if (title == null || title.length() < 5) {
                    continue;
                }

                TravelHotInfo info = new TravelHotInfo();
                info.setSource("搜索引擎公开结果");
                info.setTitle(title.length() > 80 ? title.substring(0, 80) : title);
                info.setSummary(title);
                info.setUrl(link);
                info.setHeat("搜索结果相关内容");

                list.add(info);

                if (list.size() >= 5) {
                    break;
                }
            }
        } catch (Exception e) {
            TravelHotInfo errorInfo = new TravelHotInfo();
            errorInfo.setSource("搜索引擎公开结果");
            errorInfo.setTitle("搜索失败");
            errorInfo.setSummary(e.getMessage());
            errorInfo.setUrl("");
            errorInfo.setHeat("");
            list.add(errorInfo);
        }

        return list;
    }
}