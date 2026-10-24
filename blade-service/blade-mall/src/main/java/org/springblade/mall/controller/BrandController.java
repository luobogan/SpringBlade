package org.springblade.mall.controller;

import org.springblade.mall.dto.BrandDTO;
import org.springblade.mall.service.BrandService;
import org.springblade.mall.vo.BrandVO;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springblade.core.secure.utils.SecureUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 品牌控制器
 */
@RestController
@RequestMapping("/brands")
public class BrandController extends BladeController {

    private static final Logger log = LoggerFactory.getLogger(BrandController.class);

    @Autowired
    private BrandService brandService;

    @Autowired
    private IFileStorageService fileStorageService;

    /**
     * 创建品牌
     * @param brandDTO 品牌信息
     * @param file 品牌logo图片
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<R<BrandVO>> createBrand(@Valid BrandDTO brandDTO, @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            // 处理文件上传（统一走通用文件服务 IFileStorageService：压缩/加密/分片/元数据）
            if (file != null && !file.isEmpty()) {
                // 检查文件类型
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
                }
                FileUploadOptions options = new FileUploadOptions()
                    .dir("brand")
                    .comefrom("mall")
                    .tenantId(tenantId());
                FileUploadResult result = fileStorageService.upload(file, options);
                brandDTO.setLogo("/api/blade-mall/file/download/" + result.getId());
            }

            BrandVO brandVO = brandService.createBrand(brandDTO);
            return ResponseEntity.ok(R.data(brandVO, "品牌创建成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }

    /**
     * 更新品牌
     * @param id 品牌ID
     * @param brandDTO 品牌信息
     * @param file 品牌logo图片
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<R<BrandVO>> updateBrand(@PathVariable Long id, @Valid BrandDTO brandDTO, @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            // 处理文件上传（统一走通用文件服务 IFileStorageService：压缩/加密/分片/元数据）
            if (file != null && !file.isEmpty()) {
                // 检查文件类型
                String contentType = file.getContentType();
                if (contentType == null || !contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
                }
                FileUploadOptions options = new FileUploadOptions()
                    .dir("brand")
                    .comefrom("mall")
                    .tenantId(tenantId());
                FileUploadResult result = fileStorageService.upload(file, options);
                brandDTO.setLogo("/api/blade-mall/file/download/" + result.getId());
            }

            BrandVO brandVO = brandService.updateBrand(id, brandDTO);
            return ResponseEntity.ok(R.data(brandVO, "品牌更新成功"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }

    /**
     * 删除品牌
     * @param id 品牌ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<R<Void>> deleteBrand(@PathVariable Long id) {
        try {
            brandService.deleteBrand(id);
            return ResponseEntity.ok(R.success("品牌删除成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }

    /**
     * 获取品牌详情
     * @param id 品牌ID
     * @return 品牌详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<R<BrandVO>> getBrandById(@PathVariable Long id) {
        try {
            BrandVO brandVO = brandService.getBrandById(id);
            return ResponseEntity.ok(R.data(brandVO));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }

    /**
     * 获取所有品牌（分页）
     * @param current 当前页码
     * @param pageSize 每页大小
     * @return 品牌列表
     */
    @GetMapping
    public ResponseEntity<R<Map<String, Object>>> getAllBrands(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String tenantId) {
        try {
            log.debug("=== 获取品牌列表 ===");
            log.debug("current: {}", current);
            log.debug("pageSize: {}", pageSize);
            log.debug("tenantId: {}", tenantId);

            List<BrandVO> brands = brandService.getAllBrands(tenantId);

            // 模拟分页
            int start = (current - 1) * pageSize;
            int end = Math.min(start + pageSize, brands.size());
            List<BrandVO> pageBrands = brands.subList(start, end);

            Map<String, Object> response = new HashMap<>();
            response.put("list", pageBrands);
            response.put("total", brands.size());
            response.put("current", current);
            response.put("pageSize", pageSize);

            log.debug("=== 返回数据 ===");
            log.debug("list: {}", pageBrands.size());
            log.debug("total: {}", brands.size());
            log.debug("current: {}", current);
            log.debug("pageSize: {}", pageSize);

            return ResponseEntity.ok(R.data(response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }

    /**
     * 获取活跃品牌
     * @return 活跃品牌列表
     */
    @GetMapping("/active")
    public ResponseEntity<R<List<BrandVO>>> getActiveBrands() {
        try {
            List<BrandVO> brands = brandService.getActiveBrands();
            return ResponseEntity.ok(R.data(brands));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(R.fail(e.getMessage()));
        }
    }

    /**
     * 当前租户ID（取不到时兜底默认租户）
     */
    private String tenantId() {
        try {
            String tenantId = SecureUtil.getTenantId();
            return (tenantId == null || tenantId.isBlank()) ? "000000" : tenantId;
        } catch (Exception e) {
            return "000000";
        }
    }
}


