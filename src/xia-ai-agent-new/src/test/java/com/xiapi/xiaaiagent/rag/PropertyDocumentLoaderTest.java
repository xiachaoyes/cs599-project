package com.xiapi.xiaaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PropertyDocumentLoaderTest {

    @Resource
    private PropertyDocumentLoader propertyDocumentLoader;

    @Test
    void loadMarkdowns() {
        propertyDocumentLoader.loadMarkdowns();
    }
}