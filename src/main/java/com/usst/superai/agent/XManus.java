package com.usst.superai.agent;

import com.usst.superai.Advisors.MyLogAdvisor;
import com.usst.superai.Tools.ToolProvidersRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class XManus extends ToolCallAgent {

    public XManus(ToolProvidersRegistry toolProvidersRegistry, ChatModel deepSeekChatModel) {
        super(toolProvidersRegistry.getAllTools());
        setName("XManus");
        String SYSTEM_PROMPT = """  
                You are XManus, an all-capable AI assistant, aimed at solving any task presented by the user.  
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.  
                """;
        setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """  
                Based on user needs, proactively select the most appropriate tool or combination of tools.  
                For complex tasks, you can break down the problem and use different tools step by step to solve it.  
                After using each tool, clearly explain the execution results and suggest the next steps.  
                If you want to stop the interaction at any point, use the `terminate` tool/function call.  
                """;
        setNextStepPrompt(NEXT_STEP_PROMPT);
        setMaxAgentSteps(20);
        setChatClient(ChatClient.builder(deepSeekChatModel).defaultSystem(SYSTEM_PROMPT).build());
    }
}
