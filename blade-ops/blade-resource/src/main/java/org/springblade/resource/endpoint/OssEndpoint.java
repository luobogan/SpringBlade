/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.resource.endpoint;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springblade.core.oss.OssTemplate;
import org.springblade.core.oss.QiniuTemplate;
import org.springblade.core.oss.model.OssFile;
import org.springblade.core.swagger.annotation.ApiOrder;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.resource.oss.OssBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 对象存储端点
 *
 * <p>文件上传已统一到通用上传组件（blade-file）：支持 ZIP 打包、AES 加密、
 * 本地/OSS 双通道，落 imagefile 元数据表；此处保留对象存储管理能力
 * （建桶、删桶、拷贝、查询、外链、删除）。</p>
 *
 * <p>QiniuTemplate 为可选依赖：仅当 Nacos 配置了 oss.name=qiniu 时才创建；
 * 本地存储场景下不依赖它，因此设为 required=false 以免阻断启动。</p>
 *
 * @author Chill
 */
@RestController
@RequestMapping("/oss/endpoint")
@ApiOrder
@Tag(name = "对象存储端点", description = "对象存储端点")
public class OssEndpoint {

	@Autowired(required = false)
	private QiniuTemplate qiniuTemplate;

	@Autowired
	private IFileStorageService fileStorageService;

	@Autowired
	private OssBuilder ossBuilder;

	/**
	 * 管理操作使用的模板：优先 blade_oss「启用」配置动态构建，未启用/不支持时回退 Nacos oss.* 静态配置
	 */
	private OssTemplate template() {
		return ossBuilder.getActiveTemplate().orElse(qiniuTemplate);
	}

	/**
	 * 创建存储桶
	 */
	@SneakyThrows
	@PostMapping("/make-bucket")
	public R makeBucket(@RequestParam String bucketName) {
		template().makeBucket(bucketName);
		return R.success("创建成功");
	}

	/**
	 * 删除存储桶
	 */
	@SneakyThrows
	@PostMapping("/remove-bucket")
	public R removeBucket(@RequestParam String bucketName) {
		template().removeBucket(bucketName);
		return R.success("删除成功");
	}

	/**
	 * 拷贝文件
	 */
	@SneakyThrows
	@PostMapping("/copy-file")
	public R copyFile(@RequestParam String fileName, @RequestParam String destBucketName, String destFileName) {
		template().copyFile(fileName, destBucketName, destFileName);
		return R.success("操作成功");
	}

	/**
	 * 获取文件信息
	 */
	@SneakyThrows
	@GetMapping("/stat-file")
	public R<OssFile> statFile(@RequestParam String fileName) {
		return R.data(template().statFile(fileName));
	}

	/**
	 * 获取文件相对路径
	 */
	@SneakyThrows
	@GetMapping("/file-path")
	public R<String> filePath(@RequestParam String fileName) {
		return R.data(template().filePath(fileName));
	}


	/**
	 * 获取文件外链
	 */
	@SneakyThrows
	@GetMapping("/file-link")
	public R<String> fileLink(@RequestParam String fileName) {
		return R.data(template().fileLink(fileName));
	}

	/**
	 * 上传文件（统一通用上传组件）
	 *
	 * @param file      文件
	 * @param needZip   是否打包为 ZIP
	 * @param needEncrypt 是否 AES 加密
	 * @param storage   存储方式 LOCAL / OSS
	 */
	@PostMapping("/put-file")
	public R<FileUploadResult> putFile(@RequestParam MultipartFile file,
									   @RequestParam(value = "needZip", required = false) Boolean needZip,
									   @RequestParam(value = "needEncrypt", required = false) Boolean needEncrypt,
									   @RequestParam(value = "storage", required = false) FileStorageType storage) {
		FileUploadOptions options = new FileUploadOptions()
			.dir("resource")
			.needZip(needZip)
			.needEncrypt(needEncrypt)
			.storage(storage)
			.comefrom("oss-endpoint");
		return R.data(fileStorageService.upload(file, options));
	}

	/**
	 * 上传文件（统一通用上传组件，指定对象名）
	 */
	@PostMapping("/put-file-by-name")
	public R<FileUploadResult> putFile(@RequestParam String fileName, @RequestParam MultipartFile file,
									   @RequestParam(value = "needZip", required = false) Boolean needZip,
									   @RequestParam(value = "needEncrypt", required = false) Boolean needEncrypt,
									   @RequestParam(value = "storage", required = false) FileStorageType storage) {
		FileUploadOptions options = new FileUploadOptions()
			.dir("resource")
			.needZip(needZip)
			.needEncrypt(needEncrypt)
			.storage(storage)
			.comefrom("oss-endpoint");
		return R.data(fileStorageService.upload(file, options));
	}

	/**
	 * 删除文件
	 */
	@SneakyThrows
	@PostMapping("/remove-file")
	public R removeFile(@RequestParam String fileName) {
		template().removeFile(fileName);
		return R.success("操作成功");
	}

	/**
	 * 批量删除文件
	 */
	@SneakyThrows
	@PostMapping("/remove-files")
	public R removeFiles(@RequestParam String fileNames) {
		template().removeFiles(Func.toStrList(fileNames));
		return R.success("操作成功");
	}

	/**
	 * 列出存储空间文件（按前缀过滤）
	 */
	@SneakyThrows
	@GetMapping("/list-files")
	public R<List<OssFile>> listFiles(@RequestParam(required = false) String prefix) {
		return R.data(template().listFiles(prefix));
	}

	/**
	 * 列出全部存储桶
	 */
	@SneakyThrows
	@GetMapping("/list-buckets")
	public R<List<String>> listBuckets() {
		return R.data(listBucketNames(template()));
	}

	/**
	 * 当前 OSS 配置概览（供前端对象存储页展示）
	 */
	@SneakyThrows
	@GetMapping("/config")
	public R<java.util.Map<String, Object>> config() {
		OssTemplate t = template();
		java.util.Map<String, Object> map = new java.util.HashMap<>(4);
		map.put("endpoint", t instanceof QiniuTemplate q ? q.getEndpoint() : "");
		map.put("bucket", t instanceof QiniuTemplate q ? q.getCurrentBucketName() : "");
		map.put("available", true);
		return R.data(map);
	}

	/**
	 * 存储桶列举按模板类型分发（仅七牛实现；其余模板返回空列表）
	 */
	private List<String> listBucketNames(OssTemplate t) {
		if (t instanceof QiniuTemplate q) {
			return q.listBuckets();
		}
		return new java.util.ArrayList<>();
	}

}
