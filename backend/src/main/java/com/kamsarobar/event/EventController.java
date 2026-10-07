package com.kamsarobar.event;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kamsarobar.common.web.PageResponse;
import com.kamsarobar.common.web.Pages;
import com.kamsarobar.post.dto.PostResponse;
import com.kamsarobar.security.UserPrincipal;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** The member's "My upcoming events" list, soonest first. */
    @GetMapping("/events/mine")
    public PageResponse<PostResponse> mine(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @AuthenticationPrincipal UserPrincipal viewer) {
        return eventService.myUpcoming(viewer, Pages.of(page, size));
    }

    /** Upcoming events and seminars in a city (or every city when cityId is omitted). */
    @GetMapping("/events/upcoming")
    public PageResponse<PostResponse> upcoming(@RequestParam(required = false) Long cityId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @AuthenticationPrincipal UserPrincipal viewer) {
        return eventService.upcoming(cityId, viewer, Pages.of(page, size));
    }

    /** Add the event to my upcoming events (idempotent). */
    @PutMapping("/posts/{id}/attendance")
    public PostResponse attend(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal viewer) {
        return eventService.attend(id, viewer);
    }

    @DeleteMapping("/posts/{id}/attendance")
    public PostResponse leave(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal viewer) {
        return eventService.leave(id, viewer);
    }
}
