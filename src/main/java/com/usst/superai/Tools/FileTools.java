package com.usst.superai.Tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTools {

    private String fileDir;

    private String downloadPrefix;
    public FileTools(String fileDir,String downloadPrefix) {
        this.fileDir = fileDir;
        this.downloadPrefix = downloadPrefix;
    }

    @Tool(description = "这是一个读取文件的工具")
    public String FileReader(@ToolParam(description = "传入的文件待读取") String filename) {
        try {
            String fileContent = Files.readString(Path.of(fileDir + "/" + filename));
            return fileContent;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Tool(description = "将文本内容写入 Markdown 文件，并返回可下载链接。调用该工具后，必须把返回的下载链接原样展示给用户。")
    public String writeFile(@ToolParam(description = "文件名") String filename,
                            @ToolParam(description = "要写的文件内容") String content) {
        try {
            String safefileName = StringUtils.getFilename(filename);
            if(!safefileName.endsWith(".md")){
                safefileName = safefileName+".md";
            }
            String filepath = Path.of(fileDir,safefileName).toString();
            FileUtil.mkParentDirs(filepath);
            FileUtil.writeUtf8String(content, filepath);
            return "Successfully wite to: " + downloadPrefix+safefileName;
        } catch (IORuntimeException e) {
            return "写入失败：" + e.getClass().getSimpleName() + "，原因：" + e.getMessage();
        }
    }
}
