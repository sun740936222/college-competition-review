-- ==============================================================
-- 竞赛在线评审系统 数据库初始化脚本（纯建表，不含 ALTER）
-- ==============================================================

CREATE DATABASE IF NOT EXISTS contest_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE contest_db;

SET NAMES utf8mb4;

-- ==================== 清理旧表 ====================
DROP TABLE IF EXISTS review_log;
DROP TABLE IF EXISTS score_record;
DROP TABLE IF EXISTS review_allocation;
DROP TABLE IF EXISTS score_criteria;
DROP TABLE IF EXISTS event_promotion;
DROP TABLE IF EXISTS project_material;
DROP TABLE IF EXISTS advisor;
DROP TABLE IF EXISTS team_member;
DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS track;
DROP TABLE IF EXISTS competition_template_track;
DROP TABLE IF EXISTS competition_template;
DROP TABLE IF EXISTS event;
DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS organization;

-- ==================== 组织机构 ====================
CREATE TABLE organization (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL COMMENT '名称',
    invite_code VARCHAR(50)  NOT NULL COMMENT '邀请码',
    description VARCHAR(500) NULL     COMMENT '描述',
    parent_id   BIGINT       NULL     COMMENT '父机构ID',
    level       VARCHAR(20)  NOT NULL DEFAULT 'SCHOOL' COMMENT '层级: COLLEGE,SCHOOL,PROVINCE,NATIONAL',
    is_external TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否外部评委单位',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_invite_code (invite_code),
    INDEX idx_org_level (level),
    INDEX idx_org_parent_level (parent_id, level)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '组织机构';

-- ==================== 用户 ====================
CREATE TABLE user (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password        VARCHAR(100) NOT NULL COMMENT '密码',
    real_name       VARCHAR(50)  NULL     COMMENT '真实姓名',
    role            VARCHAR(20)  NOT NULL DEFAULT 'CONTESTANT' COMMENT '角色: ADMIN,SUPER_ADMIN,CONTESTANT,JUDGE',
    organization_id BIGINT       NULL     COMMENT '所属机构ID',
    phone           VARCHAR(20)  NULL     COMMENT '电话',
    email           VARCHAR(100) NULL     COMMENT '邮箱',
    created_by      BIGINT       NULL     COMMENT '创建者用户ID',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_organization_id (organization_id),
    CONSTRAINT fk_user_organization FOREIGN KEY (organization_id) REFERENCES organization(id) ON DELETE SET NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户';

-- ==================== 赛事 ====================
CREATE TABLE event (
    id                       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name                     VARCHAR(200) NOT NULL COMMENT '赛事名称',
    description              TEXT         NULL     COMMENT '赛事描述',
    registration_deadline    DATETIME     NULL     COMMENT '报名截止日期',
    submission_deadline      DATETIME     NULL     COMMENT '提交截止日期',
    review_start_time        DATETIME     NULL     COMMENT '评审开始时间',
    review_end_time          DATETIME     NULL     COMMENT '评审结束时间',
    status                   VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT,OPEN,CLOSED',
    competition_level        VARCHAR(20)  NOT NULL DEFAULT 'SCHOOL' COMMENT '赛事级别: COLLEGE,SCHOOL,PROVINCE,NATIONAL',
    organization_id          BIGINT       NULL     COMMENT '归属机构ID',
    require_video            TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否需要视频',
    allow_resubmit           TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否允许评委提交后修改评分',
    require_comment          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '提交评分时必须填写评语',
    attachment_name          VARCHAR(255) NULL     COMMENT '附件原始文件名',
    attachment_path          VARCHAR(500) NULL     COMMENT '附件存储路径',
    parent_event_id          BIGINT       NULL     COMMENT '父赛事ID',
    created_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_event_org (organization_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '赛事';

-- ==================== 赛道 ====================
CREATE TABLE track (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    event_id    BIGINT       NOT NULL COMMENT '所属赛事ID',
    name        VARCHAR(200) NOT NULL COMMENT '赛道名称',
    group_name  VARCHAR(100) NULL     COMMENT '组别名称（如本科生创意组）',
    description TEXT         NULL     COMMENT '赛道描述',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    CONSTRAINT fk_track_event FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '赛道';

-- ==================== 赛事模板 ====================
CREATE TABLE competition_template (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL COMMENT '赛事名称',
    description VARCHAR(500) NULL     COMMENT '赛事描述',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '赛事模板';

-- ==================== 赛事模板赛道 ====================
CREATE TABLE competition_template_track (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT       NOT NULL COMMENT '所属模板ID',
    name        VARCHAR(200) NOT NULL COMMENT '赛道名称',
    group_name  VARCHAR(100) NULL     COMMENT '组别名称',
    description VARCHAR(500) NULL     COMMENT '赛道描述',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    CONSTRAINT fk_ctrack_template FOREIGN KEY (template_id) REFERENCES competition_template(id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '赛事模板赛道';

-- ==================== 项目 ====================
CREATE TABLE project (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL COMMENT '创建者用户ID',
    event_id      BIGINT       NOT NULL COMMENT '所属赛事ID',
    track_id      BIGINT       NOT NULL COMMENT '所属赛道ID',
    name          VARCHAR(200) NOT NULL COMMENT '项目名称',
    description   TEXT         NULL     COMMENT '项目描述',
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT,PENDING_REVIEW,APPROVED,REJECTED',
    current_review_level   VARCHAR(20)  NULL     COMMENT '当前审核层级: COLLEGE,SCHOOL,PROVINCE,NATIONAL',
    reject_reason          TEXT         NULL     COMMENT '驳回原因',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_event_id (event_id),
    INDEX idx_track_id (track_id),
    INDEX idx_status (status),
    CONSTRAINT fk_project_user  FOREIGN KEY (user_id)  REFERENCES user(id)  ON DELETE CASCADE,
    CONSTRAINT fk_project_event FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_track FOREIGN KEY (track_id) REFERENCES track(id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '项目';

-- ==================== 团队成员 ====================
CREATE TABLE team_member (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT      NOT NULL COMMENT '所属项目ID',
    name        VARCHAR(50) NOT NULL COMMENT '姓名',
    student_id  VARCHAR(30) NULL     COMMENT '学号',
    department  VARCHAR(100) NULL    COMMENT '院系',
    major       VARCHAR(100) NULL    COMMENT '专业',
    phone       VARCHAR(20) NULL     COMMENT '电话',
    member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '角色: LEADER,MEMBER',
    sort_order  INT         NOT NULL DEFAULT 0 COMMENT '排序',
    CONSTRAINT fk_member_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '团队成员';

-- ==================== 指导教师 ====================
CREATE TABLE advisor (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT      NOT NULL COMMENT '所属项目ID',
    name       VARCHAR(50) NOT NULL COMMENT '姓名',
    phone      VARCHAR(20) NULL     COMMENT '电话',
    CONSTRAINT fk_advisor_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '指导教师';

-- ==================== 项目材料 ====================
CREATE TABLE project_material (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    project_id        BIGINT       NOT NULL COMMENT '所属项目ID',
    material_type     VARCHAR(30)  NOT NULL COMMENT '材料类型: PITCH_PDF,PROJECT_PLAN_PDF,SUPPORTING_PDF,VIDEO_MP4',
    original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    stored_filename   VARCHAR(255) NOT NULL COMMENT '存储文件名',
    file_path         VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size         BIGINT       NOT NULL COMMENT '文件大小(字节)',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    CONSTRAINT fk_material_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '项目材料';

-- ==================== 评分标准 ====================
CREATE TABLE score_criteria (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    track_id    BIGINT       NOT NULL COMMENT '所属赛道ID',
    name        VARCHAR(100) NOT NULL COMMENT '评分维度名称',
    description VARCHAR(500) NULL     COMMENT '维度说明',
    max_score   INT          NOT NULL DEFAULT 100 COMMENT '最高分值',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sc_track (track_id),
    CONSTRAINT fk_sc_track FOREIGN KEY (track_id) REFERENCES track(id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '评分标准';

-- ==================== 评审分配 ====================
CREATE TABLE review_allocation (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT       NOT NULL COMMENT '项目ID',
    judge_id        BIGINT       NOT NULL COMMENT '评委用户ID',
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING, COMPLETED',
    overall_comment TEXT         NULL     COMMENT '总体评语',
    total_score     DECIMAL(10,1) NULL    COMMENT '总分',
    submitted_at    DATETIME     NULL     COMMENT '提交时间',
    allocated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ra_project (project_id),
    INDEX idx_ra_judge (judge_id),
    INDEX idx_ra_status (status),
    CONSTRAINT fk_ra_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_ra_judge FOREIGN KEY (judge_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_project_judge (project_id, judge_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '评审分配';

-- ==================== 评分记录 ====================
CREATE TABLE score_record (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    allocation_id   BIGINT       NOT NULL COMMENT '分配ID',
    criteria_id     BIGINT       NOT NULL COMMENT '评分维度ID',
    score           DECIMAL(5,1) NOT NULL COMMENT '得分',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sr_allocation (allocation_id),
    INDEX idx_sr_criteria (criteria_id),
    CONSTRAINT fk_sr_allocation FOREIGN KEY (allocation_id) REFERENCES review_allocation(id) ON DELETE CASCADE,
    CONSTRAINT fk_sr_criteria FOREIGN KEY (criteria_id) REFERENCES score_criteria(id) ON DELETE CASCADE,
    UNIQUE KEY uk_allocation_criteria (allocation_id, criteria_id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '评分记录';

-- ==================== 审核记录 ====================
CREATE TABLE review_log (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT      NOT NULL COMMENT '项目ID',
    reviewer_id   BIGINT      NOT NULL COMMENT '审核人用户ID',
    review_level  VARCHAR(20) NOT NULL COMMENT '审核层级: COLLEGE,SCHOOL,PROVINCE,NATIONAL',
    action        VARCHAR(20) NOT NULL COMMENT '操作: APPROVE,REJECT',
    reject_reason TEXT        NULL     COMMENT '驳回原因',
    reviewed_at   DATETIME    NOT NULL COMMENT '审核时间',
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rl_project (project_id),
    INDEX idx_rl_reviewer (reviewer_id),
    INDEX idx_rl_level (review_level),
    CONSTRAINT fk_rl_project FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
    CONSTRAINT fk_rl_reviewer FOREIGN KEY (reviewer_id) REFERENCES user(id) ON DELETE CASCADE
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '审核记录';

-- ==================== 赛事层级 & 晋级 ====================

-- 为已有 organization 表补充 is_external（若不存在）
SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'organization' AND COLUMN_NAME = 'is_external') = 0,
    'ALTER TABLE organization ADD COLUMN is_external TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否外部评委单位'' AFTER level',
    'SELECT ''is_external already exists'''
);
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 为已有 event 表补充 parent_event_id（若不存在）
SET @stmt = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'event' AND COLUMN_NAME = 'parent_event_id') = 0,
    'ALTER TABLE event ADD COLUMN parent_event_id BIGINT NULL COMMENT ''父赛事ID'' AFTER attachment_path',
    'SELECT ''parent_event_id already exists'''
);
PREPARE stmt FROM @stmt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

