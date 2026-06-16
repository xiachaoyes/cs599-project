package com.xiapi.xiaaiagent.controller;

import com.xiapi.xiaaiagent.agent.XiaManus;
import com.xiapi.xiaaiagent.app.PropertyApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private PropertyApp propertyApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    /**
     * 同步调用 AI 房产分析助手应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/property_app/chat/sync")
    public String doChatWithPropertyAppSync(String message, String chatId) {
        return propertyApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI 房产分析助手应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/property_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithPropertyAppSSE(String message, String chatId) {
        return propertyApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 房产分析助手应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/property_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithPropertyAppServerSentEvent(String message, String chatId) {
        return propertyApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 房产分析助手应用
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/property_app/chat/sse_emitter")
    public SseEmitter doChatWithPropertyAppServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        propertyApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        XiaManus xiaManus = new XiaManus(allTools, dashscopeChatModel, toolCallbackProvider);
        return xiaManus.runStream(message);
    }
}
