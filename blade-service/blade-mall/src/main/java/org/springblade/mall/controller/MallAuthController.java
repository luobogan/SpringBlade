/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.mall.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.launch.constant.TokenConstant;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.mall.dto.PhoneDTO;
import org.springblade.mall.service.WeChatService;
import org.springblade.mall.vo.WeChatSessionVO;
import org.springblade.system.user.entity.User;
import org.springblade.system.user.entity.UserInfo;
import org.springblade.system.user.entity.UserOauth;
import org.springblade.system.user.feign.IUserClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 商城认证控制器
 *
 * @author Chill
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "商城认证", description = "商城认证接口")
@RequiredArgsConstructor
public class MallAuthController {

    private static final Logger log = LoggerFactory.getLogger(MallAuthController.class);

    private final IUserClient userClient;
    private final WeChatService weChatService;

    @Value("${blade.token.sign-key:18cfeH2ra31t5TqejCeyDJ5TmnETifQB}")
    private String signKey;

    @Value("${blade.token.expire:86400}")
    private Long tokenExpire;



    /**
     * 获取微信手机号（无需登录）
     */
    @PostMapping("/phone/get")
    @Operation(summary = "获取微信手机号", description = "通过微信授权获取用户手机号")
    public R<?> getPhone(@RequestBody PhoneDTO phoneDTO) {
        try {
            // 调用服务获取手机号
            Map<String, String> phoneInfo = getWeChatPhone(phoneDTO);
            return R.data(phoneInfo);
        } catch (Exception e) {
            return R.fail("获取手机号失败：" + e.getMessage());
        }
    }

    /**
     * 获取微信手机号
     *
     * @param phoneDTO 手机号信息
     * @return 手机号
     */
    private Map<String, String> getWeChatPhone(PhoneDTO phoneDTO) {
        try {
            // 1. 先调用微信接口获取 session
            WeChatSessionVO sessionVO = weChatService.getWeChatSession(phoneDTO.getCode());
            if (sessionVO == null || sessionVO.getOpenid() == null) {
                throw new RuntimeException("微信登录失败");
            }

            // 2. 解密手机号
            String phoneJson = weChatService.decryptWeChatData(
                sessionVO.getSessionKey(),
                phoneDTO.getEncryptedData(),
                phoneDTO.getIv()
            );

            // 3. 解析手机号
            Map<String, String> phoneInfo = new HashMap<>();
            if (phoneJson != null && !phoneJson.isEmpty()) {
                // 解析 JSON 字符串并提取 phoneNumber
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(phoneJson);

                if (jsonNode.has("phoneNumber")) {
                    String phoneNumber = jsonNode.get("phoneNumber").asText();
                    phoneInfo.put("phoneNumber", phoneNumber);
                    log.info("解析到微信手机号：{}", phoneNumber);
                } else {
                    throw new RuntimeException("解密数据中未找到手机号");
                }
            } else {
                throw new RuntimeException("解密失败");
            }

            return phoneInfo;
        } catch (Exception e) {
            throw new RuntimeException("获取手机号失败：" + e.getMessage());
        }
    }

    /**
     * 微信登录
     */
    @PostMapping("/wechat/login")
    @Operation(summary = "微信登录", description = "微信授权登录")
    public R<?> wechatLogin(@RequestParam("code") String code, @RequestParam("tenantId") String tenantId) {
        try {
            // 1. 使用code获取微信session信息（openid和session_key）
            WeChatSessionVO session = weChatService.getSessionByCode(code);
            if (session == null) {
                return R.fail("微信登录失败：获取微信session失败");
            }

            String openId = session.getOpenid();
            log.info("微信登录成功，获取到openId: {}", openId);

            // 2. 检查用户是否存在
            User user = checkThirdPartyUser(openId);
            if (user == null) {
                // 3. 创建新用户
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("nickname", "微信用户");
                userInfo.put("avatar", "");
                user = createThirdPartyUser(openId, "wechat", userInfo, tenantId);
            }

            // 4. 直接生成JWT Token（不依赖请求上下文）
            String token = createJWTToken(user);

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", user);
            result.put("expiresIn", tokenExpire);
            result.put("refreshToken", token);

            return R.data(result);
        } catch (Exception e) {
            System.err.println("微信登录异常: " + e.getMessage());
            e.printStackTrace();
            return R.fail("微信登录失败：" + e.getMessage());
        }
    }



    /**
     * 检查第三方用户是否已存在
     */
    private User checkThirdPartyUser(String openId) {
        R<UserInfo> userInfoResult = userClient.userAuthInfo(createUserOauth(openId));
        if (userInfoResult.isSuccess() && userInfoResult.getData() != null) {
            return userInfoResult.getData().getUser();
        }
        return null;
    }

