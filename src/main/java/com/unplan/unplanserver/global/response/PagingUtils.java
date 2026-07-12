package com.unplan.unplanserver.global.response;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PagingUtils {

    public static final int DEFAULT_PAGE_SIZE = 30;

    private PagingUtils() {
    }

    public static PageRequest pageRequest(Integer page) {
        return PageRequest.of(resolvePage(page), DEFAULT_PAGE_SIZE);
    }

    public static PageRequest pageRequest(Integer page, Sort sort) {
        return PageRequest.of(resolvePage(page), DEFAULT_PAGE_SIZE, sort);
    }

    private static int resolvePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }
}
