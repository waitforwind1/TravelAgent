# Travel Controller and ServiceImpl Reference

下面是旅行规划第一版可用实现，供你手写参考。

## TravelController.java

```java
package com.usst.superai.controller;

import com.usst.superai.model.dto.TravelExportResponse;
import com.usst.superai.model.dto.TravelPlanRequest;
import com.usst.superai.model.dto.TravelPlanResponse;
import com.usst.superai.service.TravelPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/travel")
@Tag(name = "AI旅行规划")
public class TravelController {

    @Resource
    private TravelPlanService travelPlanService;

    @PostMapping("/plan")
    @Operation(summary = "生成旅行规划")
    public TravelPlanResponse generatePlan(@RequestBody TravelPlanRequest request) {
        return travelPlanService.generatePlan(request);
    }

    @PostMapping("/chat")
    @Operation(summary = "基于会话继续修改旅行规划")
    public TravelPlanResponse chat(@RequestBody TravelPlanRequest request) {
        return travelPlanService.chat(request);
    }

    @PostMapping(value = "/plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成旅行规划")
    public Flux<String> streamPlan(@RequestBody TravelPlanRequest request) {
        return travelPlanService.streamPlan(request);
    }

    @PostMapping("/plan/export")
    @Operation(summary = "生成旅行规划并导出Markdown")
    public TravelExportResponse generateAndExport(@RequestBody TravelPlanRequest request) {
        return travelPlanService.generateAndExport(request);
    }
}
```

## TravelPlanServiceImpl.java

```java
package com.usst.superai.service.Impl;

import com.usst.superai.Tools.ToolProvidersRegistry;
import com.usst.superai.constant.TravelPrompt;
import com.usst.superai.model.dto.TravelExportResponse;
import com.usst.superai.model.dto.TravelPlanRequest;
import com.usst.superai.model.dto.TravelPlanResponse;
import com.usst.superai.service.TravelPlanService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class TravelPlanServiceImpl implements TravelPlanService {

    private static final Path EXPORT_DIR = Path.of("src", "main", "resources", "tmp", "travel-plans");

    @Resource
    @Qualifier("travelChatClient")
    private ChatClient chatClient;

    @Resource
    private ToolProvidersRegistry toolProvidersRegistry;

    @Override
    public TravelPlanResponse generatePlan(TravelPlanRequest request) {
        String conversationId = resolveConversationId(request);
        String content = callModel(request, conversationId);
        return new TravelPlanResponse(conversationId, content);
    }

    @Override
    public TravelPlanResponse chat(TravelPlanRequest request) {
        String conversationId = resolveConversationId(request);
        String content = chatClient.prompt()
                .system(TravelPrompt.SYSTEM_PROMPT)
                .user(resolveMessage(request))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolCallbacks(toolProvidersRegistry.getAllTools())
                .call()
                .content();
        return new TravelPlanResponse(conversationId, content);
    }

    @Override
    public Flux<String> streamPlan(TravelPlanRequest request) {
        String conversationId = resolveConversationId(request);
        Flux<String> contentStream = chatClient.prompt()
                .system(TravelPrompt.SYSTEM_PROMPT)
                .user(buildPlanPrompt(request))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolCallbacks(toolProvidersRegistry.getAllTools())
                .stream()
                .content();
        return Flux.concat(Flux.just("conversationId: " + conversationId + "\n\n"), contentStream);
    }

    @Override
    public TravelExportResponse generateAndExport(TravelPlanRequest request) {
        TravelPlanResponse response = generatePlan(request);
        String filePath = writeMarkdown(response.content());
        return new TravelExportResponse(response.conversationId(), response.content(), filePath);
    }

    private String callModel(TravelPlanRequest request, String conversationId) {
        return chatClient.prompt()
                .system(TravelPrompt.SYSTEM_PROMPT)
                .user(buildPlanPrompt(request))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolCallbacks(toolProvidersRegistry.getAllTools())
                .call()
                .content();
    }

    private String buildPlanPrompt(TravelPlanRequest request) {
        return """
                请根据以下信息生成一份旅行/约会规划：
                
                城市：%s
                出发地：%s
                目的区域：%s
                日期/时间：%s
                预算：%s
                人数/关系：%s
                偏好：%s
                用户补充要求：%s
                
                请输出可执行路线，并给出预算、交通、天气风险和备选方案。
                """.formatted(
                valueOrUnknown(request == null ? null : request.getCity()),
                valueOrUnknown(request == null ? null : request.getStartLocation()),
                valueOrUnknown(request == null ? null : request.getDestinationArea()),
                valueOrUnknown(request == null ? null : request.getDate()),
                valueOrUnknown(request == null ? null : request.getBudget()),
                valueOrUnknown(request == null ? null : request.getPeople()),
                valueOrUnknown(request == null ? null : request.getPreference()),
                resolveMessage(request)
        );
    }

    private String resolveMessage(TravelPlanRequest request) {
        if (request != null && StringUtils.hasText(request.getMessage())) {
            return request.getMessage();
        }
        return "请根据已知信息生成一份旅行规划。";
    }

    private String resolveConversationId(TravelPlanRequest request) {
        if (request != null && StringUtils.hasText(request.getConversationId())) {
            return request.getConversationId();
        }
        return UUID.randomUUID().toString();
    }

    private String valueOrUnknown(String value) {
        return StringUtils.hasText(value) ? value : "未提供";
    }

    private String writeMarkdown(String content) {
        try {
            Files.createDirectories(EXPORT_DIR);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path filePath = EXPORT_DIR.resolve("travel-plan-" + timestamp + ".md");
            Files.writeString(filePath, content, StandardCharsets.UTF_8);
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to export travel plan markdown", e);
        }
    }
}
```
