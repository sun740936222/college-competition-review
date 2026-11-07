package com.zhk.contest.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.EventSaveRequest;
import com.zhk.contest.dto.request.TrackSaveRequest;
import com.zhk.contest.dto.response.ApiResponse;
import com.zhk.contest.dto.response.CompetitionTemplateResponse;
import com.zhk.contest.dto.response.EventResponse;
import com.zhk.contest.dto.response.TrackResponse;
import com.zhk.contest.entity.Event;
import com.zhk.contest.service.CompetitionTemplateService;
import com.zhk.contest.service.EventService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final CompetitionTemplateService templateService;

    public EventController(EventService eventService, CompetitionTemplateService templateService) {
        this.eventService = eventService;
        this.templateService = templateService;
    }

    /** Public: list open events for registration */
    @GetMapping
    public ApiResponse<List<EventResponse>> list(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return ApiResponse.success(eventService.listOpenEvents(userId));
    }

    /** Public: get event detail */
    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> detail(@PathVariable Long eventId) {
        return ApiResponse.success(eventService.getEventDetail(eventId));
    }

    // ========== Admin endpoints ==========

    /** Admin: list all events (including non-open, paginated) */
    @GetMapping("/admin/all")
    public ApiResponse<IPage<EventResponse>> listAll(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(eventService.listAllEvents(userId, new Page<>(current, size)));
    }

    /** Admin: create event */
    @PostMapping("/admin")
    public ApiResponse<EventResponse> create(@Valid @RequestBody EventSaveRequest request, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(eventService.createEvent(userId, request));
    }

    /** Admin: update event */
    @PutMapping("/admin/{eventId}")
    public ApiResponse<EventResponse> update(@PathVariable Long eventId,
                                              @Valid @RequestBody EventSaveRequest request,
                                              HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(eventService.updateEvent(userId, eventId, request));
    }

    /** Admin: delete event */
    @DeleteMapping("/admin/{eventId}")
    public ApiResponse<Void> delete(@PathVariable Long eventId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        eventService.deleteEvent(userId, eventId);
        return ApiResponse.success(null);
    }

    /** Admin: list child events */
    @GetMapping("/admin/{eventId}/children")
    public ApiResponse<List<EventResponse>> listChildren(@PathVariable Long eventId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(eventService.listChildren(userId, eventId));
    }

    /** Admin: delete all child events */
    @DeleteMapping("/admin/{eventId}/children")
    public ApiResponse<Void> deleteChildren(@PathVariable Long eventId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        eventService.deleteChildren(userId, eventId);
        return ApiResponse.success(null);
    }

    // ========== Competition templates ==========

    /** Admin: list all competition templates with their tracks */
    @GetMapping("/admin/competition-templates")
    public ApiResponse<List<CompetitionTemplateResponse>> getCompetitionTemplates() {
        return ApiResponse.success(templateService.listAllTemplates());
    }

    // ========== Track endpoints ==========

    /** Admin: get track templates (distinct name+group_name from all events) */
    @GetMapping("/admin/track-templates")
    public ApiResponse<List<TrackResponse>> getTrackTemplates(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(eventService.getTrackTemplates(userId));
    }

    /** Admin: get tracks for an event */
    @GetMapping("/admin/{eventId}/tracks")
    public ApiResponse<List<TrackResponse>> getTracks(@PathVariable Long eventId, HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(eventService.getTracks(userId, eventId));
    }

    /** Admin: save tracks for an event (replaces all) */
    @PutMapping("/admin/{eventId}/tracks")
    public ApiResponse<List<TrackResponse>> saveTracks(@PathVariable Long eventId,
                                                        @RequestBody List<TrackSaveRequest> tracks,
                                                        HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("adminUserId");
        return ApiResponse.success(eventService.saveTracks(userId, eventId, tracks));
    }
}
