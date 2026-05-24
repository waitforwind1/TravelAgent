package com.usst.superai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TravelHotInfo {

    /**
     * 来源平台：小红书、抖音、百度、携程、马蜂窝等
     */
    private String source;

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 原始链接
     */
    private String url;

    /**
     * 热度信息，例如点赞数、评论数、发布时间等
     */
    private String heat;
}