package com.zhk.contest.training.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.training.entity.SrtpApprovalFlowConfig;
import com.zhk.contest.training.entity.SrtpDiscipline;
import com.zhk.contest.training.enums.ApproverRole;
import com.zhk.contest.training.enums.SrtpStage;
import com.zhk.contest.training.mapper.SrtpApprovalFlowConfigMapper;
import com.zhk.contest.training.mapper.SrtpDisciplineMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * SRTP 模块独立初始化：播种学科字典与各阶段默认审批流模板（batch_id=NULL）。
 * 与竞赛系统的 InitDataConfig 完全独立，互不影响；幂等（已存在则跳过）。
 */
@Component
@Order(100)
public class SrtpInitDataConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SrtpInitDataConfig.class);

    private final SrtpDisciplineMapper disciplineMapper;
    private final SrtpApprovalFlowConfigMapper flowConfigMapper;

    public SrtpInitDataConfig(SrtpDisciplineMapper disciplineMapper,
                              SrtpApprovalFlowConfigMapper flowConfigMapper) {
        this.disciplineMapper = disciplineMapper;
        this.flowConfigMapper = flowConfigMapper;
    }

    @Override
    public void run(String... args) {
        seedDisciplines();
        seedDefaultFlowTemplates();
    }

    // ===== 学科字典 =====

    private void seedDisciplines() {
        if (disciplineMapper.selectCount(null) > 0) return;
        log.info("========== 初始化 SRTP 学科字典 ==========");

        String[] level1 = {
                "哲学", "经济学", "法学", "教育学", "文学", "历史学",
                "理学", "工学", "农学", "医学", "管理学", "艺术学"
        };
        int order = 1;
        Long medicineId = null;
        for (String name : level1) {
            Long id = insertDiscipline(null, name, order++);
            if ("医学".equals(name)) medicineId = id;
        }

        // 为医学门类补充常见二级学科（面向医学院场景）
        if (medicineId != null) {
            String[] medL2 = {
                    "基础医学", "临床医学", "口腔医学", "公共卫生与预防医学",
                    "中医学", "药学", "护理学", "医学技术"
            };
            int o = 1;
            for (String name : medL2) {
                insertDiscipline(medicineId, name, o++);
            }
        }
        log.info("========== SRTP 学科字典初始化完成 ==========");
    }

    private Long insertDiscipline(Long parentId, String name, int sortOrder) {
        SrtpDiscipline d = new SrtpDiscipline();
        d.setParentId(parentId);
        d.setName(name);
        d.setSortOrder(sortOrder);
        disciplineMapper.insert(d);
        return d.getId();
    }

    // ===== 默认审批流模板（batch_id = NULL） =====

    private void seedDefaultFlowTemplates() {
        long existing = flowConfigMapper.selectCount(
                new QueryWrapper<SrtpApprovalFlowConfig>().isNull("batch_id"));
        if (existing > 0) return;
        log.info("========== 初始化 SRTP 默认审批流模板 ==========");

        // 选题：学院审核 → 学校审核
        insertFlow(SrtpStage.TOPIC, 1, "学院审核选题", ApproverRole.COLLEGE_ADMIN, false);
        insertFlow(SrtpStage.TOPIC, 2, "学校审核选题", ApproverRole.SCHOOL_ADMIN, false);

        // 立项：指导教师 → 学院 → 学校（专家评审由学校在决定推荐省级时另行组织）
        insertFlow(SrtpStage.PROJECT, 1, "指导教师审核", ApproverRole.TEACHER, false);
        insertFlow(SrtpStage.PROJECT, 2, "学院立项审核", ApproverRole.COLLEGE_ADMIN, false);
        insertFlow(SrtpStage.PROJECT, 3, "学校立项审核", ApproverRole.SCHOOL_ADMIN, false);

        // 中期：指导教师 → 学院 → 学校
        insertFlow(SrtpStage.MIDTERM, 1, "指导教师审核", ApproverRole.TEACHER, false);
        insertFlow(SrtpStage.MIDTERM, 2, "学院中期审核", ApproverRole.COLLEGE_ADMIN, false);
        insertFlow(SrtpStage.MIDTERM, 3, "学校中期审核", ApproverRole.SCHOOL_ADMIN, false);

        // 结题：指导教师 → 学院 → 校专家评审(定等级) → 学校
        insertFlow(SrtpStage.CONCLUSION, 1, "指导教师审核", ApproverRole.TEACHER, false);
        insertFlow(SrtpStage.CONCLUSION, 2, "学院结题审核", ApproverRole.COLLEGE_ADMIN, false);
        insertFlow(SrtpStage.CONCLUSION, 3, "校专家结题评审", ApproverRole.SCHOOL_EXPERT, false);
        insertFlow(SrtpStage.CONCLUSION, 4, "学校结题审核", ApproverRole.SCHOOL_ADMIN, false);

        log.info("========== SRTP 默认审批流模板初始化完成 ==========");
    }

    private void insertFlow(SrtpStage stage, int stepOrder, String stepName,
                            ApproverRole role, boolean needAssignExpert) {
        SrtpApprovalFlowConfig c = new SrtpApprovalFlowConfig();
        c.setBatchId(null);
        c.setStage(stage);
        c.setStepOrder(stepOrder);
        c.setStepName(stepName);
        c.setApproverRole(role);
        c.setNeedAssignExpert(needAssignExpert);
        flowConfigMapper.insert(c);
    }
}
