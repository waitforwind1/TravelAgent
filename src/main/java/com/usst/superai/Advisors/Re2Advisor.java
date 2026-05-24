package com.usst.superai.Advisors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

@Slf4j
public class Re2Advisor implements StreamAdvisor, CallAdvisor {

    private ChatClientRequest buildRe2Request(ChatClientRequest chatClientRequest){
        String userText = chatClientRequest.prompt().getUserMessage().getText();
        String re2UserText = """
        %s
        
        Read the question again: %s
        """.formatted(userText, userText);
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(re2UserText))
                .build();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(buildRe2Request(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        return streamAdvisorChain.nextStream(buildRe2Request(chatClientRequest));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 15;
    }
}
