package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.Organization;
import com.zhk.contest.enums.OrganizationLevel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OrganizationMapper extends BaseMapper<Organization> {

    @Select("SELECT * FROM organization WHERE invite_code = #{inviteCode}")
    Optional<Organization> findByInviteCode(String inviteCode);

    @Select("SELECT COUNT(1) FROM organization WHERE invite_code = #{inviteCode}")
    boolean existsByInviteCode(String inviteCode);

    /** 递归 CTE：返回 root org 及其所有子 org 的 ID 列表 */
    @Select("WITH RECURSIVE org_tree AS (" +
            "  SELECT id FROM organization WHERE id = #{rootOrgId} " +
            "  UNION ALL " +
            "  SELECT o.id FROM organization o " +
            "  INNER JOIN org_tree ot ON o.parent_id = ot.id" +
            ") SELECT id FROM org_tree")
    List<Long> findOrgTreeIds(@Param("rootOrgId") Long rootOrgId);

    /** 从任意 org 向上追溯到 root org */
    default Long findRootOrgId(Long orgId) {
        if (orgId == null) return null;
        Long current = orgId;
        while (true) {
            Organization org = selectById(current);
            if (org == null || org.getParentId() == null) return current;
            current = org.getParentId();
        }
    }

    /** 从任意 org 向上追溯，返回自身及所有祖先 org 的 ID 列表 */
    default List<Long> findAncestorOrgIds(Long orgId) {
        List<Long> ancestors = new java.util.ArrayList<>();
        Long current = orgId;
        while (current != null) {
            ancestors.add(current);
            Organization org = selectById(current);
            current = org != null ? org.getParentId() : null;
        }
        return ancestors;
    }

    /** 从任意 org 向上追溯 root org 并返回其名称，若 org 本身即为 root 则返回 null */
    default String findRootOrgName(Long orgId) {
        if (orgId == null) return null;
        Long rootId = findRootOrgId(orgId);
        if (rootId == null || rootId.equals(orgId)) return null;
        Organization root = selectById(rootId);
        return root != null ? root.getName() : null;
    }

    /** 使用递归 CTE 从任意 org 向上查找 SCHOOL 级别的组织名称，找不到则返回自身名称 */
    @Select("WITH RECURSIVE org_ancestors AS (" +
            "  SELECT id, name, level, parent_id FROM organization WHERE id = #{orgId} " +
            "  UNION ALL " +
            "  SELECT o.id, o.name, o.level, o.parent_id " +
            "  FROM organization o INNER JOIN org_ancestors oa ON o.id = oa.parent_id" +
            ") SELECT name FROM org_ancestors WHERE level = 'SCHOOL' LIMIT 1")
    String findSchoolAncestorName(@Param("orgId") Long orgId);

    default String findSchoolOrgName(Long orgId) {
        if (orgId == null) return null;
        String schoolName = findSchoolAncestorName(orgId);
        if (schoolName != null) return schoolName;
        Organization self = selectById(orgId);
        return self != null ? self.getName() : null;
    }
}