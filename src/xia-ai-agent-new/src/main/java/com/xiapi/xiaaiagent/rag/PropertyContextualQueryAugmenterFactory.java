package com.xiapi.xiaaiagent.rag;

import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

/**
 * 创建上下文查询增强器的工厂
 */
public class PropertyContextualQueryAugmenterFactory {

    public static ContextualQueryAugmenter createInstance() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate("""
                你应该输出下面的内容：
                抱歉，我只能回答房产相关的问题，别的没办法帮到您哦，
                有问题可以联系编程导航客服 https://xiapi.cc
                """);
        return ContextualQueryAugmenter.builder()
                //allowEmptyContext(false) 的意思是：
                //当向量检索没有匹配到任何文档（上下文为空）时，不允许继续执行原有的问答流程
                //不允许不带RAG的情况下胡编乱造
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyContextPromptTemplate)
                .build();
    }
}
