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
import org.springblade.core.launch.constant.TokenConstant;
import org.springblade.core.secure.utils.SecureUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.mall.dto.PhoneDTO;
import org.springblade.mall.dto.WeChatLoginDTO;
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
    public R<?> wechatLogin(@RequestParam("code") String code,
                          @RequestParam(value = "tenantId", defaultValue = "083914") String tenantId) {
        try {
            // 1. 使用code获取微信session信息（openid和session_key）
            WeChatSessionVO session = weChatService.getSessionByCode(code);
            if (session == null) {
                return R.fail("微信登录失败：获取微信session失败");
            }

            String openId = session.getOpenid();
            log.info("微信登录成功，获取到openId: {}", openId);

            User user = checkThirdPartyUser(openId, tenantId);
            if (user == null || user.getId() == null || user.getId() <= 0) {
                log.info("用户不存在或无效，创建新用户, openId: {}, tenantId: {}", openId, tenantId);
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("nickname", "微信用户");
                user = createThirdPartyUser(openId, "wechat", userInfo, tenantId);
            }

            // 最终验证 user 对象完整性
            if (user == null || user.getId() == null || user.getId() <= 0) {
                throw new RuntimeException("用户对象无效，无法生成Token");
            }

            log.info("准备生成JWT Token, userId: {}, userName: {}, roleId: {}, tenantId: {}, deptId: {}",
                user.getId(), user.getName(), user.getRoleId(), user.getTenantId(), user.getDeptId());

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
     * 检查第三方用户是否已存在（通过OAuth表+用户表双重查询）
     */
    private User checkThirdPartyUser(String openId, String tenantId) {
        // 1. 先通过 OAuth 表查询（原有逻辑）
        R<UserInfo> userInfoResult = userClient.userAuthInfo(createUserOauth(openId, tenantId));
        if (userInfoResult.isSuccess() && userInfoResult.getData() != null) {
            User existingUser = userInfoResult.getData().getUser();
            if (existingUser != null && existingUser.getId() != null && existingUser.getId() > 0) {
                log.info("[OAuth查询] 已存在用户, userId: {}, openId: {}, tenantId: {}", existingUser.getId(), openId, tenantId);
                return existingUser;
            }
        }

        // 2. 【关键修复】OAuth 未绑定时，直接按 account=openId 查询用户表
        //    解决：首次登录时 OAuth 表插入了空记录但没有 userId，导致二次登录也找不到用户
        log.info("[OAuth未绑定] 尝试直接按 account 查询用户表, openId: {}, tenantId: {}", openId, tenantId);
        try {
            R<User> userByAccountResult = userClient.getUserByAccount(tenantId, openId);
            if (userByAccountResult.isSuccess() && userByAccountResult.getData() != null) {
                User accountUser = userByAccountResult.getData();
                if (accountUser != null && accountUser.getId() != null && accountUser.getId() > 0) {
                    log.info("[账户查询] 找到已存在用户, userId: {}, openId: {}, tenantId: {}", accountUser.getId(), openId, tenantId);
                    // 补充绑定 OAuth 记录
                    bindOauthIfMissing(openId, tenantId, accountUser.getId());
                    return accountUser;
                }
            }
        } catch (Exception e) {
            log.warn("[账户查询] 按账户查询用户失败（继续走创建流程）: {}", e.getMessage());
        }

        log.info("用户确实不存在, openId: {}, tenantId: {}", openId, tenantId);
        return null;
    }

    /**
     * 补充绑定 OAuth 记录（如果缺失）
     */
    private void bindOauthIfMissing(String openId, String tenantId, Long userId) {
        try {
            // 先查询是否已有绑定
            R<UserInfo> oauthCheck = userClient.userAuthInfo(createUserOauth(openId, tenantId));
            if (oauthCheck.isSuccess() && oauthCheck.getData() != null) {
                UserInfo oauthInfo = oauthCheck.getData();
                User oauthUser = oauthInfo.getUser();
                // 如果 OAuth 有记录但 userId 为空，更新它
                if (oauthUser != null && (oauthUser.getId() == null || oauthUser.getId() <= 0)) {
                    log.info("检测到 OAuth 记录缺少 userId 绑定，进行补充绑定, userId: {}", userId);
                    UserOauth userOauth = new UserOauth();
                    userOauth.setTenantId(tenantId);
                    userOauth.setUuid(openId);
                    userOauth.setUserId(userId);
                    userOauth.setUsername(openId);
                    userOauth.setSource("wechat");
                    userClient.saveUserOauth(userOauth);
                }
            }
        } catch (Exception e) {
            log.warn("补充 OAuth 绑定失败（不影响登录）: {}", e.getMessage());
        }
    }

    /**
     * 创建第三方平台用户
     */
    private User createThirdPartyUser(String openId, String platform, Map<String, Object> userInfo, String tenantId) {
        // 1. 构建新用户对象
        User user = new User();
        user.setOpenId(openId);
        user.setWxOpenid(openId);  // 【关键】必须设置 wx_openid，这是数据库唯一索引字段
        user.setAccount(openId);
        user.setPassword(DigestUtil.encrypt("random_password"));
        String nickname = userInfo.getOrDefault("nickname", platform + "用户").toString();
        user.setName(nickname);
        user.setRealName(nickname);
        user.setAvatar(userInfo.getOrDefault("avatar", "").toString());
        user.setTenantId(tenantId);
        user.setRoleId("1123598816738675202");
        user.setDeptId("1123598813738675201");
        user.setStatus(1);

        log.info("[创建用户] 准备保存用户, account: {}, wxOpenid: {}, tenantId: {}, name: {}", openId, openId, tenantId, nickname);

        // 2. 调用 Feign 保存用户
        R<User> saveResult;
        try {
            saveResult = userClient.saveUser(user);
        } catch (Exception feignEx) {
            log.error("[创建用户] Feign 调用 saveUser 异常: {}", feignEx.getMessage());
            throw new RuntimeException("远程创建用户失败: " + feignEx.getMessage());
        }

        // 3. 验证保存结果
        if (saveResult == null) {
            throw new RuntimeException("创建用户失败：返回结果为空");
        }
        if (!saveResult.isSuccess()) {
            throw new RuntimeException("创建用户失败: " + (saveResult.getMsg() != null ? saveResult.getMsg() : "未知错误"));
        }
        if (saveResult.getData() == null) {
            throw new RuntimeException("创建用户失败：返回用户数据为空");
        }

        // 4. 验证返回的用户对象完整性
        user = saveResult.getData();
        if (user.getId() == null || user.getId() <= 0) {
            log.error("[创建用户] 返回的 userId 无效: {}, 尝试按 account 重新查询", user.getId());
            // 【关键兜底】如果返回的 id 无效，尝试重新查询
            try {
                R<User> retryResult = userClient.getUserByAccount(tenantId, openId);
                if (retryResult.isSuccess() && retryResult.getData() != null
                    && retryResult.getData().getId() != null && retryResult.getData().getId() > 0) {
                    user = retryResult.getData();
                    log.info("[创建用户] 重新查询成功, userId: {}", user.getId());
                } else {
                    throw new RuntimeException("创建用户后验证失败：无法获取有效的用户ID");
                }
            } catch (Exception retryEx) {
                throw new RuntimeException("创建用户后重试查询失败: " + retryEx.getMessage());
            }
        }

        log.info("[创建用户] 创建/获取用户成功, userId: {}, name: {}, roleId: {}, deptId: {}, tenantId: {}",
            user.getId(), user.getName(), user.getRoleId(), user.getDeptId(), user.getTenantId());

        // 5. 保存 OAuth 绑定记录
        UserOauth userOauth = new UserOauth();
        userOauth.setTenantId(tenantId);
        userOauth.setUuid(openId);
        userOauth.setUserId(user.getId());
        userOauth.setUsername(openId);
        userOauth.setNickname(nickname);
        userOauth.setAvatar(userInfo.getOrDefault("avatar", "").toString());
        userOauth.setSource(platform);
        try {
            R<Boolean> oauthResult = userClient.saveUserOauth(userOauth);
            if (!oauthResult.isSuccess() || !oauthResult.getData()) {
                log.warn("[OAuth绑定] 保存OAuth记录失败，但不影响登录流程");
            } else {
                log.info("[OAuth绑定] OAuth记录保存成功, userId: {}", user.getId());
            }
        } catch (Exception oauthEx) {
            log.warn("[OAuth绑定] 保存OAuth异常（不影响登录）: {}", oauthEx.getMessage());
        }

        return user;
    }

    private UserOauth createUserOauth(String openId, String tenantId) {
        UserOauth userOauth = new UserOauth();
        userOauth.setTenantId(tenantId);
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

            // 【诊断日志】输出完整的 token 信息用于排查
            log.info("===== /auth/me 诊断开始 =====");
            log.info("[auth/me] userId from SecureUtil: {}", userId);
            try {
                String tokenHeader = org.springblade.core.tool.utils.WebUtil.getRequest().getHeader("blade-auth");
                log.info("[auth/me] blade-auth header: {}", tokenHeader != null && tokenHeader.length() > 20 ? tokenHeader.substring(0, 20) + "..." : tokenHeader);
            } catch (Exception headerEx) {
                log.warn("[auth/me] 无法读取请求头: {}", headerEx.getMessage());
            }

            if (userId == null || userId == -1) {
                // 【关键诊断】userId 为 -1 的详细原因
                log.warn("===== /auth/me 鉴权失败 =====");
                log.warn("[auth失败] userId={}, 原因: JWT中的user_id为空或-1", userId);
                log.warn("[auth失败] 这意味着 login 接口生成的JWT中 user_id 就是无效值");
                log.warn("[auth失败] 请检查 /wechat/login 返回的 user.id 是否为有效的正整数");
                log.warn("================================");
                return R.fail("未登录");
            }
            log.info("[auth/me] 鉴权成功, userId: {}", userId);

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
            result.put("nickname", user.getNickname() != null ? user.getNickname() : user.getName());
            result.put("avatar", user.getAvatar());
            result.put("phone", user.getPhone());
            result.put("mobile", user.getPhone());
            result.put("email", user.getEmail());
            result.put("gender", user.getGender() != null ? user.getGender() : user.getSex());
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
            log.info("===== /auth/me PUT 开始 =====");
            log.info("[updateUserInfo] Request - nickname: {}, avatar: {}, gender: {}, phone: {}, email: {}",
                request.getNickname(),
                request.getAvatar() != null && request.getAvatar().length() > 50 ? request.getAvatar().substring(0, 50) + "..." : request.getAvatar(),
                request.getGender(),
                request.getPhone(),
                request.getEmail());

            // 获取当前登录用户ID
            Long userId = SecureUtil.getUserId();
            if (userId == null) {
                log.warn("[updateUserInfo] 用户未登录");
                return R.fail("未登录");
            }
            log.info("[updateUserInfo] userId: {}", userId);

            // 查询用户信息
            R<UserInfo> userInfoResult = userClient.userInfo(userId);
            if (!userInfoResult.isSuccess() || userInfoResult.getData() == null) {
                log.warn("[updateUserInfo] 用户不存在, userId: {}", userId);
                return R.fail("用户不存在");
            }

            User user = userInfoResult.getData().getUser();
            log.info("[updateUserInfo] Original user - avatar: {}, name: {}, sex: {}, phone: {}",
                user.getAvatar(),
                user.getName(),
                user.getSex(),
                user.getPhone());

            // 更新用户信息
            if (request.getNickname() != null && !request.getNickname().trim().isEmpty()) {
                user.setName(request.getNickname());
                user.setNickname(request.getNickname());
            }
            if (request.getAvatar() != null) {
                // 如果avatar包含完整URL，只保留相对路径部分
                String avatarPath = request.getAvatar();
                if (avatarPath.startsWith("http://") || avatarPath.startsWith("https://")) {
                    // 找到 /uploads/ 之后的部分
                    int uploadsIndex = avatarPath.indexOf("/uploads/");
                    if (uploadsIndex > 0) {
                        avatarPath = avatarPath.substring(uploadsIndex);
                    }
                }
                user.setAvatar(avatarPath);
            }
            if (request.getGender() != null) {
                user.setGender(request.getGender());
                user.setSex(request.getGender());
            }
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                user.setPhone(request.getPhone());
            }
            if (request.getEmail() != null) {
                user.setEmail(request.getEmail());
            }

            log.info("[updateUserInfo] Updating user - avatar: {}, name: {}, gender: {}, sex: {}, phone: {}",
                user.getAvatar(),
                user.getName(),
                user.getGender(),
                user.getSex(),
                user.getPhone());

            // 保存用户信息更新
            R<User> saveResult = userClient.saveUser(user);
            if (!saveResult.isSuccess()) {
                log.error("[updateUserInfo] 保存用户信息失败: {}", saveResult.getMsg());
                return R.fail("更新用户信息失败");
            }

            log.info("[updateUserInfo] 用户信息更新成功");
            log.info("===== /auth/me PUT 完成 =====");

            // 返回更新后的用户信息
            return getCurrentUser();
        } catch (Exception e) {
            log.error("[updateUserInfo] 更新用户信息异常: {}", e.getMessage(), e);
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
        String userName = (user.getRealName() != null && !user.getRealName().isEmpty()) ? user.getRealName() : user.getName();
        claims.put(TokenConstant.USER_NAME, userName);
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



