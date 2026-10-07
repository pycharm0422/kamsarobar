package com.kamsarobar.user;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** What a member wants push notifications about. */
@Embeddable
public class NotificationSettings {

    /** New posts by members of my city. */
    @Column(name = "notify_city_posts", nullable = false)
    private boolean cityPosts = true;

    /** New events and seminars in my city. */
    @Column(name = "notify_city_events", nullable = false)
    private boolean cityEvents = true;

    /** Events and seminars from other cities that are shared with everyone. */
    @Column(name = "notify_all_events", nullable = false)
    private boolean allEvents = false;

    /** "Starting soon" reminders for events in My upcoming events. */
    @Column(name = "notify_event_reminders", nullable = false)
    private boolean eventReminders = true;

    public boolean isCityPosts() {
        return cityPosts;
    }

    public boolean isCityEvents() {
        return cityEvents;
    }

    public boolean isAllEvents() {
        return allEvents;
    }

    public boolean isEventReminders() {
        return eventReminders;
    }

    public void update(boolean cityPosts, boolean cityEvents, boolean allEvents, boolean eventReminders) {
        this.cityPosts = cityPosts;
        this.cityEvents = cityEvents;
        this.allEvents = allEvents;
        this.eventReminders = eventReminders;
    }
}
