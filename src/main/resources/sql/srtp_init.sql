-- ==============================================================
-- 大学生创新创业训练计划（SRTP）模块 数据库初始化脚本
-- 设计依据：PRD_SRTP.md
-- 原则：全部表加 srtp_ 前缀；CREATE TABLE IF NOT EXISTS（幂等、追加式）；
--       绝不 DROP / ALTER 竞赛系统既有表；逻辑外键引用 user / organization。
-- 用法：mysql -u <user> -p contest_db < srtp_init.sql
-- ==============================================================

USE contest_db;
SET NAMES utf8mb4;

-- ==================== 大创角色分配（一人多角） ====================
CREATE TABLE IF NOT EXISTS srtp_member_role (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT      NOT NULL COMMENT '关联 user.id',
    role_type    VARCHAR(20) NOT NULL COMMENT '角色: STUDENT,TEACHER,EXPERT,COLLEGE_ADMIN,SCHOOL_ADMIN',
    org_id       BIGINT      NULL     COMMENT '角色所属组织(院/校) organization.id',
    expert_level VARCHAR(20) NULL     COMMENT 'role_type=EXPERT 时: COLLEGE,SCHOOL',
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE,DISABLED',
    must_change_password TINYINT(1) NOT NULL DEFAULT 0 COMMENT '首次登录需修改密码',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_role_user (user_id),
    INDEX idx_srtp_role_type (role_type),
    INDEX idx_srtp_role_org (org_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP角色分配';

-- ==================== 专家库（含教师自主申请入库） ====================
CREATE TABLE IF NOT EXISTS srtp_expert (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NULL     COMMENT '校内教师关联 user.id，可为空',
    name           VARCHAR(50)  NOT NULL COMMENT '姓名',
    phone          VARCHAR(20)  NULL     COMMENT '联系电话',
    email          VARCHAR(100) NULL     COMMENT '邮箱',
    org_id         BIGINT       NULL     COMMENT '所属院/校 organization.id',
    title          VARCHAR(50)  NULL     COMMENT '职称',
    research_field VARCHAR(255) NULL     COMMENT '研究方向',
    expert_level   VARCHAR(20)  NOT NULL DEFAULT 'COLLEGE' COMMENT 'COLLEGE,SCHOOL',
    source         VARCHAR(20)  NOT NULL DEFAULT 'ADMIN_ADD' COMMENT 'ADMIN_ADD,SELF_APPLY,IMPORT',
    status         VARCHAR(20)  NOT NULL DEFAULT 'APPROVED' COMMENT 'PENDING,APPROVED,REJECTED',
    apply_comment  VARCHAR(500) NULL     COMMENT '自主申请说明',
    reject_reason  VARCHAR(500) NULL     COMMENT '驳回意见',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_expert_user (user_id),
    INDEX idx_srtp_expert_org (org_id),
    INDEX idx_srtp_expert_level (expert_level),
    INDEX idx_srtp_expert_status (status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP专家库';

-- ==================== 训练计划批次 ====================
CREATE TABLE IF NOT EXISTS srtp_batch (
    id                 BIGINT      AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(100) NOT NULL COMMENT '批次名称',
    code               VARCHAR(30)  NULL     COMMENT '批次编码(用于项目编号,如 2026GJ)',
    year               INT          NULL     COMMENT '年度',
    level              VARCHAR(20)  NULL     COMMENT '级别: 校级,省级,国家级',
    status             VARCHAR(20)  NOT NULL DEFAULT 'TOPIC_OPEN' COMMENT 'TOPIC_OPEN,PROJECT_OPEN,MIDTERM_OPEN,CONCLUSION_OPEN,CLOSED',
    project_deadline   DATETIME     NULL     COMMENT '立项截止(过时禁止提交)',
    midterm_deadline   DATETIME     NULL     COMMENT '中期截止',
    conclusion_deadline DATETIME    NULL     COMMENT '结题截止',
    org_id             BIGINT       NULL     COMMENT '归属组织(一般为学校) organization.id',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_batch_org (org_id),
    INDEX idx_srtp_batch_status (status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP批次';

-- ==================== 学科字典（一/二级学科树） ====================
CREATE TABLE IF NOT EXISTS srtp_discipline (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT      NULL     COMMENT '一级学科为 NULL，二级指向一级',
    name       VARCHAR(100) NOT NULL COMMENT '学科名称',
    code       VARCHAR(30)  NULL     COMMENT '学科代码',
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_discipline_parent (parent_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP学科字典';

-- ==================== 学院编码映射（项目编号用，因不改 organization 表） ====================
CREATE TABLE IF NOT EXISTS srtp_org_code (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    org_id     BIGINT      NOT NULL COMMENT 'organization.id',
    code       VARCHAR(30) NOT NULL COMMENT '学院编码(如 YXY)',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_srtp_org_code_org (org_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP学院编码映射';

-- ==================== 自定义字段定义 ====================
CREATE TABLE IF NOT EXISTS srtp_custom_field (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    stage      VARCHAR(20)  NOT NULL COMMENT 'TOPIC,PROJECT,MIDTERM,CONCLUSION',
    batch_id   BIGINT       NULL     COMMENT 'NULL=全局默认，否则针对某批次',
    field_key  VARCHAR(50)  NOT NULL COMMENT '存入 extra_data JSON 的键',
    label      VARCHAR(100) NOT NULL COMMENT '显示名',
    field_type VARCHAR(20)  NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT,TEXTAREA,NUMBER,DATE,SELECT,MULTISELECT,FILE',
    options    JSON         NULL     COMMENT 'SELECT/MULTISELECT 的选项',
    required   TINYINT(1)   NOT NULL DEFAULT 0,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_field_stage_batch (stage, batch_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP自定义字段定义';

-- ==================== 审批流程节点配置（模板 + 批次覆盖） ====================
CREATE TABLE IF NOT EXISTS srtp_approval_flow_config (
    id                 BIGINT      AUTO_INCREMENT PRIMARY KEY,
    batch_id           BIGINT      NULL     COMMENT 'NULL=全局默认模板；否则该批次副本',
    stage              VARCHAR(20) NOT NULL COMMENT 'TOPIC,PROJECT,MIDTERM,CONCLUSION',
    step_order         INT         NOT NULL COMMENT '节点顺序(从1起)',
    step_name          VARCHAR(50) NOT NULL COMMENT '如: 院专立项审核',
    approver_role      VARCHAR(30) NOT NULL COMMENT 'TEACHER,COLLEGE_EXPERT,SCHOOL_EXPERT,COLLEGE_ADMIN,SCHOOL_ADMIN',
    need_assign_expert TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '该节点是否需先分配校级专家',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_flow_batch_stage (batch_id, stage, step_order)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP审批流程配置';

-- ==================== 选题（双通道：学生自拟 / 学校给定后学生认领） ====================
CREATE TABLE IF NOT EXISTS srtp_topic (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    batch_id          BIGINT       NOT NULL COMMENT '所属批次',
    org_id            BIGINT       NULL     COMMENT '归属学院',
    source            VARCHAR(20)  NOT NULL DEFAULT 'STUDENT_PROPOSED' COMMENT 'STUDENT_PROPOSED,SCHOOL_ASSIGNED',
    project_type      VARCHAR(50)  NULL     COMMENT '项目类型',
    name              VARCHAR(200) NOT NULL COMMENT '选题名称',
    discipline_l1_id  BIGINT       NULL     COMMENT '一级学科',
    discipline_l2_id  BIGINT       NULL     COMMENT '二级学科',
    research_type     VARCHAR(50)  NULL     COMMENT '创意研究类型',
    initiator_id      BIGINT       NULL     COMMENT '选题发起人(学生自拟)/发布人(学校给定) user.id',
    teacher_id        BIGINT       NULL     COMMENT '出题教师 user.id(老师出题时绑定，学生选此题则指导教师=该老师)',
    initiator_contact VARCHAR(100) NULL     COMMENT '联系方式',
    claimed_by        BIGINT       NULL     COMMENT '学校给定选题被认领的学生(团队负责人) user.id',
    claim_status      VARCHAR(20)  NULL     COMMENT '学校给定选题认领状态: OPEN,CLAIMED',
    info              TEXT         NULL     COMMENT '选题信息/简介',
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT,SUBMITTED,IN_REVIEW,APPROVED,RETURNED,REJECTED',
    current_step      INT          NOT NULL DEFAULT 0 COMMENT '当前审批节点 step_order',
    reject_reason     VARCHAR(500) NULL     COMMENT '最近一次驳回意见',
    extra_data        JSON         NULL     COMMENT '自定义字段值',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_topic_batch (batch_id),
    INDEX idx_srtp_topic_org (org_id),
    INDEX idx_srtp_topic_initiator (initiator_id),
    INDEX idx_srtp_topic_status (status),
    INDEX idx_srtp_topic_claim (claim_status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP选题';

-- ==================== 立项（项目主档） ====================
CREATE TABLE IF NOT EXISTS srtp_project (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    batch_id         BIGINT       NOT NULL COMMENT '所属批次',
    topic_id         BIGINT       NULL     COMMENT '来源选题(若有)',
    org_id           BIGINT       NULL     COMMENT '归属学院',
    created_by       BIGINT       NULL     COMMENT '创建人(申报学生) user.id',
    project_type     VARCHAR(20)  NULL     COMMENT '项目类别: A创新训练/B创业训练/C创业实践',
    project_no       VARCHAR(50)  NULL     COMMENT '项目编号(批次code+学院码+序号)，立项通过时生成',
    name             VARCHAR(200) NOT NULL COMMENT '项目名称',
    discipline_l1_id BIGINT       NULL     COMMENT '一级学科',
    discipline_l2_id BIGINT       NULL     COMMENT '二级学科',
    student_count    INT          NULL     COMMENT '参与学生人数',
    description      TEXT         NULL     COMMENT '项目简介',
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT,SUBMITTED,IN_REVIEW,APPROVED,RETURNED,REJECTED',
    current_step     INT          NOT NULL DEFAULT 0 COMMENT '当前审批节点',
    reject_reason    VARCHAR(500) NULL     COMMENT '最近一次驳回意见',
    extra_data       JSON         NULL     COMMENT '自定义字段值',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_project_batch (batch_id),
    INDEX idx_srtp_project_org (org_id),
    INDEX idx_srtp_project_topic (topic_id),
    INDEX idx_srtp_project_status (status),
    UNIQUE KEY uk_srtp_project_no (project_no)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP立项(项目主档)';

-- ==================== 中期报告（独立记录 + 提交时快照） ====================
CREATE TABLE IF NOT EXISTS srtp_midterm_report (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    project_id       BIGINT       NOT NULL COMMENT '关联项目主档',
    batch_id         BIGINT       NULL     COMMENT '冗余便于查询',
    created_by       BIGINT       NULL     COMMENT '填报人 user.id',
    snapshot_name    VARCHAR(200) NULL     COMMENT '提交时快照-项目名称',
    project_type     VARCHAR(50)  NULL     COMMENT '项目类型',
    discipline_l1_id BIGINT       NULL     COMMENT '一级学科(快照)',
    discipline_l2_id BIGINT       NULL     COMMENT '二级学科(快照)',
    student_count    INT          NULL     COMMENT '参与学生人数',
    description      TEXT         NULL     COMMENT '中期报告简介/内容',
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT,SUBMITTED,IN_REVIEW,APPROVED,RETURNED,REJECTED',
    current_step     INT          NOT NULL DEFAULT 0 COMMENT '当前审批节点',
    reject_reason    VARCHAR(500) NULL     COMMENT '最近一次驳回意见',
    extra_data       JSON         NULL     COMMENT '自定义字段值',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_midterm_project (project_id),
    INDEX idx_srtp_midterm_status (status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP中期报告';

-- ==================== 结题报告（独立记录 + 提交时快照） ====================
CREATE TABLE IF NOT EXISTS srtp_conclusion_report (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    project_id       BIGINT       NOT NULL COMMENT '关联项目主档',
    batch_id         BIGINT       NULL     COMMENT '冗余便于查询',
    created_by       BIGINT       NULL     COMMENT '填报人 user.id',
    snapshot_name    VARCHAR(200) NULL     COMMENT '提交时快照-项目名称',
    project_type     VARCHAR(50)  NULL     COMMENT '项目类型',
    discipline_l1_id BIGINT       NULL     COMMENT '一级学科(快照)',
    discipline_l2_id BIGINT       NULL     COMMENT '二级学科(快照)',
    student_count    INT          NULL     COMMENT '参与学生人数',
    description      TEXT         NULL     COMMENT '结题报告简介/内容',
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT,SUBMITTED,IN_REVIEW,APPROVED,RETURNED,REJECTED',
    current_step     INT          NOT NULL DEFAULT 0 COMMENT '当前审批节点',
    reject_reason    VARCHAR(500) NULL     COMMENT '最近一次驳回意见',
    conclusion_grade VARCHAR(20)  NULL     COMMENT '结题等级(校专家评审给出): PASS/GOOD/EXCELLENT',
    extra_data       JSON         NULL     COMMENT '自定义字段值',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_conclusion_project (project_id),
    INDEX idx_srtp_conclusion_status (status)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP结题报告';

-- ==================== 项目成员子表（四阶段通用，支持快照） ====================
CREATE TABLE IF NOT EXISTS srtp_project_member (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(20) NOT NULL COMMENT '归属: TOPIC,PROJECT,MIDTERM,CONCLUSION',
    target_id   BIGINT      NOT NULL COMMENT '对应记录 id',
    user_id     BIGINT      NULL     COMMENT '校内学生关联 user.id',
    name        VARCHAR(50) NULL     COMMENT '姓名',
    student_no  VARCHAR(50) NULL     COMMENT '学号',
    member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT 'LEADER,MEMBER',
    sort_order  INT         NOT NULL DEFAULT 0,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_member_target (target_type, target_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP项目成员';

-- ==================== 指导教师子表（四阶段通用，支持快照） ====================
CREATE TABLE IF NOT EXISTS srtp_project_advisor (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    target_type      VARCHAR(20)  NOT NULL COMMENT '归属: TOPIC,PROJECT,MIDTERM,CONCLUSION',
    target_id        BIGINT       NOT NULL COMMENT '对应记录 id',
    user_id          BIGINT       NULL     COMMENT '关联教师 user.id',
    name             VARCHAR(50)  NULL     COMMENT '姓名',
    title            VARCHAR(50)  NULL     COMMENT '职称',
    phone            VARCHAR(20)  NULL     COMMENT '电话',
    research_project TEXT         NULL     COMMENT '承担的科研课题情况(选题阶段)',
    support_desc     TEXT         NULL     COMMENT '对本项目的支持情况(选题阶段)',
    sort_order       INT          NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_advisor_target (target_type, target_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP指导教师';

-- ==================== 审批日志（四阶段通用） ====================
CREATE TABLE IF NOT EXISTS srtp_approval_record (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    stage       VARCHAR(20) NOT NULL COMMENT 'TOPIC,PROJECT,MIDTERM,CONCLUSION',
    target_id   BIGINT      NOT NULL COMMENT '被审批记录 id',
    step_order  INT         NOT NULL COMMENT '节点',
    step_name   VARCHAR(50) NULL     COMMENT '节点名称',
    approver_id BIGINT      NULL     COMMENT '审批人 user.id',
    action      VARCHAR(20) NOT NULL COMMENT 'APPROVE,REJECT',
    comment     TEXT        NULL     COMMENT '审批/驳回意见',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_appr_target (stage, target_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP审批日志';

-- ==================== 附件（四阶段通用，仿 project_material） ====================
CREATE TABLE IF NOT EXISTS srtp_attachment (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    stage             VARCHAR(20)  NOT NULL COMMENT 'TOPIC,PROJECT,MIDTERM,CONCLUSION',
    target_id         BIGINT       NOT NULL COMMENT '归属记录 id',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_filename   VARCHAR(255) NOT NULL COMMENT '存储文件名(UUID)',
    file_path         VARCHAR(500) NOT NULL COMMENT '存储路径 uploads/srtp/...',
    file_size         BIGINT       NULL     COMMENT '文件大小',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_attach_target (stage, target_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP附件';

-- ==================== 校级专家分配（对应"分配校级专家"环节） ====================
CREATE TABLE IF NOT EXISTS srtp_expert_assignment (
    id             BIGINT      AUTO_INCREMENT PRIMARY KEY,
    stage          VARCHAR(20) NOT NULL COMMENT '被评审记录阶段',
    target_id      BIGINT      NOT NULL COMMENT '被评审记录 id',
    expert_id      BIGINT      NOT NULL COMMENT '关联 srtp_expert.id',
    assigned_by    BIGINT      NULL     COMMENT '分配人 user.id',
    review_status  VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
    review_comment TEXT        NULL     COMMENT '专家评审意见',
    score          INT         NULL     COMMENT '专家打分 0-100',
    reviewed_at    DATETIME    NULL     COMMENT '评审提交时间',
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_assign_target (stage, target_id),
    INDEX idx_srtp_assign_expert (expert_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP校级专家分配';

-- ==================== 项目推荐/晋级（校级→省级→国家级） ====================
CREATE TABLE IF NOT EXISTS srtp_promotion (
    id             BIGINT   AUTO_INCREMENT PRIMARY KEY,
    from_batch_id  BIGINT   NOT NULL COMMENT '源批次',
    from_project_id BIGINT  NOT NULL COMMENT '源项目',
    to_batch_id    BIGINT   NOT NULL COMMENT '目标批次(更高级别)',
    to_project_id  BIGINT   NULL     COMMENT '目标批次中生成的项目',
    promoted_by    BIGINT   NULL     COMMENT '推荐人(学校管理员) user.id',
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_srtp_promo_from (from_project_id),
    INDEX idx_srtp_promo_to_batch (to_batch_id),
    UNIQUE KEY uk_srtp_promo (from_project_id, to_batch_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT 'SRTP项目推荐/晋级';
