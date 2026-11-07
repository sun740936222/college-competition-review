package com.zhk.contest.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhk.contest.enums.MemberRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("team_member")
public class TeamMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("project_id")
    private Long projectId;

    private String name;

    @TableField("student_id")
    private String studentId;

    private String department;

    private String major;

    private String phone;

    @TableField("member_role")
    private MemberRole memberRole = MemberRole.MEMBER;

    @TableField("sort_order")
    private Integer sortOrder = 0;
}