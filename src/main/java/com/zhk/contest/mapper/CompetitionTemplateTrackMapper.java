package com.zhk.contest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhk.contest.entity.CompetitionTemplateTrack;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CompetitionTemplateTrackMapper extends BaseMapper<CompetitionTemplateTrack> {

    @Select("SELECT * FROM competition_template_track WHERE template_id = #{templateId} ORDER BY sort_order ASC")
    List<CompetitionTemplateTrack> findByTemplateId(Long templateId);
}
