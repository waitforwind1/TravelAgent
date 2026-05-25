package com.usst.superai.config;

import com.usst.superai.Advisors.Re2Advisor;
import com.usst.superai.constant.SystemTemplate;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ChatClientConfig {

    @Resource
    private ChatMemory jdbcChatMemory;

    @Resource
    private ChatMemory inChatMemory;

    @Bean
    public RestClientCustomizer deepSeekRestClientTimeoutCustomizer() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMinutes(3));

        return builder -> builder.requestFactory(requestFactory);
    }

    @Bean
    public ChatClient travelChatClient(@Qualifier("deepSeekChatModel") ChatModel deepseekChatModel){
        return ChatClient.builder(deepseekChatModel)
                .defaultSystem(SystemTemplate.DEFAULT_SYSTEM_TEMPLATE)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(inChatMemory).build())
                .build();
    }

    @Bean
    public ChatClient expertChatClient(@Qualifier("deepSeekChatModel") ChatModel deepseekChatModel){
        return ChatClient.builder(deepseekChatModel)
                .defaultSystem("你充当用户的知识顾问，为用户答疑解惑，回答精确")
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(jdbcChatMemory).build())
                .build();
    }

    @Bean
    public ChatClient lolChatClient(@Qualifier("dashScopeChatModel")ChatModel dashScopeChatModel){
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(jdbcChatMemory).build())
                .defaultSystem("你是一名LOL职业选手")
                .build();
    }

    @Bean
    public ChatClient.Builder dashScopeBuilder(ChatModel dashScopeChatModel){
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(new Re2Advisor());
    }
}
