package org.springblade.doc.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springblade.doc.entity.DocDetail;
import org.springblade.doc.entity.DocImageFile;
import org.springblade.doc.entity.DocSecCategory;
import org.springblade.doc.mapper.DocDetailMapper;
import org.springblade.doc.mapper.DocImageFileMapper;
import org.springblade.doc.mapper.DocSecCategoryMapper;
import org.springblade.doc.store.IDocFileStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 内容引擎 - 测试数据种子（仅内容管理员）。
 *
 * <p>无数据时一键造几条 docx/xlsx/pdf 示例文档，便于联调在线编辑/预览功能。
 * 非生产必需，保留为演示/联调用途；文件用最小合法 OOXML + 手写最小 PDF 生成，不引入额外依赖。</p>
 */
@Slf4j
@RestController
@RequestMapping("/doc/dev")
@RequiredArgsConstructor
@Tag(name = "内容引擎-测试数据", description = "造示例文档（docx/xlsx/pdf）")
public class DocDevSeedController {

    private final IDocFileStore fileStore;
    private final DocDetailMapper detailMapper;
    private final DocImageFileMapper imageFileMapper;
    private final DocSecCategoryMapper categoryMapper;

    @PreAuth(DocConstant.HAS_ROLE_DOC)
    @PostMapping("/seed")
    @Operation(summary = "生成示例文档", description = "造 docx/xlsx/pdf 各一条，挂到首个目录；便于联调预览/编辑功能")
    public R<List<Long>> seed() throws IOException {
        DocSecCategory cat = categoryMapper.selectOne(Wrappers.<DocSecCategory>lambdaQuery().last("limit 1"));
        Long catId;
        if (cat == null) {
            cat = new DocSecCategory();
            cat.setName("示例目录");
            categoryMapper.insert(cat);
            catId = cat.getId();
        } else {
            catId = cat.getId();
        }

        List<Long> ids = new ArrayList<>();
        ids.add(create("示例文档-Excel.xlsx", 8, catId, buildXlsx()));
        ids.add(create("示例文档-Word.docx", 7, catId, buildDocx()));
        ids.add(create("示例文档-PDF.pdf", 2, catId, buildPdf()));
        // 一个不支持的格式，用于演示「显示不了」兜底
        ids.add(create("示例文档-纯文本.txt", 2, catId,
            "这是一段纯文本内容，用于演示不支持格式时提示「显示不了」。".getBytes(StandardCharsets.UTF_8)));
        return R.data(ids);
    }

    private Long create(String fileName, int docFileType, Long catId, byte[] bytes) {
        Long fileId = fileStore.save(bytes);

        DocImageFile img = new DocImageFile();
        img.setVersionId(fileId);
        img.setFileId(fileId);
        img.setFileName(fileName);
        img.setFileSize((long) bytes.length);
        img.setDocFileType(docFileType);
        imageFileMapper.insert(img);

        DocDetail doc = new DocDetail();
        doc.setDocSubject(fileName);
        doc.setSecCategoryId(catId);
        doc.setDocStatus(1);
        doc.setDocEdition(1);
        doc.setIsHistory(0);
        doc.setIsValid(1);
        doc.setMainFileId(fileId);
        detailMapper.insert(doc);

        // 回填文件关联所属文档
        img.setDocId(doc.getId());
        imageFileMapper.updateById(img);
        return doc.getId();
    }

    // ───────────── 最小合法文件字节构造 ─────────────

    private byte[] buildPdf() {
        String obj1 = "<< /Type /Catalog /Pages 2 0 R >>";
        String obj2 = "<< /Type /Pages /Kids [3 0 R] /Count 1 >>";
        String obj3 = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R "
            + "/Resources << /Font << /F1 5 0 R >> >> >>";
        String text = "BT /F1 24 Tf 72 700 Td (Hello DocEngine - PDF sample) Tj ET";
        String obj4 = "<< /Length " + text.length() + " >>\nstream\n" + text + "\nendstream";
        String obj5 = "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>";

        StringBuilder sb = new StringBuilder("%PDF-1.4\n");
        int[] off = new int[5];
        off[0] = sb.length();
        sb.append("1 0 obj\n").append(obj1).append("\nendobj\n");
        off[1] = sb.length();
        sb.append("2 0 obj\n").append(obj2).append("\nendobj\n");
        off[2] = sb.length();
        sb.append("3 0 obj\n").append(obj3).append("\nendobj\n");
        off[3] = sb.length();
        sb.append("4 0 obj\n").append(obj4).append("\nendobj\n");
        off[4] = sb.length();
        sb.append("5 0 obj\n").append(obj5).append("\nendobj\n");

        int xref = sb.length();
        sb.append("xref\n0 6\n0000000000 65535 f \n");
        for (int i = 0; i < 5; i++) {
            sb.append(String.format("%010d 00000 n \n", off[i]));
        }
        sb.append("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        return sb.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private byte[] buildXlsx() throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("[Content_Types].xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                + "<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>"
                + "</Types>");
        m.put("_rels/.rels",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>");
        m.put("xl/workbook.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                + "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
        m.put("xl/_rels/workbook.xml.rels",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>"
                + "</Relationships>");
        m.put("xl/worksheets/sheet1.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<sheetData><row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c></row></sheetData></worksheet>");
        m.put("xl/sharedStrings.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"1\" uniqueCount=\"1\">"
                + "<si><t>内容引擎示例</t></si></sst>");
        return zip(m);
    }

    private byte[] buildDocx() throws IOException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("[Content_Types].xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>");
        m.put("_rels/.rels",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                + "</Relationships>");
        m.put("word/document.xml",
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body><w:p><w:r><w:t>内容引擎示例文档（docx）</w:t></w:r></w:p></w:body></w:document>");
        return zip(m);
    }

    private byte[] zip(Map<String, String> entries) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }
}
