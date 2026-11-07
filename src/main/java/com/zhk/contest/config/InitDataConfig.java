package com.zhk.contest.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhk.contest.entity.CompetitionTemplate;
import com.zhk.contest.entity.CompetitionTemplateTrack;
import com.zhk.contest.entity.Organization;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.mapper.CompetitionTemplateMapper;
import com.zhk.contest.mapper.CompetitionTemplateTrackMapper;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.zhk.contest.util.SecurityUtils;

import java.util.List;

@Component
public class InitDataConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InitDataConfig.class);

    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;
    private final CompetitionTemplateMapper templateMapper;
    private final CompetitionTemplateTrackMapper templateTrackMapper;

    /**
     * 初始化账号凭据一律来自环境变量（application.properties 的 app.init.* 占位符），
     * 严禁在代码中写死明文。未配置时自动生成随机密码并打印到日志（仅首次创建时）。
     */
    private final String superAdminUsername;
    private final String superAdminPassword;
    private final String defaultAdminPassword;

    private static final String DEFAULT_SUPER_ADMIN_USERNAME = "superadmin";

    public InitDataConfig(OrganizationMapper organizationMapper,
                          UserMapper userMapper,
                          CompetitionTemplateMapper templateMapper,
                          CompetitionTemplateTrackMapper templateTrackMapper,
                          @Value("${app.init.super-admin-username:}") String superAdminUsername,
                          @Value("${app.init.super-admin-password:}") String superAdminPassword,
                          @Value("${app.init.default-admin-password:}") String defaultAdminPassword) {
        this.organizationMapper = organizationMapper;
        this.userMapper = userMapper;
        this.templateMapper = templateMapper;
        this.templateTrackMapper = templateTrackMapper;
        this.superAdminUsername = superAdminUsername;
        this.superAdminPassword = superAdminPassword;
        this.defaultAdminPassword = defaultAdminPassword;
    }

    /** 生成随机初始密码（12位），用于未通过环境变量提供凭据时的兜底 */
    private static String generateRandomPassword() {
        return SecurityUtils.generateRandomPassword(12);
    }

    @Override
    public void run(String... args) {
        seedCompetitionTemplates();
        ensureSuperAdminExists();

        long nationalCount = organizationMapper.selectCount(
                new QueryWrapper<Organization>().lambda()
                        .eq(Organization::getLevel, OrganizationLevel.NATIONAL));

        if (nationalCount == 0) {
            log.info("============================================");
            log.info("  未检测到任何国赛组委会。");
            log.info("  请通过注册页面创建首个国赛组委会账号。");
            log.info("  POST /api/auth/register/national");
            log.info("============================================");
        } else {
            ensureAdminsExist();
            logAccounts();
        }
    }

    private void ensureSuperAdminExists() {
        long count = userMapper.selectCount(
                new QueryWrapper<User>().lambda()
                        .eq(User::getRole, UserRole.SUPER_ADMIN));
        if (count == 0) {
            String username = (superAdminUsername != null && !superAdminUsername.isBlank())
                    ? superAdminUsername : DEFAULT_SUPER_ADMIN_USERNAME;
            boolean generated = (superAdminPassword == null || superAdminPassword.isBlank());
            String password = generated ? generateRandomPassword() : superAdminPassword;

            User superAdmin = new User();
            superAdmin.setUsername(username);
            superAdmin.setPassword(SecurityUtils.hashPassword(password));
            superAdmin.setRealName("超级管理员");
            superAdmin.setRole(UserRole.SUPER_ADMIN);
            superAdmin.setPhone("10000000000");
            userMapper.insert(superAdmin);

            log.info("============================================");
            log.info("  超级管理员账号已自动创建");
            log.info("  用户名: {}", username);
            if (generated) {
                log.warn("  未配置 INIT_SUPER_ADMIN_PASSWORD，已生成随机密码（请立即登录修改）：{}", password);
            } else {
                log.info("  密码: 见环境变量 INIT_SUPER_ADMIN_PASSWORD");
            }
            log.info("============================================");
        }
    }

    private void ensureAdminsExist() {
        List<Organization> orgs = organizationMapper.selectList(
                new QueryWrapper<Organization>().lambda()
                        .in(Organization::getLevel, OrganizationLevel.SCHOOL, OrganizationLevel.PROVINCE, OrganizationLevel.NATIONAL));

        for (Organization org : orgs) {
            long adminCount = userMapper.selectCount(
                    new QueryWrapper<User>().lambda()
                            .eq(User::getOrganizationId, org.getId())
                            .eq(User::getRole, UserRole.ADMIN));
            if (adminCount == 0) {
                String code = org.getInviteCode();
                String suffix = (code != null && code.contains("-"))
                        ? code.split("-")[1].toLowerCase() : org.getId().toString();
                boolean generated = (defaultAdminPassword == null || defaultAdminPassword.isBlank());
                String password = generated ? generateRandomPassword() : defaultAdminPassword;

                User admin = new User();
                admin.setUsername("admin_" + suffix);
                admin.setPassword(SecurityUtils.hashPassword(password));
                admin.setRealName(org.getName() + "管理员");
                admin.setRole(UserRole.ADMIN);
                admin.setOrganizationId(org.getId());
                userMapper.insert(admin);

                if (generated) {
                    log.warn("  组织[{}] 管理员 {} 已创建，随机初始密码（请尽快修改）：{}",
                            org.getName(), admin.getUsername(), password);
                }
            }
        }
    }

    private void logAccounts() {
        List<Organization> nationalOrgs = organizationMapper.selectList(
                new QueryWrapper<Organization>().lambda()
                        .eq(Organization::getLevel, OrganizationLevel.NATIONAL));

        log.info("============================================");
        log.info("【已注册国赛组委会】共 {} 个", nationalOrgs.size());
        for (Organization org : nationalOrgs) {
            List<User> admins = userMapper.selectList(
                    new QueryWrapper<User>().lambda()
                            .eq(User::getOrganizationId, org.getId())
                            .eq(User::getRole, UserRole.ADMIN)
                            .last("LIMIT 1"));
            if (!admins.isEmpty()) {
                User admin = admins.get(0);
                log.info("  {} — 管理员: {} (邀请码: {})",
                        org.getName(), admin.getUsername(), org.getInviteCode());
            }
        }
        log.info("============================================");
    }

    // ===== Competition Templates =====

    private void seedCompetitionTemplates() {
        if (templateMapper.selectCount(null) > 0) return;

        log.info("========== 初始化赛事模板数据 ==========");

        Long t1 = insertTemplate("中国国际大学生创新大赛(2026)", "教育部主办的国家级创新创业赛事", 1);
        insertTemplateTrack(t1, "高教主赛道", "本科生创意组", "项目尚未完成工商注册", 1);
        insertTemplateTrack(t1, "高教主赛道", "本科生创业组", "已完成工商注册", 2);
        insertTemplateTrack(t1, "高教主赛道", "研究生创意组", "研究生创意组", 3);
        insertTemplateTrack(t1, "高教主赛道", "研究生创业组", "研究生创业组", 4);
        insertTemplateTrack(t1, "青年红色筑梦之旅赛道", "公益组", "不以营利为目标", 5);
        insertTemplateTrack(t1, "青年红色筑梦之旅赛道", "创意组", "解决农业农村和城乡社区发展问题", 6);
        insertTemplateTrack(t1, "青年红色筑梦之旅赛道", "创业组", "商业手段助力乡村振兴", 7);
        insertTemplateTrack(t1, "产业赛道", "企业命题组", "企业出题师生答题", 8);
        insertTemplateTrack(t1, "产业赛道", "成果转化组", "高校科研成果转化", 9);
        insertTemplateTrack(t1, "职教赛道", null, "职业院校学生技能创新", 10);
        log.info("  1. 中国国际大学生创新大赛(2026) — 10个赛道组别");

        Long t2 = insertTemplate("全国大学生职业规划大赛", "帮助学生明确职业方向", 2);
        insertTemplateTrack(t2, "成长赛道", null, "面向大一、大二学生", 1);
        insertTemplateTrack(t2, "就业赛道", null, "面向大三、大四学生", 2);
        log.info("  2. 全国大学生职业规划大赛 — 2个赛道");

        Long t3 = insertTemplate("\"挑战杯\"中国大学生创业计划竞赛(2026)", "共青团中央等单位主办", 3);
        insertTemplateTrack(t3, "先进制造", null, "重大技术装备、航空航天等", 1);
        insertTemplateTrack(t3, "新一代信息技术", null, "集成电路、AI等", 2);
        insertTemplateTrack(t3, "生物医药与健康科技", null, "脑机接口、生物制造等", 3);
        insertTemplateTrack(t3, "新能源新材料", null, "绿色能源、先进材料等", 4);
        insertTemplateTrack(t3, "现代农业与食品科技", null, "科技农业、生物育种等", 5);
        insertTemplateTrack(t3, "新消费与文化创意", null, "国潮文创、沉浸式消费等", 6);
        insertTemplateTrack(t3, "现代服务与社会治理", null, "智慧城市、社会服务等", 7);
        log.info("  3. 挑战杯 — 7个赛道");

        log.info("========== 赛事模板初始化完成 ==========");
    }

    private Long insertTemplate(String name, String description, int sortOrder) {
        CompetitionTemplate t = new CompetitionTemplate();
        t.setName(name);
        t.setDescription(description);
        t.setSortOrder(sortOrder);
        templateMapper.insert(t);
        return t.getId();
    }

    private void insertTemplateTrack(Long templateId, String name, String groupName, String description, int sortOrder) {
        CompetitionTemplateTrack t = new CompetitionTemplateTrack();
        t.setTemplateId(templateId);
        t.setName(name);
        t.setGroupName(groupName);
        t.setDescription(description);
        t.setSortOrder(sortOrder);
        templateTrackMapper.insert(t);
    }
}
