package com.zhk.contest.training.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.training.dto.SrtpAdvisorDTO;
import com.zhk.contest.training.dto.SrtpMemberDTO;
import com.zhk.contest.training.entity.SrtpProjectAdvisor;
import com.zhk.contest.training.entity.SrtpProjectMember;
import com.zhk.contest.training.enums.SrtpMemberKind;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpProjectAdvisorMapper;
import com.zhk.contest.training.mapper.SrtpProjectMemberMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 成员/指导教师子表通用读写（四阶段共用）。保存采用"全量替换"：先删后插。
 */
@Service
public class SrtpParticipantService {

    private final SrtpProjectMemberMapper memberMapper;
    private final SrtpProjectAdvisorMapper advisorMapper;

    public SrtpParticipantService(SrtpProjectMemberMapper memberMapper,
                                  SrtpProjectAdvisorMapper advisorMapper) {
        this.memberMapper = memberMapper;
        this.advisorMapper = advisorMapper;
    }

    public List<SrtpProjectMember> listMembers(SrtpStage stage, Long targetId) {
        return memberMapper.selectList(
                new QueryWrapper<SrtpProjectMember>().lambda()
                        .eq(SrtpProjectMember::getTargetType, stage)
                        .eq(SrtpProjectMember::getTargetId, targetId)
                        .orderByAsc(SrtpProjectMember::getSortOrder));
    }

    public List<SrtpProjectAdvisor> listAdvisors(SrtpStage stage, Long targetId) {
        return advisorMapper.selectList(
                new QueryWrapper<SrtpProjectAdvisor>().lambda()
                        .eq(SrtpProjectAdvisor::getTargetType, stage)
                        .eq(SrtpProjectAdvisor::getTargetId, targetId)
                        .orderByAsc(SrtpProjectAdvisor::getSortOrder));
    }

    public void replaceMembers(SrtpStage stage, Long targetId, List<SrtpMemberDTO> members) {
        memberMapper.delete(new QueryWrapper<SrtpProjectMember>().lambda()
                .eq(SrtpProjectMember::getTargetType, stage)
                .eq(SrtpProjectMember::getTargetId, targetId));
        if (members == null) return;
        int order = 0;
        for (SrtpMemberDTO dto : members) {
            SrtpProjectMember m = new SrtpProjectMember();
            m.setTargetType(stage);
            m.setTargetId(targetId);
            m.setUserId(dto.getUserId());
            m.setName(dto.getName());
            m.setStudentNo(dto.getStudentNo());
            m.setMemberRole(dto.getMemberRole() != null ? dto.getMemberRole() : SrtpMemberKind.MEMBER);
            m.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++);
            memberMapper.insert(m);
        }
    }

    public void replaceAdvisors(SrtpStage stage, Long targetId, List<SrtpAdvisorDTO> advisors) {
        advisorMapper.delete(new QueryWrapper<SrtpProjectAdvisor>().lambda()
                .eq(SrtpProjectAdvisor::getTargetType, stage)
                .eq(SrtpProjectAdvisor::getTargetId, targetId));
        if (advisors == null) return;
        int order = 0;
        for (SrtpAdvisorDTO dto : advisors) {
            SrtpProjectAdvisor a = new SrtpProjectAdvisor();
            a.setTargetType(stage);
            a.setTargetId(targetId);
            a.setUserId(dto.getUserId());
            a.setName(dto.getName());
            a.setTitle(dto.getTitle());
            a.setPhone(dto.getPhone());
            a.setResearchProject(dto.getResearchProject());
            a.setSupportDesc(dto.getSupportDesc());
            a.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : order++);
            advisorMapper.insert(a);
        }
    }

    /**
     * 把某个立项的团队成员和指导教师全量复制到指定 stage/targetId(通常是同一学生的中期/结题报告)。
     * 中期/结题不再让学生重复填写这些信息——它们就是立项时那一份的延续快照。
     * 返回复制过来的成员数量，供调用方回填 student_count。
     */
    public int copyFromProject(Long fromProjectId, SrtpStage toStage, Long toTargetId) {
        // 先清空可能残留的旧数据（例如上一次草稿失败留下的行）
        memberMapper.delete(new QueryWrapper<SrtpProjectMember>().lambda()
                .eq(SrtpProjectMember::getTargetType, toStage)
                .eq(SrtpProjectMember::getTargetId, toTargetId));
        advisorMapper.delete(new QueryWrapper<SrtpProjectAdvisor>().lambda()
                .eq(SrtpProjectAdvisor::getTargetType, toStage)
                .eq(SrtpProjectAdvisor::getTargetId, toTargetId));

        List<SrtpProjectMember> members = memberMapper.selectList(
                new QueryWrapper<SrtpProjectMember>().lambda()
                        .eq(SrtpProjectMember::getTargetType, SrtpStage.PROJECT)
                        .eq(SrtpProjectMember::getTargetId, fromProjectId)
                        .orderByAsc(SrtpProjectMember::getSortOrder));
        for (SrtpProjectMember src : members) {
            SrtpProjectMember n = new SrtpProjectMember();
            n.setTargetType(toStage);
            n.setTargetId(toTargetId);
            n.setUserId(src.getUserId());
            n.setName(src.getName());
            n.setStudentNo(src.getStudentNo());
            n.setMemberRole(src.getMemberRole());
            n.setSortOrder(src.getSortOrder());
            memberMapper.insert(n);
        }

        List<SrtpProjectAdvisor> advisors = advisorMapper.selectList(
                new QueryWrapper<SrtpProjectAdvisor>().lambda()
                        .eq(SrtpProjectAdvisor::getTargetType, SrtpStage.PROJECT)
                        .eq(SrtpProjectAdvisor::getTargetId, fromProjectId)
                        .orderByAsc(SrtpProjectAdvisor::getSortOrder));
        for (SrtpProjectAdvisor src : advisors) {
            SrtpProjectAdvisor n = new SrtpProjectAdvisor();
            n.setTargetType(toStage);
            n.setTargetId(toTargetId);
            n.setUserId(src.getUserId());
            n.setName(src.getName());
            n.setTitle(src.getTitle());
            n.setPhone(src.getPhone());
            n.setResearchProject(src.getResearchProject());
            n.setSupportDesc(src.getSupportDesc());
            n.setSortOrder(src.getSortOrder());
            advisorMapper.insert(n);
        }
        return members.size();
    }

    /**
     * 提交前校验：至少绑定一名"已注册教师账号"的指导教师（按 user_id）。
     * 指导教师若只填了姓名/职称/电话而未从教师账号选择器中选中，user_id 为空，
     * 会导致"指导教师审核"环节永远无人可审（按 user_id 匹配），记录卡死无法再被任何人处理。
     */
    public void requireBoundAdvisor(SrtpStage stage, Long targetId) {
        boolean hasBound = listAdvisors(stage, targetId).stream().anyMatch(a -> a.getUserId() != null);
        if (!hasBound) {
            throw new BusinessException("请在“指导教师”处通过搜索姓名/工号选择一位已注册的教师账号后再提交（仅填写姓名/职称/电话无法进入指导教师审核环节）");
        }
    }
}
