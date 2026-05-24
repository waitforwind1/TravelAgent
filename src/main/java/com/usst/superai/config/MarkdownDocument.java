package com.usst.superai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MarkdownDocument {

    private ResourcePatternResolver resourcePatternResolver;

    public MarkdownDocument(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 使用ResourcePatternResolver 来获取本地的大量对象  得到一个resource列表
     * ResourceLoader：获取一个资源
     * ResourcePatternResolver：按路径规则获取一批资源
     * @return
     */
    public List<Document> loadMarkdown(){
        List<Document> documentList = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .build();
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource,config);
                documentList.addAll(markdownDocumentReader.get());
            }
        } catch (IOException e) {
            log.error("读取本地文档错误:",e);
            throw new RuntimeException(e);
        }
        return documentList;
    }

}
