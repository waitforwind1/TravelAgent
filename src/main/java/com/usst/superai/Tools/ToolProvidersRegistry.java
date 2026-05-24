package com.usst.superai.Tools;

import lombok.Getter;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
@Getter
@Component
public class ToolProvidersRegistry {
    ToolCallback[] allTools;

    public ToolProvidersRegistry(List<ToolCallbackProvider> toolCallbackProviders) {
        this.allTools = toolCallbackProviders.stream()
                .flatMap(provider-> Arrays.stream(provider.getToolCallbacks()))
                .toArray(ToolCallback[]::new);
    }

}
