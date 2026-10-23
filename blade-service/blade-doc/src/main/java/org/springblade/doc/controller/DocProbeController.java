package org.springblade.doc.controller;

import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.doc.constant.DocConstant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 骨架探活端点：验证 blade-doc 经网关/Nacos 注册并可鉴权访问。
 *
 * <p>仅用于基础设施阶段（I-1）验证；后续阶段由真实 DocEngine 控制器替换/扩充。</p>
 */
@RestController
@RequestMapping("/doc/probe")
public class DocProbeController {

    @PreAuth(DocConstant.HAS_AUTH)
    @GetMapping("/ping")
    public R<String> ping() {
        return R.data("blade-doc ok");
    }
}
