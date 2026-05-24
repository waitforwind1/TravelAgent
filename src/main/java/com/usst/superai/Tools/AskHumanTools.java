package com.usst.superai.Tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Scanner;

public class AskHumanTools {

    @Tool(description = "Use this tool to ask human for help")
    public String askHuman(@ToolParam(description = "The question you want to ask human") String inquire){
        System.out.println("XManus: "+inquire);
        Scanner scanner = new Scanner(System.in);
        System.out.println("补充：");
        return scanner.nextLine();
    }
}
