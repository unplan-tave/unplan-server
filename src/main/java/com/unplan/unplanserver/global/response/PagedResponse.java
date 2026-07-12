package com.unplan.unplanserver.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PagedResponse<T> {

    private static final String SUCCESS_MESSAGE = "요청 성공";

    private boolean success;
    private String message;
    private List<T> data;
    private PaginationInfo pagination;

    public static <T> PagedResponse<T> of(Page<T> page) {
        return of(page.getContent(), page);
    }

    public static <T> PagedResponse<T> of(List<T> content, Page<?> page) {
        return new PagedResponse<>(
                true,
                SUCCESS_MESSAGE,
                content,
                PaginationInfo.from(page)
        );
    }

    @Getter
    @AllArgsConstructor
    public static class PaginationInfo {

        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;

        private static PaginationInfo from(Page<?> page) {
            return new PaginationInfo(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.hasNext(),
                    page.hasPrevious()
            );
        }
    }
}
