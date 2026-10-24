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

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.props.FileStorageProperties;
import org.springblade.resource.entity.Oss;
import org.springblade.resource.service.IOssService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 对象存储配置 → Nacos 同步
 *
 * <p>目的：让「所有模块」的附件上传/下载统一跟随本页维护的存储配置。</p>
 *
 * <p>原理：各业务模块启动器统一监听 {@code blade-{profile}.yaml}（如 blade-dev.yaml）。
 * 本服务在配置的「保存 / 启用 / 删除」之后，把当前生效配置以
 * {@code blade.file.storage.*} 写回该 dataId，各模块收到 Nacos 刷新事件后自动重新绑定
 * {@link FileStorageProperties}，从而无需重启即可统一生效。</p>
 *
 * <p>同步内容：</p>
 * <ul>
 *     <li>本地存储：default-type=LOCAL，并下发 image-dir / file-dir / backup-dir /
 *         backup-cycle / default-need-zip / default-need-encrypt；</li>
 *     <li>第三方存储：default-type=OSS（具体 Template 由各模块 OssBuilder 按分类构建）；</li>
 *     <li>无启用配置：清空同步项，各模块回退本地默认。</li>
 * </ul>
 *
 * <p>注意：同步失败仅记录告警，不阻断业务（页面配置仍落库，且本模块经
 * {@link OssStorageConfigProvider} 直接读库，始终生效）。</p>
 *
 * @author Chill
 */
@Slf4j
@Component
public class OssConfigNacosSyncService {

	private static final String GROUP = "DEFAULT_GROUP";
	private static final String CATEGORY_LOCAL = "local";

	@Value("${spring.cloud.nacos.config.server-addr:127.0.0.1:8848}")
	private String serverAddr;

	@Value("${spring.cloud.nacos.config.namespace:}")
	private String namespace;

	@Value("${spring.cloud.nacos.config.username:nacos}")
	private String username;

	@Value("${spring.cloud.nacos.config.password:nacos}")
	private String password;

	@Value("${spring.profiles.active:dev}")
	private String profile;

	private final IOssService ossService;

	private volatile ConfigService configService;

	public OssConfigNacosSyncService(IOssService ossService) {
		this.ossService = ossService;
	}

	@PostConstruct
	public void init() {
		try {
			Properties props = new Properties();
			props.put(PropertyKeyConst.SERVER_ADDR, serverAddr);
			if (namespace != null && !namespace.isBlank()) {
				props.put(PropertyKeyConst.NAMESPACE, namespace);
			}
			if (username != null && !username.isBlank()) {
				props.put(PropertyKeyConst.USERNAME, username);
				props.put(PropertyKeyConst.PASSWORD, password);
			}
			this.configService = NacosFactory.createConfigService(props);
			log.info("OssConfigNacosSync: 已连接 Nacos {}, 同步目标 dataId={}", serverAddr, dataId());
		} catch (NacosException e) {
			log.warn("OssConfigNacosSync: 连接 Nacos 失败，配置同步将不可用（不影响本模块读取库配置）", e);
		}
	}

	@PreDestroy
	public void destroy() {
		try {
			if (configService != null) {
				configService.shutDown();
			}
		} catch (NacosException e) {
			log.warn("OssConfigNacosSync: 关闭 Nacos 连接失败", e);
		}
	}

	/**
	 * 把当前生效配置同步到 Nacos（建议在保存/启用/删除之后调用）
	 */
	public void sync() {
		if (configService == null) {
			log.warn("OssConfigNacosSync: Nacos 不可用，跳过同步");
			return;
		}
		String dataId = dataId();
		try {
			String original = configService.getConfig(dataId, GROUP, 5000);
			if (original == null || original.isBlank()) {
				log.warn("OssConfigNacosSync: 配置[{}]为空，跳过同步", dataId);
				return;
			}
			Yaml yaml = new Yaml();
			Object loaded = yaml.load(original);
			@SuppressWarnings("unchecked")
			Map<String, Object> root = loaded instanceof Map ? (Map<String, Object>) loaded : new LinkedHashMap<>();

			Map<String, Object> blade = child(root, "blade");
			Map<String, Object> file = child(blade, "file");
			@SuppressWarnings("unchecked")
			Map<String, Object> storage = file.get("storage") instanceof Map
				? (Map<String, Object>) file.get("storage") : new LinkedHashMap<>();

			apply(storage);
			file.put("storage", storage);

			String updated = yaml.dump(root);
			boolean ok = configService.publishConfig(dataId, GROUP, updated);
			if (ok) {
				log.info("OssConfigNacosSync: 已同步生效配置到 Nacos[{}]，各模块将自动刷新", dataId);
			} else {
				log.warn("OssConfigNacosSync: 发布配置[{}]返回失败", dataId);
			}
		} catch (Exception e) {
			log.warn("OssConfigNacosSync: 同步配置[{}]失败（不影响本模块）", dataId, e);
		}
	}

	/**
	 * 按当前启用配置写入 blade.file.storage 各属性
	 */
	private void apply(Map<String, Object> storage) {
		Oss oss = ossService.getOne(
			Wrappers.<Oss>lambdaQuery()
				.eq(Oss::getStatus, 1)
				.last("limit 1")
		);
		if (oss == null) {
			// 无启用配置：清空，各模块回退本地默认
			storage.remove("default-type");
			storage.remove("image-dir");
			storage.remove("file-dir");
			storage.remove("backup-dir");
			storage.remove("backup-cycle");
			storage.remove("default-need-zip");
			storage.remove("default-need-encrypt");
			return;
		}
		String category = oss.getCategory() == null ? "" : oss.getCategory();
		if (CATEGORY_LOCAL.equals(category)) {
			storage.put("default-type", "LOCAL");
			putIfNotBlank(storage, "image-dir", oss.getImageDir());
			putIfNotBlank(storage, "file-dir", oss.getFileDir());
			putIfNotBlank(storage, "backup-dir", oss.getBackupDir());
			if (oss.getBackupCycle() != null) {
				storage.put("backup-cycle", oss.getBackupCycle());
			}
			storage.put("default-need-zip", Integer.valueOf(1).equals(oss.getCompressEnable()));
			storage.put("default-need-encrypt", Integer.valueOf(1).equals(oss.getEncryptEnable()));
		} else {
			// 第三方存储：交由各模块的 OssBuilder 按分类构建对应 Template
			storage.put("default-type", "OSS");
			storage.remove("image-dir");
			storage.remove("file-dir");
		}
	}

	private static Map<String, Object> child(Map<String, Object> parent, String key) {
		Object value = parent.get(key);
		if (value instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) value;
			return map;
		}
		Map<String, Object> created = new LinkedHashMap<>();
		parent.put(key, created);
		return created;
	}

	private static void putIfNotBlank(Map<String, Object> map, String key, String value) {
		if (value != null && !value.isBlank()) {
			map.put(key, value);
		} else {
			map.remove(key);
		}
	}

	private String dataId() {
		return "blade-" + (profile == null || profile.isBlank() ? "dev" : profile) + ".yaml";
	}

}
