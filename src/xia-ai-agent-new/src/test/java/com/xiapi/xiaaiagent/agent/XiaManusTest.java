package com.xiapi.xiaaiagent.agent;

import com.xiapi.xiaaiagent.constant.FileConstant;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class XiaManusTest {

    @Resource
    private XiaManus xiaManus;

    @Test
    public void run() {
        String userPrompt = """
                我的房源居住在武汉理工大学南湖校区，请帮我找到 5 公里内合适的看房地点，
                并结合一些网络图片，制定一份详细的看房计划，一定要图片
                并以 PDF 格式输出""";
        String answer = xiaManus.run(userPrompt);
        System.out.println("========== Agent 执行结果 ==========");
        System.out.println(answer);
        System.out.println("========== PDF 文件保存在：" + FileConstant.FILE_SAVE_DIR + "/pdf/ ==========");
        Assertions.assertNotNull(answer);
    }
}