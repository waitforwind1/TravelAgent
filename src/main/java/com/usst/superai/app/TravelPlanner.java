package com.usst.superai.app;

import com.usst.superai.Tools.ToolProvidersRegistry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TravelPlanner {

    @Resource
    @Qualifier("expertChatClient")
    private  ChatClient expertChatClient;

    @Resource
    @Qualifier("travelChatClient")
    private  ChatClient novelChatClient;

    @Resource
    private ToolProvidersRegistry toolProvidersRegistry;

    @Resource
    private Advisor myAdvisor;

    // 简单的AI应用（可以使用工具）
    public String doChat(String message, String conversationId) {

        return novelChatClient.prompt()
                .user(message)
                // 设置了用户对话上下文 以及上下文存储的地方 按照ID存储
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 提供工具调用 包括MCP服务  自定义展开了所有ToolCallbackProvider对象
                .toolCallbacks(toolProvidersRegistry.getAllTools())
                .call()
                .content();
    }

    // 加入RAG检索的应用 RAG独自分开出来 这个应用回复比较慢 每次新开对话需要加载文档到向量数据库进行检索
    public String doChatWithRAG(String message, String conversationId) {

        return novelChatClient.prompt()
                .user(message)
                // 设置了用户对话上下文 以及上下文存储的地方 按照ID存储
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // 设置了自定义advisor 其中设置了RAG向量数据库存储于内存中 自定义markdown文档切割 放入内存向量数据库中
                .advisors(myAdvisor)
                // 提供工具调用 包括MCP服务  自定义展开了所有ToolCallbackProvider对象
                .toolCallbacks(toolProvidersRegistry.getAllTools())
                .call()
                .content();
    }


}
