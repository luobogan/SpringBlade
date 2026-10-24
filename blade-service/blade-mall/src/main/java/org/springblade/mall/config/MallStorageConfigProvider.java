package org.springblade.mall.config;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.boot.file.spi.StorageConfigProvider;
import org.springblade.core.oss.OssTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 商城附件存储配置来源：读取 blade_oss 启用配置
 *
 * <p>让 blade-mall 的上传/下载与 blade-resource 保持一致，按页面维护的「启用」存储配置
 * （压缩开关 / 加密开关 / 图片目录 / 文件目录 / 存储类型）走，启用/停用切换即时生效。</p>
 *
 * <p>注册为 Spring 组件后，自动配置的默认实现（读配置中心 blade.file.storage.*）按
 * {@code @ConditionalOnMissingBean} 自动让位。本地存储直接落盘；第三方存储因商城侧未装配
 * OssTemplate，{@link org.springblade.core.boot.file.service.FileStorageService} 会自动降级为本地。</p>
 *
 * @author Blade
 */
@Slf4j
@Component
public class MallStorageConfigProvider implements StorageConfigProvider {

	private static final String CATEGORY_LOCAL = "local";
	private static final String SQL = "SELECT category, image_dir, file_dir, backup_dir, backup_cycle, compress_enable, encrypt_enable"
		+ " FROM blade_oss WHERE status = 1 AND is_deleted = 0 ORDER BY id DESC LIMIT 1";

	private final JdbcTemplate jdbcTemplate;

	public MallStorageConfigProvider(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
	 * 当前「启用」的配置（即时读取，保证启用/停用切换立即生效）
	 */
	private Map<String, Object> active() {
		try {
			return jdbcTemplate.queryForMap(SQL);
		} catch (Exception e) {
			log.debug("未读取到启用的存储配置，回退配置中心: {}", e.getMessage());
			return null;
		}
	}

	@Override
	public FileStorageType defaultType() {
		Map<String, Object> oss = active();
		String category = oss == null ? null : str(oss.get("category"));
		if (category == null) {
			return null;
		}
		return CATEGORY_LOCAL.equals(category) ? FileStorageType.LOCAL : FileStorageType.OSS;
	}

	@Override
	public String imageDir() {
		Map<String, Object> oss = active();
		return isLocal(oss) ? str(oss.get("image_dir")) : null;
	}

	@Override
	public String fileDir() {
		Map<String, Object> oss = active();
		return isLocal(oss) ? str(oss.get("file_dir")) : null;
	}

	@Override
	public String backupDir() {
		Map<String, Object> oss = active();
		return isLocal(oss) ? str(oss.get("backup_dir")) : null;
	}

	@Override
	public Integer backupCycle() {
		Map<String, Object> oss = active();
		return isLocal(oss) ? integer(oss.get("backup_cycle")) : null;
	}

	@Override
	public Boolean zipEnabled() {
		Map<String, Object> oss = active();
		return isLocal(oss) ? toBool(integer(oss.get("compress_enable"))) : null;
	}

	@Override
	public Boolean encryptEnabled() {
		Map<String, Object> oss = active();
		return isLocal(oss) ? toBool(integer(oss.get("encrypt_enable"))) : null;
	}

	@Override
	public OssTemplate ossTemplate() {
		// 商城侧未装配第三方存储模板；若启用配置为第三方，FileStorageService 会自动降级为本地
		return null;
	}

	private static boolean isLocal(Map<String, Object> oss) {
		return oss != null && CATEGORY_LOCAL.equals(str(oss.get("category")));
	}

	private static Boolean toBool(Integer value) {
		return value == null ? null : value == 1;
	}

	private static String str(Object value) {
		return value == null ? null : value.toString();
	}

	private static Integer integer(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		try {
			return Integer.valueOf(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
