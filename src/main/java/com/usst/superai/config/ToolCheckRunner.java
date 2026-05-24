package com.usst.superai.config;

import com.usst.superai.Tools.ToolProvidersRegistry;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 检测应用启动时的Tool工具有哪些
 */
//@Component
public class ToolCheckRunner implements CommandLineRunner {

    @Resource
    private ToolProvidersRegistry toolProvidersRegistry;

    @Override
    public void run(String... args) {
        ToolCallback[] toolCallbacks = toolProvidersRegistry.getAllTools();

        System.out.println("========== MCP 工具数量 ==========");
        System.out.println(toolCallbacks.length);

        System.out.println("========== MCP 工具列表 ==========");
        for (ToolCallback toolCallback : toolCallbacks) {
            System.out.println("工具名：" + toolCallback.getToolDefinition().name());
            System.out.println("描述：" + toolCallback.getToolDefinition().description());
            System.out.println("--------------------------------");
        }
    }
}