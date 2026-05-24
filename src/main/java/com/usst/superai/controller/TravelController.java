package com.usst.superai.controller;

import cn.hutool.core.util.StrUtil;
import com.usst.superai.Exception.BusinessException;
import com.usst.superai.model.dto.TravelExportResponse;
import com.usst.superai.model.dto.TravelPlanResponse;
import com.usst.superai.model.result.BaseResponse;
import com.usst.superai.model.result.ErrorCode;
import com.usst.superai.model.dto.TravelPlanRequest;
import com.usst.superai.service.TravelPlanService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;


@RestController
@RequestMapping("/travel")
@Tag(name = "AI旅行规划")
@Slf4j
public class TravelController {

    @Resource
    private TravelPlanService travelPlanService;

    /**
     * 和下面的接口功能一样 根据请求生成返回内容
     * @param request
     * @return
     */
    @PostMapping("/plan")
    public BaseResponse<TravelPlanResponse> generatePlan(@RequestBody TravelPlanRequest request) {
        validatePlanRequest(request);
        return BaseResponse.success(travelPlanService.generatePlan(request));
    }

    /**
     * 根据请求生成返回内容
     * @param request
     * @return
     */
    @PostMapping("/chat")
    public BaseResponse<TravelPlanResponse> chat(@RequestBody TravelPlanRequest request) {
        validateChatRequest(request);
        return BaseResponse.success(travelPlanService.chat(request));
    }

    /**
     * 响应式输出
     * @param request
     * @return
     */
    @PostMapping(value = "/plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamPlan(@RequestBody TravelPlanRequest request) {
        validatePlanRequest(request);
        return travelPlanService.streamPlan(request);
    }

    /**
     * 根据提供的文件名下载文件
     * @param filename
     * @param response
     */
    @GetMapping("/plan/download/{filename}")
    public void downloadFile(@PathVariable String filename, HttpServletResponse response) {
        validateDownloadFilename(filename);
        travelPlanService.downloadFile(filename, response);
    }

    /**
     * 生成并导出
     * @param request
     * @return
     */
    @PostMapping("/plan/generateAndExport")
    public BaseResponse<TravelExportResponse> generateAndExport(@RequestBody TravelPlanRequest request) {
        validatePlanRequest(request);
        return BaseResponse.success(travelPlanService.generateAndExport(request));
    }

    /**
     * 加入了RAG检索增强（内存实现）的接口 可能会比较慢
     * @param request
     * @return
     */
    @PostMapping("/chatWithRag")
    public BaseResponse<TravelPlanResponse> chatWithRag(@RequestBody TravelPlanRequest request) {
        validateChatRequest(request);
        return BaseResponse.success(travelPlanService.chatWithRag(request));
    }

    /**
     * 校验对话请求以及内部的参数
     * @param request
     */
    private void validatePlanRequest(TravelPlanRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        boolean hasCity = StringUtils.hasText(request.getCity());
        boolean hasMessage = StringUtils.hasText(request.getMessage());

        if (!hasCity && !hasMessage) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请填写目标城市或补充需求");
        }
    }

    /**
     * 校验请对话求以及内部的参数
     * @param request
     */
    private void validateChatRequest(TravelPlanRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }

        if (!StringUtils.hasText(request.getConversationId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "conversationId不能为空");
        }

        if (!StringUtils.hasText(request.getMessage())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入聊天内容");
        }
    }

    /**
     * 校验下载请求以及内部的参数
     * @param filename
     */
    private void validateDownloadFilename(String filename) {
        if (!StringUtils.hasText(filename)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不合法");
        }

        if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名只能包含字母、数字、点、下划线和中划线");
        }

        if (!filename.endsWith(".md")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只允许下载 Markdown 文件");
        }
    }
}