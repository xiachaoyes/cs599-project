package com.xiapi.xiaaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class PropertyAppTest {

    @Resource
    private PropertyApp propertyApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是程序员xiapi";
        String answer = propertyApp.doChat(message, chatId);
        // 第二轮
        message = "我想在武汉买一套三居室的房子，有什么推荐吗？";
        answer = propertyApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "刚才我说过想买什么来着？请帮我回忆一下";
        answer = propertyApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我想了解光谷东的楼盘情况，请给我出一份分析报告";
        PropertyApp.PropertyReport propertyReport = propertyApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(propertyReport);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "买房首付不够怎么办？有什么建议吗？";
        String answer = propertyApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithTools() {
        // 测试联网搜索：房价查询
        testMessage("武汉光谷东2025年房价走势如何？帮我搜索一下");

        // 测试网页抓取：政策查询
        testMessage("帮我查一下2025年武汉最新购房政策");

        // 测试资源下载：小区图片
        testMessage("下载一些武汉光谷东高档小区的实景图片");

        // 测试终端操作：数据分析
        testMessage("执行 Python3 脚本来分析近3个月的房价数据");

        // 测试文件操作：保存分析数据
        testMessage("保存我的房产分析数据到文件");

        // 测试 PDF 生成
        testMessage("生成一份'武汉光谷东房产分析报告'PDF，包含楼盘对比、周边配套和购房建议");
    }

    private void testMessage(String message) {
        String chatId = UUID.randomUUID().toString();
        String answer = propertyApp.doChatWithTools(message, chatId);
        System.out.println(answer);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp() {
        String chatId = UUID.randomUUID().toString();
        // 测试地图 MCP
        String message = "武汉理工大学南湖校区周边有哪些新楼盘？帮我搜索 5 公里内的，务必保留每个楼盘的图片";
        String answer = propertyApp.doChatWithMcp(message, chatId);
        System.out.println(answer);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatWithMcp2() {
        String chatId = UUID.randomUUID().toString();
        // 测试图片搜索 MCP
        String message = "帮我搜索一些高档精装修房源的实景图片";
        String answer = propertyApp.doChatWithMcp(message, chatId);
        System.out.println(answer);
        Assertions.assertNotNull(answer);
    }

}
