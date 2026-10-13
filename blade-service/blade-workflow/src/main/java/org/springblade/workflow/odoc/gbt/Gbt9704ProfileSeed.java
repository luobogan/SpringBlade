package org.springblade.workflow.odoc.gbt;

import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.entity.WfOdocGbtProfile;
import org.springblade.workflow.odoc.mapper.WfOdocGbtProfileMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * GB/T 9704 版式规则库种子（P0 前置项）
 *
 * <p>服务启动时若 {@code wf_odoc_gbt_profile} 为空，则按 {@link Gbt9704BuiltinProfiles#seedProfiles()}
 * 写入 15 文种基准版式（standard/command/minutes 三套）。幂等：仅当表为空时插入；
 * 后续可在管理页按机关/文种微调，不会覆盖已有数据。</p>
 */
@Slf4j
@Component
@Order(100)
public class Gbt9704ProfileSeed implements CommandLineRunner {

    private final WfOdocGbtProfileMapper mapper;

    @Value("${blade.workflow.odoc.gbt.seed-enabled:true}")
    private boolean seedEnabled;

    public Gbt9704ProfileSeed(WfOdocGbtProfileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        Long count = mapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }
        Map<String, Gbt9704Profile> seeds = Gbt9704BuiltinProfiles.seedProfiles();
        int n = 0;
        for (Gbt9704DocType docType : Gbt9704DocType.values()) {
            Gbt9704Profile profile = seeds.get(docType.getCode());
            if (profile == null) {
                continue;
            }
            WfOdocGbtProfile entity = new WfOdocGbtProfile();
            entity.setProfileName(docType.getName());
            entity.setOrgScope(null);
            entity.setStandardVer(profile.getStandardVer());
            entity.setProfileJson(profile.toJson());
            entity.setEnabled(1);
            entity.setStatus(1);
            mapper.insert(entity);
            n++;
        }
        if (n > 0) {
            log.info("[odoc] GB/T 9704 版式规则库已初始化 {} 条文种版式", n);
        }
    }
}
