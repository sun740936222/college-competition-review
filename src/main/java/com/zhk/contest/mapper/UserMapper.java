package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM user WHERE username = #{username}")
    Optional<User> findByUsername(String username);

    @Select("SELECT COUNT(1) FROM user WHERE username = #{username}")
    boolean existsByUsername(String username);

    /** Find all JUDGE users within the given org IDs (or with null org). */
    @Select("<script>" +
            "SELECT * FROM user WHERE role = 'JUDGE'" +
            "<if test='orgIds != null and orgIds.size() > 0'>" +
            "  AND (organization_id IN " +
            "    <foreach collection='orgIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "    OR organization_id IS NULL)" +
            "</if>" +
            "</script>")
    List<User> findJudgesByOrgIds(@Param("orgIds") List<Long> orgIds);
}