package com.kamsarobar.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Builds bounded page requests so a client can never ask for an unbounded result set.
 */
public final class Pages {

    public static final int MAX_PAGE_SIZE = 50;

    private Pages() {
    }

    public static Pageable of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), clamp(size));
    }

    public static Pageable of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), clamp(size), sort);
    }

    private static int clamp(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }
}
