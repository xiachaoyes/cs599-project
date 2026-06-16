package com.xiapi.xiaaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceDownloadToolTest {

    @Test
    public void testDownloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String url = "https://pic.616pic.com/ys_bnew_img/00/43/05/iCoQGsbKwl.jpg";
        String fileName = "logo.png";
        String result = tool.downloadResource(url, fileName);
        System.out.println(result);
        assertNotNull(result);
    }
}