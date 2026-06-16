package com.xiapi.xiaaiagent.app;

import com.xiapi.xiaaiagent.advisor.MyLoggerAdvisor;
import com.xiapi.xiaaiagent.advisor.ReReadingAdvisor;
import com.xiapi.xiaaiagent.chatmemory.FileBasedChatMemory;
import com.xiapi.xiaaiagent.rag.PropertyRagCustomAdvisorFactory;
import com.xiapi.xiaaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class PropertyApp {

    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT = "你是一位深耕房产分析领域的专家，你可以调用以下工具来辅助解决用户房产需求：\n" +
            "- searchWeb 联网搜索：查询房价走势、政策动态、小区信息\n" +
            "- downloadResource 下载资源：下载网络图片到本地\n" +
            "- generatePDF 生成PDF：将内容生成为PDF文件\n" +
            "\n" +
            "核心规则：\n" +
            "1. 当用户要求生成PDF时，你必须调用 generatePDF 工具，把看房内容整理成 Markdown 格式传给工具\n" +
            "2. 工具返回 \"PDF generated successfully\" 表示成功，直接告知用户文件路径\n" +
            "3. 工具返回 \"PDF generation FAILED\" 才表示失败，把错误信息如实告诉用户\n" +
            "4. 不要凭空说 PDF 生成失败或有字体问题！以工具返回结果为准\n" +
            "5. 调用 generatePDF 时，在 content 中最好用 ![alt](文件名) 引用已经下载好的图片\n" +
            "6. 如果用户说都不限，就不要反复问，直接开始搜索并制定计划\n" +
            "\n";

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public PropertyApp(ChatModel dashscopeChatModel) {
//        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat-memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 初始化基于内存的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor，可按需开启
                        new MyLoggerAdvisor()
//                        // 自定义推理增强 Advisor，可按需开启
//                       ,new ReReadingAdvisor()
                )
                .build();
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(propertyTools)
                .stream()
                .content();
    }

    record PropertyReport(String title, List<String> suggestions) {

    }

    /**
     * AI 房产分析报告功能（实战结构化输出）
     *
     * @param message
     * @param chatId
     * @return
     */
    public PropertyReport doChatWithReport(String message, String chatId) {
        PropertyReport propertyReport = chatClient
                .prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成房产分析结果，标题为{用户名}的房产分析报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(PropertyReport.class);
        log.info("propertyReport: {}", propertyReport);
        return propertyReport;
    }

    // AI 房产知识库问答功能

    @Resource
    private VectorStore propertyVectorStore;

    @Resource
    private Advisor propertyRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    /**
     * 和 RAG 知识库进行对话
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                // 使用改写后的查询
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 应用 RAG 知识库问答
                .advisors(new QuestionAnswerAdvisor(propertyVectorStore))
                // 应用 RAG 检索增强服务（基于云知识库服务）
//                .advisors(propertyRagCloudAdvisor)
                // 应用 RAG 检索增强服务（基于 PgVector 向量存储）
                //.advisors(new QuestionAnswerAdvisor(pgVectorVectorStore))
                // 应用自定义的 RAG 检索增强服务（文档查询器 + 上下文增强器）
//                .advisors(
//                        PropertyRagCustomAdvisorFactory.createPropertyRagCustomAdvisor(
//                                propertyVectorStore, "买房"
//                        )
//                )
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ToolCallback[] propertyTools;

    /**
     * AI 房产分析报告功能（支持调用工具）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    // AI 调用 MCP 服务
    //自动注入的这一个玩意儿的作用是，读取配置类中的mcp配置，定位到JSON文件，然后将JSON文件中的工具注入到ToolCallbackProvider
    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * AI 房产分析报告功能（调用 MCP 服务）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
