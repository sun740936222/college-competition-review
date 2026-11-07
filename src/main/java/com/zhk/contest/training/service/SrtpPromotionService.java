package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.training.entity.*;
import com.zhk.contest.training.enums.SrtpRecordStatus;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.*;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目推荐/晋级：学校管理员从源批次(如校级)遴选已通过项目，推荐到更高级别批次(省级/国家级)。
 * 在目标批次生成对应项目(复制信息+成员+指导教师，生成目标级别编号)，并记录推荐关系；防重复推荐。
 */
@Service
public class SrtpPromotionService {

    private final SrtpProjectMapper projectMapper;
    private final SrtpBatchMapper batchMapper;
    private final SrtpPromotionMapper promotionMapper;
    private final SrtpProjectMemberMapper memberMapper;
    private final SrtpProjectAdvisorMapper advisorMapper;
    private final SrtpProjectService projectService;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public SrtpPromotionService(SrtpProjectMapper projectMapper, SrtpBatchMapper batchMapper,
                                SrtpPromotionMapper promotionMapper, SrtpProjectMemberMapper memberMapper,
                                SrtpProjectAdvisorMapper advisorMapper, SrtpProjectService projectService,
                                UserMapper userMapper, OrganizationMapper organizationMapper) {
        this.projectMapper = projectMapper;
        this.batchMapper = batchMapper;
        this.promotionMapper = promotionMapper;
        this.memberMapper = memberMapper;
        this.advisorMapper = advisorMapper;
        this.projectService = projectService;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    public record PromoteResult(int promoted, int skipped, List<String> errors) {}

    @Transactional
    public PromoteResult promote(Long adminId, Long toBatchId, List<Long> projectIds) {
        requireSchoolAdmin(adminId);
        if (toBatchId == null) throw new BusinessException("请选择目标批次");
        if (projectIds == null || projectIds.isEmpty()) throw new BusinessException("请选择要推荐的项目");
        SrtpBatch toBatch = batchMapper.selectById(toBatchId);
        if (toBatch == null) throw new BusinessException("目标批次不存在");

        int promoted = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        for (Long pid : projectIds) {
            SrtpProject src = projectMapper.selectById(pid);
            if (src == null) { skipped++; errors.add("项目#" + pid + " 不存在"); continue; }
            if (src.getStatus() != SrtpRecordStatus.APPROVED) {
                skipped++; errors.add(src.getName() + "：未通过立项审核，不可推荐"); continue;
            }
            SrtpBatch fromBatch = batchMapper.selectById(src.getBatchId());
            if (!levelHigher(toBatch.getLevel(), fromBatch != null ? fromBatch.getLevel() : null)) {
                skipped++; errors.add(src.getName() + "：目标批次级别须高于源批次"); continue;
            }
            long dup = promotionMapper.selectCount(new QueryWrapper<SrtpPromotion>().lambda()
                    .eq(SrtpPromotion::getFromProjectId, pid)
                    .eq(SrtpPromotion::getToBatchId, toBatchId));
            if (dup > 0) { skipped++; errors.add(src.getName() + "：已推荐到该批次，跳过"); continue; }

            SrtpProject copy = copyProject(src, toBatchId);
            projectMapper.insert(copy);
            copy.setProjectNo(projectService.generateProjectNo(copy));
            projectMapper.updateById(copy);
            copyParticipants(src.getId(), copy.getId());

            SrtpPromotion promo = new SrtpPromotion();
            promo.setFromBatchId(src.getBatchId());
            promo.setFromProjectId(src.getId());
            promo.setToBatchId(toBatchId);
            promo.setToProjectId(copy.getId());
            promo.setPromotedBy(adminId);
            promotionMapper.insert(promo);
            promoted++;
        }
        return new PromoteResult(promoted, skipped, errors);
    }

    /** 某源批次中已被推荐（到任意更高批次）的项目ID集合，供前端标记 */
    public List<Long> promotedFromIds(Long adminId, Long fromBatchId) {
        requireSchoolAdmin(adminId);
        return promotionMapper.selectList(new QueryWrapper<SrtpPromotion>().lambda()
                        .eq(SrtpPromotion::getFromBatchId, fromBatchId))
                .stream().map(SrtpPromotion::getFromProjectId).distinct().collect(Collectors.toList());
    }

    // ===== 内部 =====

    private SrtpProject copyProject(SrtpProject src, Long toBatchId) {
        SrtpProject c = new SrtpProject();
        c.setBatchId(toBatchId);
        c.setOrgId(src.getOrgId());
        c.setCreatedBy(src.getCreatedBy());
        c.setProjectType(src.getProjectType());
        c.setName(src.getName());
        c.setDisciplineL1Id(src.getDisciplineL1Id());
        c.setDisciplineL2Id(src.getDisciplineL2Id());
        c.setStudentCount(src.getStudentCount());
        c.setDescription(src.getDescription());
        c.setExtraData(src.getExtraData());
        c.setStatus(SrtpRecordStatus.APPROVED); // 已推荐/上报到目标级别
        c.setCurrentStep(0);
        return c;
    }

    private void copyParticipants(Long fromId, Long toId) {
        List<SrtpProjectMember> members = memberMapper.selectList(new QueryWrapper<SrtpProjectMember>().lambda()
                .eq(SrtpProjectMember::getTargetType, SrtpStage.PROJECT)
                .eq(SrtpProjectMember::getTargetId, fromId));
        for (SrtpProjectMember m : members) {
            SrtpProjectMember n = new SrtpProjectMember();
            n.setTargetType(SrtpStage.PROJECT); n.setTargetId(toId);
            n.setUserId(m.getUserId()); n.setName(m.getName()); n.setStudentNo(m.getStudentNo());
            n.setMemberRole(m.getMemberRole()); n.setSortOrder(m.getSortOrder());
            memberMapper.insert(n);
        }
        List<SrtpProjectAdvisor> advisors = advisorMapper.selectList(new QueryWrapper<SrtpProjectAdvisor>().lambda()
                .eq(SrtpProjectAdvisor::getTargetType, SrtpStage.PROJECT)
                .eq(SrtpProjectAdvisor::getTargetId, fromId));
        for (SrtpProjectAdvisor a : advisors) {
            SrtpProjectAdvisor n = new SrtpProjectAdvisor();
            n.setTargetType(SrtpStage.PROJECT); n.setTargetId(toId);
            n.setUserId(a.getUserId()); n.setName(a.getName()); n.setTitle(a.getTitle()); n.setPhone(a.getPhone());
            n.setResearchProject(a.getResearchProject()); n.setSupportDesc(a.getSupportDesc()); n.setSortOrder(a.getSortOrder());
            advisorMapper.insert(n);
        }
    }

    private int rank(String level) {
        if (level == null) return 0;
        return switch (level) {
            case "校级" -> 1;
            case "省级" -> 2;
            case "国家级" -> 3;
            default -> 0;
        };
    }

    /** 目标级别是否高于源级别（级别未知时，只要求两者不同由调用方保证） */
    private boolean levelHigher(String toLevel, String fromLevel) {
        int t = rank(toLevel), f = rank(fromLevel);
        if (t == 0 || f == 0) return true; // 级别未标注则不强制方向
        return t > f;
    }

    private User requireSchoolAdmin(Long adminId) {
        User u = (adminId == null) ? null : userMapper.selectById(adminId);
        if (u == null) throw new BusinessException(403, "无权操作");
        AdminLevelResolver.requireAtLeast(u, OrganizationLevel.SCHOOL, organizationMapper);
        return u;
    }
}
