package com.usst.superai.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Base64;
import java.util.List;

//@Component
public class  FileChatMemoryRepository implements ChatMemoryRepository {

    private final ObjectMapper objectMapper;

    private final Path memoryDir = Paths.get("chat-memory");

    public FileChatMemoryRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        try {
            Files.createDirectories(memoryDir);
        } catch (IOException e) {
            throw new IllegalStateException("创建聊天记忆目录失败: " + memoryDir, e);
        }
    }

    @Override
    public List<String> findConversationIds() {
        try {
            if (!Files.exists(memoryDir)) {
                return List.of();
            }

            try (var stream = Files.list(memoryDir)) {
                return stream
                        .filter(path -> path.toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString())
                        .map(fileName -> fileName.substring(0, fileName.length() - ".json".length()))
                        .map(this::decodeConversationId)
                        .toList();
            }
        } catch (IOException e) {
            throw new IllegalStateException("读取会话列表失败", e);
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        Path filePath = getFilePath(conversationId);

        if (!Files.exists(filePath)) {
            return List.of();
        }

        try {
            List<StoredMessage> storedMessages = objectMapper.readValue(
                    filePath.toFile(),
                    new TypeReference<List<StoredMessage>>() {}
            );

            return storedMessages.stream()
                    .map(this::toSpringAiMessage)
                    .toList();

        } catch (IOException e) {
            throw new IllegalStateException("读取聊天记忆失败, conversationId = " + conversationId, e);
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");
        Assert.notNull(messages, "messages cannot be null");
        Assert.noNullElements(messages, "messages cannot contain null elements");

        Path filePath = getFilePath(conversationId);

        List<StoredMessage> storedMessages = messages.stream()
                .map(this::toStoredMessage)
                .toList();

        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(filePath.toFile(), storedMessages);
        } catch (IOException e) {
            throw new IllegalStateException("保存聊天记忆失败, conversationId = " + conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        Assert.hasText(conversationId, "conversationId cannot be null or empty");

        Path filePath = getFilePath(conversationId);

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new IllegalStateException("删除聊天记忆失败, conversationId = " + conversationId, e);
        }
    }

    private Path getFilePath(String conversationId) {
        String fileName = encodeConversationId(conversationId) + ".json";
        return memoryDir.resolve(fileName);
    }

    private StoredMessage toStoredMessage(Message message) {
        return new StoredMessage(
                message.getMessageType().name(),
                message.getText()
        );
    }

    private Message toSpringAiMessage(StoredMessage storedMessage) {
        return switch (storedMessage.type()) {
            case "USER" -> new UserMessage(storedMessage.text());
            case "ASSISTANT" -> new AssistantMessage(storedMessage.text());
            case "SYSTEM" -> new SystemMessage(storedMessage.text());
            default -> throw new IllegalArgumentException("不支持的消息类型: " + storedMessage.type());
        };
    }

    private String encodeConversationId(String conversationId) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(conversationId.getBytes(StandardCharsets.UTF_8));
    }

    private String decodeConversationId(String encoded) {
        byte[] bytes = Base64.getUrlDecoder().decode(encoded);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public record StoredMessage(
            String type,
            String text
    ) {
    }
}