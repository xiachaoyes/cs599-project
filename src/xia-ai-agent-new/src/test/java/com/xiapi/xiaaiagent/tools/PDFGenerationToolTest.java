package com.xiapi.xiaaiagent.tools;

import com.xiapi.xiaaiagent.constant.FileConstant;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();

        // 扫描本地 download 目录中的图片，生成内嵌图片的 markdown
        StringBuilder content = new StringBuilder();
        content.append("# 房产分析测试报告\n\n");
        content.append("## 基本信息\n\n");
        content.append("这份报告通过本地图片验证 PDF 的 Markdown 渲染效果。\n\n");

        // 扫描 download 目录
        String downloadDir = FileConstant.FILE_SAVE_DIR + "/download";
        Path downloadPath = Path.of(downloadDir);
        if (Files.isDirectory(downloadPath)) {
            try (var stream = Files.list(downloadPath)) {
                var images = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String n = p.getFileName().toString().toLowerCase();
                            return n.matches(".*\\.(jpg|jpeg|png|gif|webp)$")
                                    || (!n.contains(".") && Files.isReadable(p));
                        })
                        .collect(Collectors.toList());

                if (!images.isEmpty()) {
                    content.append("## 本地图片（共 ").append(images.size()).append(" 张）\n\n");
                    for (Path img : images) {
                        String name = img.getFileName().toString();
                        String alt = name.replaceFirst("\\.[^.]+$", "");
                        // 用 file:// 路径让 addImage 优先走文件名匹配
                        content.append("![").append(alt).append("](").append(name).append(")\n\n");
                    }
                }
            } catch (Exception e) {
                content.append("(扫描图片失败：" + e.getMessage() + ")\n\n");
            }
        } else {
            content.append("(download 目录不存在：" + downloadDir + ")\n\n");
        }

        content.append("## 测试结论\n\n");
        content.append("- Markdown 标题渲染正常\n");
        content.append("- 列表项渲染正常\n");
        content.append("- 图片从本地 download 目录嵌入正常\n");
        content.append("- 没有 emoji 乱码问题\n");

        String result = tool.generatePDF("房产分析测试报告.pdf", content.toString());
        System.out.println(result);
        System.out.println("PDF 文件路径: " + FileConstant.FILE_SAVE_DIR + "/pdf/房产分析测试报告.pdf");
    }
}
