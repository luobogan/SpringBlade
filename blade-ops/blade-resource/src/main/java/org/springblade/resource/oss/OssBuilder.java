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
package org.springblade.resource.oss;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import io.minio.MinioClient;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.oss.AliossTemplate;
import org.springblade.core.oss.MinioTemplate;
import org.springblade.core.oss.OssTemplate;
import org.springblade.core.oss.QiniuTemplate;
import org.springblade.core.oss.props.OssProperties;
import org.springblade.core.oss.rule.OssRule;
import org.springblade.resource.entity.Oss;
import org.springblade.resource.service.IOssService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OSS 模板构建器
 *
 * <p>读取 blade_oss 中「启用」的配置，动态构建对应存储类型的 OssTemplate 并缓存；
 * 配置变更（新增/修改/删除/启用切换）后由 OssController 调 {@link #clearCache()} 失效缓存。
 * 未启用任何配置或配置类型暂不支持构建时返回空，由调用方回退到 Nacos oss.* 静态配置。</p>
 *
 * <p>当前支持构建：qiniu（七牛云）、alioss（阿里云）、minio（MinIO）；
 * tencent/huawei/amazon/local/rustfs 暂不支持自动构建（页面可维护，管理端点回退静态配置）。</p>
 *
 * @author Chill
 */
@Slf4j
@Component
@AllArgsConstructor
public class OssBuilder {

	private final IOssService ossService;

	private final OssRule ossRule;

	private final Map<Long, OssTemplate> templateCache = new ConcurrentHashMap<>();

	/**
	 * 获取「启用」配置对应的模板
	 *
	 * @return 启用配置构建的模板；无启用配置/构建失败时返回 empty
	 */
	public Optional<OssTemplate> getActiveTemplate() {
		Oss oss = ossService.getOne(
			Wrappers.<Oss>lambdaQuery()
				.eq(Oss::getStatus, 1)
				.last("limit 1")
		);
		if (oss == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(templateCache.computeIfAbsent(oss.getId(), id -> build(oss)));
	}

	/**
	 * 清空模板缓存（配置变更后调用）
	 */
	public void clearCache() {
		templateCache.clear();
	}

	/**
	 * 按分类构建模板
	 */
	private OssTemplate build(Oss oss) {
		OssProperties props = toProps(oss);
		try {
			switch (oss.getCategory() == null ? "" : oss.getCategory()) {
				case "qiniu":
					return buildQiniu(props);
				case "alioss":
					return buildAliyun(props);
				case "minio":
					return buildMinio(props);
				default:
					log.warn("OssBuilder: 分类[{}]暂不支持动态构建模板，回退静态配置", oss.getCategory());
					return null;
			}
		} catch (Exception e) {
			log.error(String.format("OssBuilder: 构建[%s/%s]模板失败", oss.getCategory(), oss.getOssCode()), e);
			return null;
		}
	}

	private OssProperties toProps(Oss oss) {
		OssProperties props = new OssProperties();
		props.setName(oss.getCategory());
		props.setEndpoint(oss.getEndpoint());
		props.setAccessKey(oss.getAccessKey());
		props.setSecretKey(oss.getSecretKey());
		props.setBucketName(oss.getBucketName());
		props.setRegion(oss.getRegion());
		return props;
	}

	private OssTemplate buildQiniu(OssProperties props) {
		Auth auth = Auth.create(props.getAccessKey(), props.getSecretKey());
		com.qiniu.storage.Configuration cfg = com.qiniu.storage.Configuration.create(Region.autoRegion());
		return new QiniuTemplate(auth, new UploadManager(cfg), new BucketManager(auth, cfg), props, ossRule);
	}

	private OssTemplate buildAliyun(OssProperties props) {
		OSSClient client = (OSSClient) new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKey(), props.getSecretKey());
		return new AliossTemplate(client, props, ossRule);
	}

	@SneakyThrows
	private OssTemplate buildMinio(OssProperties props) {
		MinioClient client = MinioClient.builder()
			.endpoint(props.getEndpoint())
			.credentials(props.getAccessKey(), props.getSecretKey())
			.build();
		return new MinioTemplate(client, ossRule, props);
	}

}
