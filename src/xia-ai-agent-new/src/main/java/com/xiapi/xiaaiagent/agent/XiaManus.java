package com.xiapi.xiaaiagent.agent;

import com.xiapi.xiaaiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * xiapi 的 AI 超级智能体（拥有自主规划能力，可以直接使用）
 */
@Component
public class XiaManus extends ToolCallAgent {

    private final ToolCallbackProvider toolCallbackProvider;

    public XiaManus(ToolCallback[] allTools, ChatModel dashscopeChatModel, ToolCallbackProvider toolCallbackProvider) {
        super(allTools);
        this.toolCallbackProvider = toolCallbackProvider;
        this.setName("xiaManus");
        String SYSTEM_PROMPT = """
                You are XiaManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    /**
     * 合并本地工具和 MCP 远程工具
     */
    @Override
    protected ToolCallback[] getToolCallbacks() {
        ToolCallback[] localTools = super.getToolCallbacks();
        ToolCallback[] mcpTools = toolCallbackProvider.getToolCallbacks();
        //创建一个新数组 merged，长度等于两者之和，先复制 localTools 的全部元素。
        ToolCallback[] merged = Arrays.copyOf(localTools, localTools.length + mcpTools.length);
        //将 mcpTools 的所有元素复制到 merged 中紧接 localTools 之后的位置。
        System.arraycopy(mcpTools, 0, merged, localTools.length, mcpTools.length);
        return merged;
    }
}
