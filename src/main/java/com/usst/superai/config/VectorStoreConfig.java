package com.usst.superai.config;


import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import redis.clients.jedis.JedisPooled;

import java.util.List;

@Configuration
public class VectorStoreConfig {

    @Resource
    private MarkdownDocument markdownDocument;

    /**
     * 调用SimpleVectorStore构造一个向量数据库对象 存储文档 并转换为向量
     * 本来这个配置类里还有redis做vectorStore 删掉了
     * @param zhipuaiEmbeddingModel
     * @return
     */
    @Bean
    public VectorStore simpleVectorStore(@Qualifier("zhiPuAiEmbeddingModel") EmbeddingModel zhipuaiEmbeddingModel){
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(zhipuaiEmbeddingModel).build();
        List<Document> documentList = markdownDocument.loadMarkdown();
        simpleVectorStore.add(documentList);
        return simpleVectorStore;
    }

}
