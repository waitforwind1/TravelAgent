package com.usst.superai.agent;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.agent.Agent;
import com.usst.superai.Exception.AgentException;
import com.usst.superai.constant.AgentState;
import io.swagger.v3.core.util.ReferenceTypeUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.util.TypeCollector;
import org.springframework.http.client.support.InterceptingHttpAccessor;

import javax.management.monitor.StringMonitor;
import javax.swing.plaf.ColorUIResource;
import java.util.ArrayList;
import java.util.List;

@Data
@Slf4j
public abstract class BaseAgent {

    private String name;
    private String description;

    private String systemPrompt;
    private String nextStepPrompt;

    // 初始化agent状态
    private AgentState state = AgentState.IDLE;
    // 初始步数设置为0
    private Integer currentStep = 0;
    private Integer maxAgentSteps = 20;
    // 初始化agent消息列表
    private List<Message> messageList = new ArrayList<>();
    // 传入要使用的ChatClient
    private ChatClient chatClient;

    public String run(String userPrompt){
        if(this.state!=AgentState.IDLE){
            throw new AgentException("can not run from state :"+this.state);
        }
        if(StrUtil.isBlank(userPrompt)){
            throw new AgentException("can not run from blank userPrompt");
        }
        // 维护模型聊天上下文
        messageList.add(new UserMessage(userPrompt));
        List<String> results = new ArrayList<>();
        this.state = AgentState.RUNNING;
        try {
            while(currentStep<maxAgentSteps && state!= AgentState.FINISHED){
                currentStep+=1;
                log.info("Excuting Step：{}/{}",currentStep,maxAgentSteps);
                String stepResult = step();
                String res = "Step:"+currentStep+":"+stepResult;
                results.add(res);
            }
            return String.join("/n",results);
        } catch (AgentException e) {
            state  = AgentState.ERROR;
            return "执行错误"+e.getMessage();
        } finally {
            cleanUp();
        }
    }

    /**
     * 清理资源 初始化
     * @return
     */
    public Boolean cleanUp(){
        log.info("执行结束 重新初始化");
        currentStep = 0;
        if(state != AgentState.ERROR){
            state = AgentState.IDLE;
        }
        return true;
    }

    /**
     *  抽象方法 定义一个模板 用于给子类续写具体执行任务
     * @return
     */
    public abstract String step();
}
