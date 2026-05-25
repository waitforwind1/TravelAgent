package com.usst.superai.service.Impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.ansi.AnsiColors;
import com.alibaba.cloud.ai.advisor.DocumentRetrievalAdvisor;
import com.usst.superai.Exception.BusinessException;
import com.usst.superai.Tools.ToolProvidersRegistry;
import com.usst.superai.model.dto.*;
import com.usst.superai.model.result.ErrorCode;
import com.usst.superai.constant.TravelPrompt;
import com.usst.superai.service.TravelPlanService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class TravelPlanServiceImpl implements TravelPlanService {

    @Resource
    private ChatClient travelChatClient;

    @Resource
    private ToolProvidersRegistry toolProvidersRegistry;

    @Value("${app.downloadPrefix}")
    private String downloadPrefix;

    @Value("${app.filedir}")
    private String exportDir;

    @Autowired
    private VectorStore simpleVectorStore;

    /**
     * 这个和下面的chat方法其实是一样的 就看前端传来的请求的ID 根据ID分发不同对话
     * @param request
     * @return
     */
    @Override
    public TravelPlanResponse generatePlan(TravelPlanRequest request) {
        String conversationId = resolveConversationId(request);
        String content = callModel(request,conversationId);
        return new TravelPlanResponse(conversationId,content);
    }

    /**
     * 和上边的功能一样
     * @param request
     * @return
     */
    @Override
    public TravelPlanResponse chat(TravelPlanRequest request) {
        String conversationId = resolveConversationId(request);
        String content = travelChatClient.prompt()
                // todo:刚才是把这行代码给注释了 速度比之前快不少  加上之后还可以 和之前差不多
                .system(TravelPrompt.SYSTEM_PROMPT)
                .user(resolveMessage(request))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID,conversationId))
                .toolCallbacks(toolProvidersRegistry.getAllTools())
                .call().content();
        return new TravelPlanResponse(conversationId,content);
    }

    @Override
    public TravelPlanResponse chatWithRag(TravelPlanRequest request) {
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .similarityThreshold(0.50)
                        .vectorStore(simpleVectorStore)
                        .build())
//                允许检索为空的时候自主思考
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
        String conversationId = resolveConversationId(request);
        String content = travelChatClient.prompt()
                .system(TravelPrompt.SYSTEM_PROMPT)
                .user(resolveMessage(request))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID,conversationId))
                //加入RAG检索
                .advisors(retrievalAugmentationAdvisor)
                .toolCallbacks(toolProvidersRegistry.getAllTools())
                .call().content();
        return new TravelPlanResponse(conversationId,content);
    }

    /**
     * 流式输出 响应式
     * @param request
     * @return
     */
    @Override
    public Flux<String> streamPlan(TravelPlanRequest request) {
        String conversationId = resolveConversationId(request);
        Flux<String> contentStream = travelChatClient.prompt()
                .system(TravelPrompt.SYSTEM_PROMPT)
                .user(buildPlanPrompt(request))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                // RAG还没有设置
                .toolCallbacks(toolProvidersRegistry.getAllTools())
                .stream()
                .content();
        return Flux.concat(Flux.just("conversationId:"+conversationId+"\n\n"),contentStream);
    }

    /**
     * 生成然后导出计划 返回的data里有下载链接
     * @param request
     * @return
     */
    @Override
    public TravelExportResponse generateAndExport(TravelPlanRequest request) {
        TravelPlanResponse response = generatePlan(request);
        String content = response.content();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成内容为空，请重新生成");
        }
        return new TravelExportResponse(response.conversationId(),response.content(),markdownWriter(content));
    }


    /**
     * 下载文件的接口 会根据当前文件名映射到他所在在目录  创建一个文件 然后把该文件写入到response对象中返回
     * @param filename
     * @param response
     */
    @Override
    public void downloadFile(String filename, HttpServletResponse response) {
        try {
            String safeFileName = FileUtil.getName(filename);
            File file = FileUtil.file(exportDir, safeFileName);
            if(!file.exists()){
                response.setStatus(ErrorCode.DATA_NOT_EXIST.getCode());
                return;
            }
            response.setContentType("application/octet-stream");
            response.setHeader(
                    "Content-Disposition",
                    "attachment; filename=\"" + safeFileName + "\""
            );
            FileUtil.writeToStream(file,response.getOutputStream());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"文件下载失败");
        }
    }

    /**
     * 根据内容写入到指定目录下 作为待下载
     * @param content
     * @return
     */
    private String markdownWriter(String content){
        try {
            FileUtil.mkdir(exportDir);
            String timeStamp = DateUtil.format(DateUtil.date(),"yyMMddHHmmss");
            String filename = "travel-plan-"+timeStamp+".md";
            String filePath = exportDir+"/"+filename;
            Files.writeString(Path.of(filePath),content,StandardCharsets.UTF_8);
            return downloadPrefix+filename;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,e.getMessage());
        }
    }

    private String resolveConversationId(TravelPlanRequest travelPlanRequest){
        if(travelPlanRequest!=null && StringUtils.hasText(travelPlanRequest.getConversationId()))
            return travelPlanRequest.getConversationId();
        return UUID.randomUUID().toString();
    }

    /**
     * 调用client生成回答内容
     * @param travelPlanRequest
     * @param conversationId
     * @return
     */
    private String callModel(TravelPlanRequest travelPlanRequest,String conversationId){
        try {
            return travelChatClient.prompt()
                    .system(TravelPrompt.SYSTEM_PROMPT)
                    .user(buildPlanPrompt(travelPlanRequest))
                    .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolCallbacks(toolProvidersRegistry.getAllTools())
                    .call()
                    .content();
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "DeepSeek 响应超时或接口返回异常，请稍后重试");
        }
    }

    private String valueOrUnknown(String value){
        return StringUtils.hasText(value)?value:"未提供";
    }
    private String resolveMessage(TravelPlanRequest travelPlanRequest){
        String message = travelPlanRequest.getMessage();
        if(StringUtils.hasText(message))
            return message;
        return "请根据已提供信息生成一份旅游规划";
    }

    private String buildPlanPrompt(TravelPlanRequest travelPlanRequest){
        return """
                根据以下信息，生成一份专业的旅游规划，
                目标城市：%s,
                出发地点：%s,
                目标区域：%s,
                日期时间：%s,
                预算：%s,
                出行人数：%s,
                偏好要求：%s,
                用户补充要求：%s
                """.formatted(
                        valueOrUnknown(travelPlanRequest.getCity()),
                valueOrUnknown(travelPlanRequest.getStartLocation()),
                valueOrUnknown(travelPlanRequest.getDestinationArea()),
                valueOrUnknown(travelPlanRequest.getDateTime()),
                valueOrUnknown(travelPlanRequest.getBudget()),
                valueOrUnknown(travelPlanRequest.getPeople()),
                valueOrUnknown(travelPlanRequest.getPreference()),
                resolveMessage(travelPlanRequest)
        );
    }


}
