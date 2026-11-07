package com.zhk.contest.service.impl;

import com.zhk.contest.dto.response.CompetitionTemplateResponse;
import com.zhk.contest.dto.response.TrackResponse;
import com.zhk.contest.entity.CompetitionTemplate;
import com.zhk.contest.entity.CompetitionTemplateTrack;
import com.zhk.contest.mapper.CompetitionTemplateMapper;
import com.zhk.contest.mapper.CompetitionTemplateTrackMapper;
import com.zhk.contest.service.CompetitionTemplateService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CompetitionTemplateServiceImpl implements CompetitionTemplateService {

    private final CompetitionTemplateMapper templateMapper;
    private final CompetitionTemplateTrackMapper templateTrackMapper;

    public CompetitionTemplateServiceImpl(CompetitionTemplateMapper templateMapper,
                                          CompetitionTemplateTrackMapper templateTrackMapper) {
        this.templateMapper = templateMapper;
        this.templateTrackMapper = templateTrackMapper;
    }

    @Override
    public List<CompetitionTemplateResponse> listAllTemplates() {
        List<CompetitionTemplate> templates = templateMapper.selectList(null);
        if (templates.isEmpty()) return List.of();

        // Load all tracks in one query, group by templateId
        List<CompetitionTemplateTrack> allTracks = templateTrackMapper.selectList(null);
        Map<Long, List<CompetitionTemplateTrack>> tracksByTemplate = allTracks.stream()
                .collect(Collectors.groupingBy(CompetitionTemplateTrack::getTemplateId));

        return templates.stream().map(t -> toResponse(t, tracksByTemplate.getOrDefault(t.getId(), List.of()))).toList();
    }

    private CompetitionTemplateResponse toResponse(CompetitionTemplate template, List<CompetitionTemplateTrack> tracks) {
        CompetitionTemplateResponse resp = new CompetitionTemplateResponse();
        resp.setId(template.getId());
        resp.setName(template.getName());
        resp.setDescription(template.getDescription());

        resp.setTracks(tracks.stream().map(t -> {
            TrackResponse tr = new TrackResponse();
            tr.setId(t.getId());
            tr.setName(t.getName());
            tr.setGroupName(t.getGroupName());
            tr.setDescription(t.getDescription());
            return tr;
        }).toList());

        return resp;
    }
}
