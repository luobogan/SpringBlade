package org.springblade.mall.controller;

import org.springblade.mall.dto.BrandDTO;
import org.springblade.mall.service.BrandService;
import org.springblade.mall.vo.BrandVO;
import org.springblade.core.tool.api.R;
import org.springblade.core.boot.ctrl.BladeController;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springblade.core.launch.constant.AppConstant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 品牌控制器
 */
@RestController
@RequestMapping(AppConstant.APPLICATION_MALL_NAME + "/brands")
public class BrandController extends BladeController {

    @Autowired
    private BrandService brandService;

    /**
     * 创建品牌
     * @param brandDTO 品牌信息
     * @param file 品牌logo图片
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<R<BrandVO>> createBrand(@Valid BrandDTO brandDTO, @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            // 处理文件上传
            if (file != null && !file.isEmpty()) {
                // 检查文件类型
                String contentType = file.getContentType();
                if (!contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
                }
                
                // 确保上传目录存在
                String UPLOAD_DIR = System.getProperty("user.dir") + System.getProperty("file.separator") + "uploads";
                java.io.File uploadDir = new java.io.File(UPLOAD_DIR);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                
                // 生成唯一文件名
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
                String fileName = java.util.UUID.randomUUID().toString() + extension;
                
                // 保存文件
                java.io.File dest = new java.io.File(uploadDir, fileName);
                file.transferTo(dest);
                
                // 设置logo路径
                String fileUrl = "/uploads/" + fileName;
                brandDTO.setLogo(fileUrl);
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
            // 处理文件上传
            if (file != null && !file.isEmpty()) {
                // 检查文件类型
                String contentType = file.getContentType();
                if (!contentType.startsWith("image/")) {
                    return ResponseEntity.badRequest().body(R.fail("只能上传图片文件"));
                }
                
                // 确保上传目录存在
                String UPLOAD_DIR = System.getProperty("user.dir") + System.getProperty("file.separator") + "uploads";
                java.io.File uploadDir = new java.io.File(UPLOAD_DIR);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                
                // 生成唯一文件名
                String originalFilename = file.getOriginalFilename();
                String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
                String fileName = java.util.UUID.randomUUID().toString() + extension;
                
                // 保存文件
                java.io.File dest = new java.io.File(uploadDir, fileName);
                file.transferTo(dest);
                
                // 设置logo路径
                String fileUrl = "/uploads/" + fileName;
                brandDTO.setLogo(fileUrl);
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
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            System.out.println("=== 获取品牌列表 ===");
            System.out.println("current: " + current);
            System.out.println("pageSize: " + pageSize);
            
            List<BrandVO> brands = brandService.getAllBrands();
            
            // 模拟分页
            int start = (current - 1) * pageSize;
            int end = Math.min(start + pageSize, brands.size());
            List<BrandVO> pageBrands = brands.subList(start, end);
            
            Map<String, Object> response = new HashMap<>();
            response.put("list", pageBrands);
            response.put("total", brands.size());
            response.put("current", current);
            response.put("pageSize", pageSize);

            System.out.println("=== 返回数据 ===");
            System.out.println("list: " + pageBrands.size());
            System.out.println("total: " + brands.size());
            System.out.println("current: " + current);
            System.out.println("pageSize: " + pageSize);

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
}


