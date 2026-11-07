package com.zhk.contest.service;

import com.zhk.contest.dto.response.CompetitionTemplateResponse;

import java.util.List;

public interface CompetitionTemplateService {
    List<CompetitionTemplateResponse> listAllTemplates();
}