    /**
     * 创建第三方平台用户
     */
    private User createThirdPartyUser(String openId, String platform, Map<String, Object> userInfo, String tenantId) {
        User user = new User();
        user.setAccount(openId); // 使用openId作为账号
        user.setPassword(DigestUtil.encrypt("random_password")); // 随机密码
        user.setName(userInfo.getOrDefault("nickname", platform + "用户").toString());
        user.setAvatar(userInfo.getOrDefault("avatar", "").toString());
        user.setTenantId(tenantId); // 使用传入的tenantId
        user.setRoleId("1123598816738675202"); // 默认角色ID（需要提前创建）
        user.setStatus(1);

        // 创建用户
        R<Boolean> saveResult = userClient.saveUser(user);
        if (!saveResult.isSuccess() || !saveResult.getData()) {
            throw new RuntimeException("创建用户失败");
        }

        // 同时在blade_user_oauth表中创建记录
        UserOauth userOauth = new UserOauth();
        userOauth.setTenantId(tenantId); // 使用传入的tenantId
        userOauth.setUuid(openId);
        userOauth.setUserId(user.getId());
        userOauth.setUsername(openId);
        userOauth.setNickname(userInfo.getOrDefault("nickname", "").toString());
        userOauth.setAvatar(userInfo.getOrDefault("avatar", "").toString());
        userOauth.setSource(platform);
        userClient.userAuthInfo(userOauth);

        return user;
    }

    private UserOauth createUserOauth(String openId) {
        UserOauth userOauth = new UserOauth();
        userOauth.setUuid(openId);
        userOauth.setUsername(openId);
        return userOauth;
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public R<?> getCurrentUser() {
        try {
            // 获取当前登录用户 ID
            Long userId = SecureUtil.getUserId();
            log.info("getCurrentUser - userId from token: {}", userId);
            if (userId == null || userId == -1) {
                log.warn("getCurrentUser - userId is null or -1");
                return R.fail("未登录");
            }

            // 查询用户信息
            R<UserInfo> userInfoResult = userClient.userInfo(userId);
            if (!userInfoResult.isSuccess() || userInfoResult.getData() == null) {
                return R.fail("用户不存在");
            }

            User user = userInfoResult.getData().getUser();
            log.info("getCurrentUser - user from db: {}", user != null ? user.getId() : "null");

            // 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("name", user.getName());
            result.put("avatar", user.getAvatar());
            result.put("phone", user.getPhone());
            result.put("email", user.getEmail());
            result.put("gender", user.getSex());
            result.put("tenantId", user.getTenantId());
            result.put("oauthId", userInfoResult.getData().getOauthId());

            return R.data(result);
        } catch (Exception e) {
            return R.fail("获取用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户信息请求参数
     */
    public static class UpdateUserRequest {
        private String nickname;
        private String avatar;
        private Integer gender;
        private String phone;
        private String email;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public Integer getGender() {
            return gender;
        }

        public void setGender(Integer gender) {
            this.gender = gender;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/me")
    @Operation(summary = "更新用户信息", description = "更新当前登录用户的信息")
    public R<?> updateUserInfo(@RequestBody UpdateUserRequest request) {
        try {
            // 获取当前登录用户ID
            Long userId = SecureUtil.getUserId();
            if (userId == null) {
                return R.fail("未登录");
            }

            // 查询用户信息
            R<UserInfo> userInfoResult = userClient.userInfo(userId);
            if (!userInfoResult.isSuccess() || userInfoResult.getData() == null) {
                return R.fail("用户不存在");
            }

            User user = userInfoResult.getData().getUser();

            // 更新用户信息
            if (request.getNickname() != null) {
                user.setName(request.getNickname());
            }
            if (request.getAvatar() != null) {
                user.setAvatar(request.getAvatar());
            }
            if (request.getPhone() != null) {
                user.setPhone(request.getPhone());
            }
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }

            // 保存用户信息更新
            R<Boolean> saveResult = userClient.saveUser(user);
            if (!saveResult.isSuccess()) {
                return R.fail("更新用户信息失败");
            }

            // 返回更新后的用户信息
            return getCurrentUser();
        } catch (Exception e) {
            return R.fail("更新用户信息失败：" + e.getMessage());
        }
    }

    /**
     * 创建 JWT Token（不依赖请求上下文）
     */
    private String createJWTToken(User user) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        // 生成签名密钥（与 SecureUtil 一致的方式）
        // 先将 signKey 进行 Base64 编码成字符串，然后再解码成 byte 数组
        String base64Security = Base64.getEncoder().encodeToString(signKey.getBytes(StandardCharsets.UTF_8));
        SecretKey signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Security));

        // 构建 JWT 参数
        Map<String, Object> claims = new HashMap<>();
        claims.put(TokenConstant.TOKEN_TYPE, TokenConstant.ACCESS_TOKEN);
        claims.put(TokenConstant.TENANT_ID, user.getTenantId());
        claims.put(TokenConstant.USER_ID, String.valueOf(user.getId()));
        claims.put(TokenConstant.ROLE_ID, user.getRoleId());
        claims.put(TokenConstant.DEPT_ID, user.getDeptId());
        claims.put(TokenConstant.ACCOUNT, user.getAccount());
        claims.put(TokenConstant.USER_NAME, user.getRealName());
        claims.put(TokenConstant.CLIENT_ID, "sword");

        // 计算过期时间
        Date exp = new Date(nowMillis + tokenExpire * 1000);

        // 生成 JWT Token
        return Jwts.builder()
            .header().add("typ", "JWT").and()
            .issuer("blade")
            .audience().add("blade").and()
            .claims(claims)
            .issuedAt(now)
            .expiration(exp)
            .notBefore(now)
            .signWith(signingKey)
            .compact();
    }
}



