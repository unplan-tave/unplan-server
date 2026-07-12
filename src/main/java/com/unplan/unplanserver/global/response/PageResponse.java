package com.unplan.unplanserver.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> data;
    private PaginationInfo pagination;

    public List<T> data() {
        return data;
    }

    public PaginationInfo pagination() {
        return pagination;
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return of(page.getContent(), page);
    }

    public static <T> PageResponse<T> of(List<T> content, Page<?> page) {
        return new PageResponse<>(
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

        public int page() {
            return page;
        }

        public int size() {
            return size;
        }

        public long totalElements() {
            return totalElements;
        }

        public int totalPages() {
            return totalPages;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public boolean hasPrevious() {
            return hasPrevious;
        }

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
