package com.usst.superai.agent;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.usst.superai.constant.AgentState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ToolCallAgent extends ReActAgent{

    private ToolCallingManager toolCallingManager;
    private ChatResponse toolCallChatResponse;
    private ChatOptions chatOptions;
    private ToolCallback[] availableTools;

    public ToolCallAgent(ToolCallback[] availableTools) {
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.availableTools = availableTools;
        // springai官方配置 禁用工具托管给模型 自己手动执行工具
        this.chatOptions = ToolCallingChatOptions.builder()
                .internalToolExecutionEnabled(false)
                .build();
    }

    @Override
    public boolean think() {
        if(getNextStepPrompt()!=null && !getNextStepPrompt().isEmpty()){
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        List<Message> messageList = getMessageList();
        // 构造目前的prompt对象
        Prompt prompt = new Prompt(messageList,chatOptions);
        try {
            ChatResponse chatResponse = this.getChatClient().prompt(prompt).system(getSystemPrompt()).toolCallbacks(availableTools).call().chatResponse();
            this.toolCallChatResponse = chatResponse;
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList= assistantMessage.getToolCalls();
            log.info(getName()+"的思考："+result);
            log.info(getName()+"预计执行的工具数目："+toolCallList.size());
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",getName(),toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            if(toolCallList.isEmpty()){
//                getMessageList().add(assistantMessage);
                return false;
            }else {
                return true;
            }
        } catch (Exception e) {
            log.info(getName()+"的调用遇到问题"+e.getMessage());
            messageList.add(new AssistantMessage("处理时遇到错误"+e.getMessage()));
            return false;
        }
    }

    @Override
    public String act() {
        if(!toolCallChatResponse.hasToolCalls()){
            return "没有工具调用";
        }
        Prompt prompt = new Prompt(getMessageList(),chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录工具执行上下文
        setMessageList(toolExecutionResult.conversationHistory());
        // 当前执行的工具结果信息
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(toolResponse -> "工具" + toolResponse.name() + "完成了它的任务，结果：" + toolResponse.responseData())
                .collect(Collectors.joining("/n"));
        boolean terminate = toolResponseMessage.getResponses().stream()
                .anyMatch(toolResponse -> toolResponse.name().equals("doTerminate"));
        if(terminate){
            setState(AgentState.FINISHED);
        }
        log.info("工具执行结果："+results);
        return results;
    }
}
