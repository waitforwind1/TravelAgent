package com.usst.superai.agent;

import com.usst.superai.Exception.AgentException;
import io.swagger.v3.core.util.ReferenceTypeUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 这里将step执行动作的方法 分解为think和act两块 正好符合ReAct模式
 */
@Slf4j
public abstract class ReActAgent extends BaseAgent{

    public abstract boolean think();

    public abstract String act();

    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if(!shouldAct){
                return "思考完成 无需执行动作";
            }
            return act();
        } catch (AgentException e) {
            return "步骤动作执行失败"+e.getMessage();
        }
    }
}
