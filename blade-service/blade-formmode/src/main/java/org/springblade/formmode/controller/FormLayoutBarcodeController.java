package org.springblade.formmode.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.workflow.constant.WorkflowConstant;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Excel 表单布局「二维/条形码」元素的码图生成。
 *
 * <p>对齐泛微 E9：</p>
 * <ul>
 *   <li>{@code /workflow/createQRCode?msg=...} → {@code CreateQRCodeServlet.java:43-69}（ZXing QR_CODE）</li>
 *   <li>{@code /workflow/createWfBarCode?msg=...&type=code128&hrp=none} →
 *       {@code ParseExcelLayout.java:2089-2124}（CODE_128，中文内容回退占位图）</li>
 * </ul>
 *
 * <p>本项目由 {@code ExcelPreview/components/ElementNodes.tsx#BarcodeImage} 以 fetch + Blob 方式取图，
 * 请求头携带 Blade-Auth，故无需为 {@code <img>} 单独加网关白名单。</p>
 *
 * <p>只依赖 zxing core：{@link BitMatrix} → JDK {@link BufferedImage} → PNG，
 * 不引入 javase 的 MatrixToImageWriter，避免传递依赖与 headless 环境差异。</p>
 */
@RestController
@RequestMapping("/form-layout/barcode")
@PreAuth(WorkflowConstant.HAS_AUTH)
@Tag(name = "表单布局-码图", description = "Excel 布局二维码/条形码图片生成")
public class FormLayoutBarcodeController {

    private static final int MAX_SIDE = 1000;
    private static final int MAX_TEXT = 2048;

    @GetMapping("/png")
    @Operation(summary = "生成二维码/条形码 PNG", description = "type=qr(QR_CODE,默认) | bar(CODE_128)；msg=编码内容；hrp=none 表示前端隐藏编码文本")
    public ResponseEntity<byte[]> png(
        @RequestParam(value = "type", defaultValue = "qr") String type,
        @RequestParam(value = "msg", defaultValue = "") String msg,
        @RequestParam(value = "width", defaultValue = "100") int width,
        @RequestParam(value = "height", defaultValue = "100") int height,
        @RequestParam(value = "hrp", required = false) String hrp) {

        String text = msg == null ? "" : msg.trim();
        if (text.isEmpty()) {
            text = " ";
        }
        if (text.length() > MAX_TEXT) {
            return badRequest("编码内容过长（上限 " + MAX_TEXT + " 字符）");
        }

        int w = clamp(width, "bar".equalsIgnoreCase(type) ? 160 : 100);
        int h = clamp(height, "bar".equalsIgnoreCase(type) ? 60 : 100);
        BarcodeFormat format = "bar".equalsIgnoreCase(type) ? BarcodeFormat.CODE_128 : BarcodeFormat.QR_CODE;

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.MARGIN, 1);

        try {
            BitMatrix matrix = new MultiFormatWriter().encode(text, format, w, h, hints);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", out);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            // 码图内容随表单值变化，禁止缓存，避免不同单据拿到同一张码图
            headers.setCacheControl(CacheControl.noStore().getHeaderValue());
            return new ResponseEntity<>(out.toByteArray(), headers, org.springframework.http.HttpStatus.OK);
        } catch (WriterException e) {
            // CODE_128 不支持中文等非 ASCII 内容：对齐 E9 的 illegalBarCode 回退语义
            return badRequest("条形码内容含非 ASCII 字符，请改用二维码或英文内容");
        } catch (IOException e) {
            return badRequest("码图输出失败：" + e.getMessage());
        }
    }

    private static int clamp(int value, int fallback) {
        if (value <= 0) {
            return fallback;
        }
        return Math.min(value, MAX_SIDE);
    }

    private static ResponseEntity<byte[]> badRequest(String message) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return new ResponseEntity<>(
            message.getBytes(StandardCharsets.UTF_8), headers, org.springframework.http.HttpStatus.BAD_REQUEST
        );
    }
}
