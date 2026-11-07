package com.zhk.contest.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.EventSaveRequest;
import com.zhk.contest.dto.request.TrackSaveRequest;
import com.zhk.contest.dto.response.EventResponse;
import com.zhk.contest.dto.response.TrackResponse;
import com.zhk.contest.entity.Event;

import java.util.List;

public interface EventService {
    List<EventResponse> listOpenEvents(Long userId);
    EventResponse getEventDetail(Long eventId);

    // Admin operations
    EventResponse createEvent(Long userId, EventSaveRequest request);
    EventResponse updateEvent(Long userId, Long eventId, EventSaveRequest request);
    void deleteEvent(Long userId, Long eventId);
    IPage<EventResponse> listAllEvents(Long userId, Page<Event> page);

    // Event hierarchy
    List<EventResponse> listChildren(Long userId, Long eventId);
    void deleteChildren(Long userId, Long eventId);

    // Track operations
    List<TrackResponse> getTracks(Long userId, Long eventId);
    List<TrackResponse> saveTracks(Long userId, Long eventId, List<TrackSaveRequest> tracks);
    List<TrackResponse> getTrackTemplates(Long userId);
}
