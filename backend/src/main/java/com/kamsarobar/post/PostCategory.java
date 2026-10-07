package com.kamsarobar.post;

public enum PostCategory {
    GENERAL,
    JOB_OPENING,
    HELP_NEEDED,
    EVENT,
    SEMINAR,
    ANNOUNCEMENT;

    /** Events and seminars have a date and can be added to a member's "My upcoming events" list. */
    public boolean isEvent() {
        return this == EVENT || this == SEMINAR;
    }
}
