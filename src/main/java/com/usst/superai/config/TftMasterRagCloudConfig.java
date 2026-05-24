package com.usst.superai.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 使用阿里云的云知识库做RGA检索
 */
//@Configuration
public class TftMasterRagCloudConfig {

    @Bean
    public DashScopeApi dashScopeApi(
            @Value("${spring.ai.dashscope.api-key}") String apiKey
    ) {
        return DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
    }

    @Bean
    public DocumentRetriever lolMasterRagCloudAdvisor(DashScopeApi dashScopeApi){
        final String DASHSCOPE_INDNEX_NAME = "云顶之弈教程";
        return new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .indexName(DASHSCOPE_INDNEX_NAME)
                        .build());
    }
}
