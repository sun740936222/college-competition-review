package com.zhk.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhk.contest.dto.request.EventSaveRequest;
import com.zhk.contest.dto.request.TrackSaveRequest;
import com.zhk.contest.dto.response.EventResponse;
import com.zhk.contest.dto.response.TrackResponse;
import com.zhk.contest.entity.Event;
import com.zhk.contest.entity.Track;
import com.zhk.contest.entity.User;
import com.zhk.contest.enums.CompetitionLevel;
import com.zhk.contest.enums.EventStatus;
import com.zhk.contest.enums.OrganizationLevel;
import com.zhk.contest.enums.UserRole;
import com.zhk.contest.exception.BusinessException;
import com.zhk.contest.mapper.EventMapper;
import com.zhk.contest.mapper.OrganizationMapper;
import com.zhk.contest.mapper.TrackMapper;
import com.zhk.contest.mapper.UserMapper;
import com.zhk.contest.service.EventService;
import com.zhk.contest.util.AdminLevelResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class EventServiceImpl implements EventService {

    private final EventMapper eventMapper;
    private final TrackMapper trackMapper;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;

    public EventServiceImpl(EventMapper eventMapper, TrackMapper trackMapper,
                            UserMapper userMapper, OrganizationMapper organizationMapper) {
        this.eventMapper = eventMapper;
        this.trackMapper = trackMapper;
        this.userMapper = userMapper;
        this.organizationMapper = organizationMapper;
    }

    @Override
    public List<EventResponse> listOpenEvents(Long userId) {
        List<Event> events;
        if (userId != null) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getOrganizationId() != null) {
                List<Long> ancestorOrgIds = getAncestorOrgIds(user);
                events = eventMapper.selectList(new QueryWrapper<Event>().lambda()
                        .eq(Event::getStatus, EventStatus.OPEN)
                        .and(w -> w.in(Event::getOrganizationId, ancestorOrgIds)
                                .or().isNull(Event::getOrganizationId)));
            } else {
                events = eventMapper.selectList(new QueryWrapper<Event>().lambda()
                        .eq(Event::getStatus, EventStatus.OPEN)
                        .isNull(Event::getOrganizationId));
            }
        } else {
            events = eventMapper.selectList(new QueryWrapper<Event>().lambda()
                    .eq(Event::getStatus, EventStatus.OPEN)
                    .isNull(Event::getOrganizationId));
        }
        return events.stream().map(this::toDetailResponse).toList();
    }

    @Override
    public EventResponse getEventDetail(Long eventId) {
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        return toDetailResponse(event);
    }

    @Override
    @Transactional
    public EventResponse createEvent(Long userId, EventSaveRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");

        Long orgId = user.getOrganizationId();
        if (orgId == null) {
            // SUPER_ADMIN or users without org binding must specify organizationId in the request
            if (user.getRole() == UserRole.SUPER_ADMIN && request.getOrganizationId() != null) {
                orgId = request.getOrganizationId();
            } else {
                throw new BusinessException("您尚未绑定单位，无法创建赛事");
            }
        }

        Event event = new Event();
        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setSubmissionDeadline(request.getSubmissionDeadline());
        event.setReviewStartTime(request.getReviewStartTime());
        event.setReviewEndTime(request.getReviewEndTime());
        event.setRequireVideo(request.getRequireVideo() != null && request.getRequireVideo());
        event.setAllowResubmit(request.getAllowResubmit() != null && request.getAllowResubmit());
        event.setRequireComment(request.getRequireComment() != null && request.getRequireComment());
        event.setOrganizationId(orgId);

        if (request.getParentEventId() != null) {
            Event parent = eventMapper.selectById(request.getParentEventId());
            if (parent == null) throw new BusinessException("父赛事不存在");
            // Include both subtree and ancestor orgs: parent event is typically upstream
            List<Long> accessibleOrgIds = new java.util.ArrayList<>(getVisibleOrgIds(user));
            accessibleOrgIds.addAll(getAncestorOrgIds(user));
            if (parent.getOrganizationId() != null && !accessibleOrgIds.contains(parent.getOrganizationId())) {
                throw new BusinessException(403, "无权在该父赛事下创建子赛事");
            }
            event.setParentEventId(request.getParentEventId());
        }

        if (request.getCompetitionLevel() != null) {
            try {
                event.setCompetitionLevel(CompetitionLevel.valueOf(request.getCompetitionLevel()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的赛事级别");
            }
        }

        String status = request.getStatus();
        if (status != null) {
            try {
                event.setStatus(EventStatus.valueOf(status));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的赛事状态");
            }
        } else {
            event.setStatus(EventStatus.DRAFT);
        }

        eventMapper.insert(event);

        // Create tracks from selected templates
        if (request.getTracks() != null && !request.getTracks().isEmpty()) {
            createTracksForEvent(event.getId(), request.getTracks());
        }

        // Auto-create child events for selected orgs (e.g. national admin picks provinces)
        if (request.getChildEventOrgIds() != null && !request.getChildEventOrgIds().isEmpty()) {
            for (Long childOrgId : request.getChildEventOrgIds()) {
                var childOrg = organizationMapper.selectById(childOrgId);
                if (childOrg == null) continue;
                // Only create child events for orgs in admin's visible subtree
                if (!getVisibleOrgIds(user).contains(childOrgId)) continue;

                Event child = new Event();
                child.setName(request.getName() + "——" + childOrg.getName() + "赛区");
                child.setDescription(request.getDescription());
                child.setRegistrationDeadline(request.getRegistrationDeadline());
                child.setSubmissionDeadline(request.getSubmissionDeadline());
                child.setReviewStartTime(request.getReviewStartTime());
                child.setReviewEndTime(request.getReviewEndTime());
                child.setRequireVideo(request.getRequireVideo() != null && request.getRequireVideo());
                child.setAllowResubmit(request.getAllowResubmit() != null && request.getAllowResubmit());
                child.setRequireComment(request.getRequireComment() != null && request.getRequireComment());
                child.setOrganizationId(childOrgId);
                child.setParentEventId(event.getId());
                // Child competition level: one level below parent
                if (event.getCompetitionLevel() != null) {
                    CompetitionLevel parentLevel = event.getCompetitionLevel();
                    if (parentLevel == CompetitionLevel.NATIONAL) {
                        child.setCompetitionLevel(CompetitionLevel.PROVINCE);
                    } else if (parentLevel == CompetitionLevel.PROVINCE) {
                        child.setCompetitionLevel(CompetitionLevel.SCHOOL);
                    } else if (parentLevel == CompetitionLevel.SCHOOL) {
                        child.setCompetitionLevel(CompetitionLevel.COLLEGE);
                    } else {
                        child.setCompetitionLevel(parentLevel);
                    }
                }
                child.setStatus(event.getStatus());
                eventMapper.insert(child);

                // Copy tracks from parent to child
                if (request.getTracks() != null && !request.getTracks().isEmpty()) {
                    createTracksForEvent(child.getId(), request.getTracks());
                }
            }
        }

        return toDetailResponse(event);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long userId, Long eventId, EventSaveRequest request) {
        User user = checkAdminUser(userId);

        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        checkEventOwnership(user, event);

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setSubmissionDeadline(request.getSubmissionDeadline());
        event.setReviewStartTime(request.getReviewStartTime());
        event.setReviewEndTime(request.getReviewEndTime());
        event.setRequireVideo(request.getRequireVideo() != null && request.getRequireVideo());
        event.setAllowResubmit(request.getAllowResubmit() != null && request.getAllowResubmit());
        event.setRequireComment(request.getRequireComment() != null && request.getRequireComment());

        if (request.getCompetitionLevel() != null) {
            try {
                event.setCompetitionLevel(CompetitionLevel.valueOf(request.getCompetitionLevel()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的赛事级别");
            }
        }

        if (request.getStatus() != null) {
            try {
                event.setStatus(EventStatus.valueOf(request.getStatus()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的赛事状态");
            }
        }

        eventMapper.updateById(event);

        // Auto-create child events for newly selected orgs (skip orgs that already have a child)
        if (request.getChildEventOrgIds() != null && !request.getChildEventOrgIds().isEmpty()) {
            List<Event> existingChildren = eventMapper.selectList(
                    new QueryWrapper<Event>().lambda()
                            .eq(Event::getParentEventId, eventId));
            LinkedHashSet<Long> existingOrgIds = new LinkedHashSet<>();
            for (Event child : existingChildren) {
                if (child.getOrganizationId() != null) existingOrgIds.add(child.getOrganizationId());
            }

            List<Track> parentTracks = trackMapper.findByEventIdOrderBySortOrder(eventId);

            for (Long orgId : request.getChildEventOrgIds()) {
                if (existingOrgIds.contains(orgId)) continue;

                var childOrg = organizationMapper.selectById(orgId);
                if (childOrg == null) continue;
                if (!getVisibleOrgIds(user).contains(orgId)) continue;

                Event childEvent = new Event();
                childEvent.setName(event.getName() + "——" + childOrg.getName() + "赛区");
                childEvent.setDescription(event.getDescription());
                childEvent.setRegistrationDeadline(event.getRegistrationDeadline());
                childEvent.setSubmissionDeadline(event.getSubmissionDeadline());
                childEvent.setReviewStartTime(event.getReviewStartTime());
                childEvent.setReviewEndTime(event.getReviewEndTime());
                childEvent.setRequireVideo(event.getRequireVideo());
                childEvent.setAllowResubmit(event.getAllowResubmit());
                childEvent.setRequireComment(event.getRequireComment());
                childEvent.setOrganizationId(orgId);
                childEvent.setParentEventId(eventId);
                if (event.getCompetitionLevel() != null) {
                    CompetitionLevel parentLevel = event.getCompetitionLevel();
                    if (parentLevel == CompetitionLevel.NATIONAL) {
                        childEvent.setCompetitionLevel(CompetitionLevel.PROVINCE);
                    } else if (parentLevel == CompetitionLevel.PROVINCE) {
                        childEvent.setCompetitionLevel(CompetitionLevel.SCHOOL);
                    } else if (parentLevel == CompetitionLevel.SCHOOL) {
                        childEvent.setCompetitionLevel(CompetitionLevel.COLLEGE);
                    } else {
                        childEvent.setCompetitionLevel(parentLevel);
                    }
                }
                childEvent.setStatus(event.getStatus());
                eventMapper.insert(childEvent);

                for (Track track : parentTracks) {
                    Track childTrack = new Track();
                    childTrack.setEventId(childEvent.getId());
                    childTrack.setName(track.getName());
                    childTrack.setGroupName(track.getGroupName());
                    childTrack.setDescription(track.getDescription());
                    childTrack.setSortOrder(track.getSortOrder());
                    trackMapper.insert(childTrack);
                }
            }
        }

        return toDetailResponse(event);
    }

    @Override
    @Transactional
    public void deleteEvent(Long userId, Long eventId) {
        User user = checkAdminUser(userId);

        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        checkEventOwnership(user, event);

        eventMapper.deleteById(eventId);
    }

    @Override
    public IPage<EventResponse> listAllEvents(Long userId, Page<Event> page) {
        User user = checkAdminUser(userId);

        boolean isSuperAdmin = AdminLevelResolver.isSuperAdmin(user);
        List<Long> visibleOrgIds = getVisibleOrgIds(user);
        List<Long> ancestorOrgIds = AdminLevelResolver.getAncestorOrgIds(user, organizationMapper);
        // Merge visible (downward) and ancestor (upward) org ids
        LinkedHashSet<Long> allOrgIdSet = new LinkedHashSet<>(visibleOrgIds);
        allOrgIdSet.addAll(ancestorOrgIds);
        List<Long> allOrgIds = new ArrayList<>(allOrgIdSet);
        var wrapper = new QueryWrapper<Event>().lambda();
        if (!isSuperAdmin) {
            if (allOrgIds.isEmpty()) {
                wrapper.isNull(Event::getOrganizationId);
            } else {
                wrapper.in(Event::getOrganizationId, allOrgIds);
            }
        }
        // SUPER_ADMIN: no org filter, sees all events

        // Admin sees events at own level and one level below (e.g. PROVINCE sees PROVINCE + SCHOOL)
        // Also always include events from admin's visible org tree regardless of competitionLevel,
        // because competitionLevel serves as the review ceiling and projects stay in child events.
        // SUPER_ADMIN sees events at all levels — skip this filter
        if (!isSuperAdmin) {
            var adminLevel = AdminLevelResolver.getLevel(user, organizationMapper);
            if (adminLevel != null) {
                CompetitionLevel ownLevel = CompetitionLevel.valueOf(adminLevel.name());
                OrganizationLevel childLevel = adminLevel.nextDown();
                wrapper.and(w -> {
                    if (childLevel != null) {
                        CompetitionLevel childCompLevel = CompetitionLevel.valueOf(childLevel.name());
                        w.and(inner -> inner.eq(Event::getCompetitionLevel, ownLevel)
                                .or().eq(Event::getCompetitionLevel, childCompLevel));
                    } else {
                        w.and(inner -> inner.eq(Event::getCompetitionLevel, ownLevel));
                    }
                    if (!allOrgIds.isEmpty()) {
                        w.or().in(Event::getOrganizationId, allOrgIds);
                    }
                });
            }
        }
        return eventMapper.selectPage(page, wrapper).convert(this::toDetailResponse);
    }

    // ===== Track operations =====

    @Override
    public List<TrackResponse> getTracks(Long userId, Long eventId) {
        User user = checkAdminUser(userId);
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        checkEventOwnership(user, event);

        return trackMapper.findByEventIdOrderBySortOrder(eventId)
                .stream().map(this::toTrackResponse).toList();
    }

    @Override
    public List<TrackResponse> getTrackTemplates(Long userId) {
        checkAdminUser(userId);
        List<Track> allTracks = trackMapper.selectList(null);
        Set<String> seen = new LinkedHashSet<>();
        List<TrackResponse> result = new ArrayList<>();
        for (Track t : allTracks) {
            String key = (t.getName() != null ? t.getName() : "") + "||" +
                         (t.getGroupName() != null ? t.getGroupName() : "");
            if (seen.add(key)) {
                result.add(toTrackResponse(t));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public List<TrackResponse> saveTracks(Long userId, Long eventId, List<com.zhk.contest.dto.request.TrackSaveRequest> tracks) {
        User user = checkAdminUser(userId);
        Event event = eventMapper.selectById(eventId);
        if (event == null) throw new BusinessException("赛事不存在");
        checkEventOwnership(user, event);

        // Delete existing tracks for this event
        trackMapper.delete(new QueryWrapper<Track>().lambda().eq(Track::getEventId, eventId));

        // Insert new tracks
        int sort = 1;
        for (com.zhk.contest.dto.request.TrackSaveRequest t : tracks) {
            Track track = new Track();
            track.setEventId(eventId);
            track.setName(t.getName());
            track.setGroupName(t.getGroupName());
            track.setDescription(t.getDescription());
            track.setSortOrder(t.getSortOrder() != null ? t.getSortOrder() : sort++);
            trackMapper.insert(track);
        }

        return trackMapper.findByEventIdOrderBySortOrder(eventId)
                .stream().map(this::toTrackResponse).toList();
    }

    // ===== event hierarchy =====

    @Override
    public List<EventResponse> listChildren(Long userId, Long eventId) {
        User user = checkAdminUser(userId);
        Event parent = eventMapper.selectById(eventId);
        if (parent == null) throw new BusinessException("赛事不存在");
        checkEventOwnership(user, parent);

        List<Event> children = eventMapper.selectList(
                new QueryWrapper<Event>().lambda().eq(Event::getParentEventId, eventId));
        return children.stream().map(this::toDetailResponse).toList();
    }

    @Override
    @Transactional
    public void deleteChildren(Long userId, Long eventId) {
        User user = checkAdminUser(userId);
        Event parent = eventMapper.selectById(eventId);
        if (parent == null) throw new BusinessException("赛事不存在");
        checkEventOwnership(user, parent);

        List<Event> children = eventMapper.selectList(
                new QueryWrapper<Event>().lambda().eq(Event::getParentEventId, eventId));
        for (Event child : children) {
            eventMapper.deleteById(child.getId());
        }
    }

    // ===== helpers =====

    private User checkAdminUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(401, "用户不存在");
        return user;
    }

    private void checkEventOwnership(User user, Event event) {
        if (event.getOrganizationId() != null) {
            List<Long> visibleOrgIds = getVisibleOrgIds(user);
            if (!visibleOrgIds.contains(event.getOrganizationId())) {
                throw new BusinessException(403, "无权编辑该赛事");
            }
        }
    }

    private List<Long> getAncestorOrgIds(User user) {
        return AdminLevelResolver.getAncestorOrgIds(user, organizationMapper);
    }

    private List<Long> getVisibleOrgIds(User user) {
        return AdminLevelResolver.getVisibleOrgIds(user, organizationMapper);
    }

    private EventResponse toDetailResponse(Event event) {
        EventResponse resp = toEventResponse(event);
        resp.setTracks(trackMapper.findByEventIdOrderBySortOrder(event.getId())
                .stream().map(this::toTrackResponse).toList());
        return resp;
    }

    private EventResponse toEventResponse(Event event) {
        EventResponse resp = new EventResponse();
        resp.setId(event.getId());
        resp.setName(event.getName());
        resp.setDescription(event.getDescription());
        resp.setRegistrationDeadline(event.getRegistrationDeadline());
        resp.setSubmissionDeadline(event.getSubmissionDeadline());
        resp.setReviewStartTime(event.getReviewStartTime());
        resp.setReviewEndTime(event.getReviewEndTime());
        resp.setRequireVideo(event.getRequireVideo());
        resp.setAllowResubmit(event.getAllowResubmit());
        resp.setRequireComment(event.getRequireComment());
        resp.setAttachmentName(event.getAttachmentName());
        resp.setCompetitionLevel(event.getCompetitionLevel() != null
                ? event.getCompetitionLevel().name() : null);
        resp.setStatus(event.getStatus() != null ? event.getStatus().name() : null);
        resp.setParentEventId(event.getParentEventId());
        if (event.getParentEventId() != null) {
            Event parent = eventMapper.selectById(event.getParentEventId());
            resp.setParentEventName(parent != null ? parent.getName() : null);
        }
        Long childCount = eventMapper.selectCount(
                new QueryWrapper<Event>().lambda().eq(Event::getParentEventId, event.getId()));
        resp.setChildEventCount(childCount != null ? childCount.intValue() : 0);
        return resp;
    }

    private void createTracksForEvent(Long eventId, List<TrackSaveRequest> tracks) {
        int sort = 1;
        for (TrackSaveRequest t : tracks) {
            Track track = new Track();
            track.setEventId(eventId);
            track.setName(t.getName());
            track.setGroupName(t.getGroupName());
            track.setDescription(t.getDescription());
            track.setSortOrder(sort++);
            trackMapper.insert(track);
        }
    }

    private TrackResponse toTrackResponse(Track track) {
        TrackResponse resp = new TrackResponse();
        resp.setId(track.getId());
        resp.setName(track.getName());
        resp.setGroupName(track.getGroupName());
        resp.setDescription(track.getDescription());
        return resp;
    }
}
