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

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.boot.file.spi.StorageConfigProvider;
import org.springblade.core.oss.OssTemplate;
import org.springblade.resource.entity.Oss;
import org.springblade.resource.service.IOssService;
import org.springframework.stereotype.Component;

/**
 * 基于 blade_oss 配置表的存储配置来源
 *
 * <p>让「统一上传/下载组件」按存储类型适配到页面维护的配置：</p>
 * <ul>
 *     <li>启用配置为「本地存储」：使用其 图片目录 / 文件目录 / 备份目录 / 备份周期
 *         以及 压缩开关 / 加密开关，文件落本地磁盘；</li>
 *     <li>启用配置为「第三方存储」（七牛 / 阿里云 / MinIO）：使用 {@link OssBuilder}
 *         构建的该类型对应 {@link OssTemplate} 上传与取流；</li>
 *     <li>未启用任何配置：全部返回 null，由组件回退到配置中心 blade.file.storage.*。</li>
 * </ul>
 *
 * <p>注册为 {@code @Component} 后，自动配置的默认实现（读配置中心）自动让位。</p>
 *
 * @author Chill
 */
@Slf4j
@Component
@AllArgsConstructor
public class OssStorageConfigProvider implements StorageConfigProvider {

	private static final String CATEGORY_LOCAL = "local";

	private final IOssService ossService;

	private final OssBuilder ossBuilder;

	/**
	 * 当前「启用」的配置（即时读取，保证启用/停用切换立即生效）
	 */
	private Oss active() {
		return ossService.getOne(
			Wrappers.<Oss>lambdaQuery()
				.eq(Oss::getStatus, 1)
				.last("limit 1")
		);
	}

	@Override
	public FileStorageType defaultType() {
		Oss oss = active();
		if (oss == null || oss.getCategory() == null) {
			return null;
		}
		return CATEGORY_LOCAL.equals(oss.getCategory()) ? FileStorageType.LOCAL : FileStorageType.OSS;
	}

	@Override
	public String imageDir() {
		Oss oss = active();
		return isLocal(oss) ? oss.getImageDir() : null;
	}

	@Override
	public String fileDir() {
		Oss oss = active();
		return isLocal(oss) ? oss.getFileDir() : null;
	}

	@Override
	public String backupDir() {
		Oss oss = active();
		return isLocal(oss) ? oss.getBackupDir() : null;
	}

	@Override
	public Integer backupCycle() {
		Oss oss = active();
		return isLocal(oss) ? oss.getBackupCycle() : null;
	}

	@Override
	public Boolean zipEnabled() {
		Oss oss = active();
		return isLocal(oss) ? toBool(oss.getCompressEnable()) : null;
	}

	@Override
	public Boolean encryptEnabled() {
		Oss oss = active();
		return isLocal(oss) ? toBool(oss.getEncryptEnable()) : null;
	}

	@Override
	public OssTemplate ossTemplate() {
		// 第三方存储：由 OssBuilder 按分类构建对应模板；本地存储返回 null（走本地落盘）
		return ossBuilder.getActiveTemplate().orElse(null);
	}

	private static boolean isLocal(Oss oss) {
		return oss != null && CATEGORY_LOCAL.equals(oss.getCategory());
	}

	private static Boolean toBool(Integer value) {
		return value == null ? null : value == 1;
	}

}
