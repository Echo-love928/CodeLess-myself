package com.hjq.CodeLess.core;

import cn.hutool.core.io.FileUtil;
import com.hjq.CodeLess.ai.model.MultiFileCodeResult;
import com.hjq.CodeLess.core.saver.MultiFileCodeFileSaverTemplate;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeFileSaverTemplateTest {

    @Test
    void shouldUseAppIdAsPreviewDirectoryName() {
        long appId = 999999999999999001L;
        MultiFileCodeResult result = new MultiFileCodeResult();
        result.setHtmlCode("<!doctype html><html><body>preview</body></html>");
        result.setCssCode("body { color: black; }");
        result.setJsCode("console.log('ready');");

        File savedDir = new MultiFileCodeFileSaverTemplate().saveCode(result, appId);
        try {
            assertEquals("multi_file_" + appId, savedDir.getName());
            assertTrue(new File(savedDir, "index.html").isFile());
            assertTrue(new File(savedDir, "style.css").isFile());
            assertTrue(new File(savedDir, "script.js").isFile());
        } finally {
            FileUtil.del(savedDir);
        }
    }
}
