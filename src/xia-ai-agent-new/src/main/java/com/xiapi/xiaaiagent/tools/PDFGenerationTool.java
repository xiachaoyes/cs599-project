package com.xiapi.xiaaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.xiapi.xiaaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 生成工具（支持 Markdown 解析渲染）
 */
public class PDFGenerationTool {

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,4})\\s+(.+)$");

    /**
     * 创建中文字体，从 classpath 加载项目内嵌的黑体（simhei.ttf）
     */
    private static PdfFont createChineseFont() throws IOException {
        try (var in = PDFGenerationTool.class.getResourceAsStream("/fonts/simhei.ttf")) {
            if (in != null) {
                byte[] fontBytes = in.readAllBytes();
                return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H);
            }
        }
        throw new IOException("Font not found in classpath: /fonts/simhei.ttf");
    }

    /**
     * 过滤掉 CMap 字体不支持的字符（emoji、surrogate pairs 等）
     * 保留：ASCII可打印字符、中文（CJK统一表意文字）、中文标点、全角字符
     */
    private static String sanitize(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t'                     // 控制字符
                    || (c >= 0x20 && c <= 0x7E)                         // ASCII 可打印
                    || (c >= 0x3000 && c <= 0x9FFF)                     // CJK 符号/标点/统一表意文字
                    || (c >= 0xFF00 && c <= 0xFFEF)                     // 全角字符
                    || (c >= 0x2000 && c <= 0x206F)                     // 通用标点
                    || (c >= 0x3300 && c <= 0x33FF)                     // CJK 兼容字符（如 ㎡）
            ) {
                sb.append(c);
            }
            // Surrogate pairs（emoji等高位字符）直接跳过
        }
        return sb.toString();
    }

    @Tool(description = "Generate a PDF file with given markdown content", returnDirect = false)
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF (supports markdown format)") String content) {
        String fileDir = FileConstant.FILE_SAVE_DIR + "/pdf";
        String filePath = fileDir + "/" + fileName;
        try {
            FileUtil.mkdir(fileDir);
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                PdfFont font = createChineseFont();
                document.setFont(font);

                String[] lines = content.split("\n");
                for (String line : lines) {
                    processLine(line, document, font);
                }
            }
            return "PDF generated successfully to: " + filePath + ". File size: " + Files.size(Path.of(filePath)) + " bytes";
        } catch (Exception e) {
            String errMsg = "PDF generation FAILED: " + e.getClass().getSimpleName() + " - " + e.getMessage();
            // 打印完整堆栈到日志，便于诊断
            System.err.println(errMsg);
            e.printStackTrace();
            return errMsg;
        }
    }

    private void processLine(String line, Document document, PdfFont font) throws IOException {
        String trimmed = sanitize(line.trim());

        // 跳过空行和水平分割线
        if (trimmed.isEmpty() || trimmed.matches("^-{3,}$")) {
            document.add(new Paragraph("").setMarginBottom(6));
            return;
        }

        // 图片 ![alt](url) — 用原始 line 做匹配（URL 中有特殊字符不要 sanitize）
        Matcher imgMatcher = IMAGE_PATTERN.matcher(line.trim());
        if (imgMatcher.matches()) {
            addImage(document, imgMatcher.group(2), imgMatcher.group(1));
            return;
        }
        Matcher inlineImgMatcher = IMAGE_PATTERN.matcher(line);
        if (inlineImgMatcher.find()) {
            addImage(document, inlineImgMatcher.group(2), inlineImgMatcher.group(1));
            return;
        }

        // 标题 # / ## / ### / ####
        Matcher headingMatcher = HEADING_PATTERN.matcher(trimmed);
        if (headingMatcher.matches()) {
            int level = headingMatcher.group(1).length();
            String headingText = stripBold(headingMatcher.group(2));
            float fontSize = level == 1 ? 18f : level == 2 ? 15f : level == 3 ? 13f : 11.5f;
            Paragraph heading = new Paragraph(headingText)
                    .setFont(font)
                    .setFontSize(fontSize)
                    .setMarginBottom(10 - level * 2);
            if (level == 1) {
                heading.setTextAlignment(TextAlignment.CENTER);
                heading.setMarginBottom(14);
            }
            document.add(heading);
            return;
        }

        // 列表项 - xxx  /  * xxx
        if (trimmed.matches("^[-*]\\s+.+")) {
            String itemText = stripBold(trimmed.replaceFirst("^[-*]\\s+", ""));
            document.add(new Paragraph("\u2022  " + itemText)
                    .setFont(font)
                    .setFontSize(11)
                    .setMarginLeft(20)
                    .setMarginBottom(3));
            return;
        }

        // 加粗子标题行：**xxx**
        if (trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length() > 4) {
            String boldText = trimmed.substring(2, trimmed.length() - 2);
            document.add(new Paragraph(boldText)
                    .setFont(font)
                    .setFontSize(12)
                    .setMarginBottom(6)
                    .setMarginLeft(4));
            return;
        }

        // 普通段落
        String paragraphText = stripBold(trimmed);
        document.add(new Paragraph(paragraphText)
                .setFont(font)
                .setFontSize(11)
                .setMarginBottom(5));
    }

    private String stripBold(String text) {
        return BOLD_PATTERN.matcher(text).replaceAll("$1");
    }

    /**
     * 下载图片并嵌入 PDF（优先 HTTP，失败则扫描本地 download 目录按 alt 名匹配）
     */
    private void addImage(Document document, String imgUrl, String imgAlt) {
        byte[] imgBytes = null;

        // 1. HTTP 下载
        if (imgUrl.startsWith("http://") || imgUrl.startsWith("https://")) {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(imgUrl)).timeout(Duration.ofSeconds(15)).GET().build();
                imgBytes = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
            } catch (Exception ignored) {}
        }

        // 2. 本地 download 目录 —— 按 url 末尾文件名匹配
        if (imgBytes == null) {
            String fileName = imgUrl.contains("/") ? imgUrl.substring(imgUrl.lastIndexOf('/') + 1) : imgUrl;
            Path localPath = Path.of(FileConstant.FILE_SAVE_DIR, "download", fileName);
            try { if (Files.exists(localPath)) imgBytes = Files.readAllBytes(localPath); }
            catch (Exception ignored) {}
        }

        // 3. 本地 download 目录 —— 扫描所有图片文件，优先按 alt 关键字 + 文件名匹配
        if (imgBytes == null) {
            try {
                Path downloadDir = Path.of(FileConstant.FILE_SAVE_DIR, "download");
                if (Files.isDirectory(downloadDir)) {
                    try (var stream = Files.list(downloadDir)) {
                        var list = stream.filter(Files::isRegularFile)
                                .filter(p -> {
                                    String n = p.getFileName().toString().toLowerCase();
                                    // 有已知图片扩展名的直接通过，无扩展名的也尝试（可能是下载时没带后缀）
                                    return n.matches(".*\\.(jpg|jpeg|png|gif|webp)$")
                                            || (!n.contains(".") && Files.isReadable(p));
                                })
                                .toList();
                        // 优先按 alt 名部分匹配
                        imgBytes = list.stream()
                                .filter(p -> {
                                    String n = p.getFileName().toString();
                                    String keyword = imgAlt.replaceAll("[的是一张\\s]", "");
                                    return keyword.length() >= 2 && n.contains(keyword.substring(0, Math.min(2, keyword.length())));
                                })
                                .findFirst()
                                .map(p -> { try { return Files.readAllBytes(p); } catch (IOException e) { return null; }})
                                .orElse(null);
                        // 退而求其次：取第一张
                        if (imgBytes == null && !list.isEmpty()) {
                            imgBytes = Files.readAllBytes(list.get(0));
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        // 4. 嵌入 PDF
        if (imgBytes != null && imgBytes.length > 0) {
            try {
                Image image = new Image(ImageDataFactory.create(imgBytes));
                float pageWidth = document.getPdfDocument().getDefaultPageSize().getWidth() - 60;
                if (image.getImageWidth() > pageWidth) {
                    image.setWidth(UnitValue.createPointValue(pageWidth));
                }
                image.setMarginBottom(10);
                document.add(image);
                return;
            } catch (Exception ignored) {}
        }

        // 5. 全部失败
        document.add(new Paragraph("[ 图片: " + sanitize(imgAlt) + " ]")
                .setFontSize(9).setMarginBottom(4));
    }
}
