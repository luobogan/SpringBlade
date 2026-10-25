package org.springblade.doc.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springblade.doc.entity.DocDetail;

import java.util.Date;
import java.util.List;

/**
 * 文档主表 Mapper。
 *
 * <p>分部隔离经 blade_scope_data 配置的 scopeClass（{@code DocDataScope}）由
 * DataScopeInterceptor 自动追加 sub_company_id IN (...)，业务代码零硬编码（落地规范 §2）。</p>
 */
@Mapper
public interface DocDetailMapper extends BaseMapper<DocDetail> {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM doc_detail WHERE approve_inst_id = #{instId} AND is_deleted = 0 LIMIT 1")
    DocDetail selectByApproveInstId(@Param("instId") Long instId);

    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE doc_detail SET doc_status = #{status} WHERE approve_inst_id = #{instId} AND is_deleted = 0")
    int updateStatusByApproveInstId(@Param("instId") Long instId, @Param("status") int status);

    /**
     * 记录级可见性检索（等价 E9 DocShareUtil.getOnlyHasRight）：
     * 仅返回「我创建的 + 共享给我(人员/部门/角色/分部/所有人) + 我管理的目录下的文档」；
     * allVisible=true（超管/内容管理员）时跳过权限裁剪，返回租户内全部。
     *
     * <p>忽略租户行拦截器，改由 {@code tenant_id = #{tenantId}} 显式隔离（避免拦截器重写复杂子查询出错）。</p>
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>"
        + "SELECT d.* FROM doc_detail d "
        + "WHERE d.is_deleted = 0 AND d.is_history = 0 "
        + "<if test='keyword != null and keyword != \"\"'> AND d.doc_subject LIKE CONCAT('%', #{keyword}, '%') </if>"
        + "<if test='secCategoryId != null'> AND d.sec_category_id = #{secCategoryId} </if>"
        + "<if test='!allVisible'> AND ( d.create_user = #{userId} "
        + "  OR EXISTS (SELECT 1 FROM doc_share s WHERE s.doc_id = d.id AND s.share_level &gt;= 1 AND ( "
        + "    (s.share_type = 1 AND s.target_id = #{userId}) OR s.share_type = 5 "
        + "    OR (s.share_type = 3 AND s.target_id = #{deptId}) "
        + "    <if test='roleIds != null and roleIds.size() &gt; 0'> OR (s.share_type = 4 AND s.target_id IN <foreach collection='roleIds' item='rid' open='(' separator=',' close=')'>#{rid}</foreach>) </if>"
        + "    OR (s.share_type = 2 AND s.target_id = #{divisionId}) )) "
        + "  OR EXISTS (SELECT 1 FROM doc_sec_category c WHERE c.id = d.sec_category_id "
        + "    <if test='roleIds != null and roleIds.size() &gt; 0'> AND c.manage_role_id IN <foreach collection='roleIds' item='rid2' open='(' separator=',' close=')'>#{rid2}</foreach></if>) ) </if>"
        + "ORDER BY d.create_time DESC "
        + "</script>")
    IPage<DocDetail> searchVisible(IPage<DocDetail> page,
                                   @Param("keyword") String keyword,
                                   @Param("secCategoryId") Long secCategoryId,
                                   @Param("allVisible") boolean allVisible,
                                   @Param("userId") Long userId,
                                   @Param("deptId") Long deptId,
                                   @Param("divisionId") Long divisionId,
                                   @Param("roleIds") List<Long> roleIds);

    /**
     * 移入回收站：逻辑删除（is_deleted=1）并记录操作人/操作时间。
     *
     * <p>DocDetail.isDeleted 带 {@code @TableLogic}，MP 的 updateById 会自动追加 {@code is_deleted=0}
     * 条件，无法更新「已逻辑删除」的行，故回收站的标记/恢复/彻底删除统一用自定义 SQL。
     * DataScope 拦截器只处理 SELECT（见 DataScopeInterceptor），此处仅受租户行拦截器约束
     * （doc_detail 在 blade.tenant.tables 白名单内，越租户改不动）。</p>
     */
    @Update("UPDATE doc_detail SET is_deleted = 1, update_time = #{updateTime}, update_user = #{updateUser} WHERE id = #{id} AND is_deleted = 0")
    int markRecycled(@Param("id") Long id, @Param("updateTime") Date updateTime, @Param("updateUser") Long updateUser);

    /**
     * 从回收站恢复：清除逻辑删除标记。
     *
     * <p>doc_status 保持删除前原值——「恢复」不等于「发布」，不回写生效态。</p>
     */
    @Update("UPDATE doc_detail SET is_deleted = 0, update_time = #{updateTime}, update_user = #{updateUser} WHERE id = #{id}")
    int restoreFromRecycle(@Param("id") Long id, @Param("updateTime") Date updateTime, @Param("updateUser") Long updateUser);

    /**
     * 彻底删除：物理删除主表行（绕过 @TableLogic 的逻辑删除）。
     */
    @Delete("DELETE FROM doc_detail WHERE id = #{id}")
    int purgePhysically(@Param("id") Long id);

    /**
     * 读取含「已逻辑删除」的文档（回收站恢复/彻底删除需要原状态）。
     *
     * <p>自定义 @Select 不会被 MP 自动追加 is_deleted 条件（同 selectByApproveInstId 手动加条件的行为）。</p>
     */
    @Select("SELECT * FROM doc_detail WHERE id = #{id}")
    DocDetail selectByIdIncludeDeleted(@Param("id") Long id);

    /**
     * 批量读取（含已逻辑删除）文档的标题/目录，供回收站列表回填文档标题。
     */
    @Select("<script>"
        + "SELECT id, doc_subject, sec_category_id FROM doc_detail "
        + "WHERE id IN <foreach item='i' collection='ids' open='(' separator=',' close=')'>#{i}</foreach>"
        + "</script>")
    List<DocDetail> selectBriefByIds(@Param("ids") List<Long> ids);

    /**
     * 按目录读取（含已逻辑删除）文档的标题/目录。
     *
     * <p>回收站按目录过滤专用：MP 的 selectList 会自动追加 is_deleted=0，
     * 用它过滤回收站将恒为空。</p>
     */
    @Select("SELECT id, doc_subject, sec_category_id FROM doc_detail WHERE sec_category_id = #{secCategoryId}")
    List<DocDetail> selectBriefByCategory(@Param("secCategoryId") Long secCategoryId);
}
