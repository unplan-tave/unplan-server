package com.unplan.unplanserver.global.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PagedResponseTest {

    @Test
    void ofPageReturnsContentAndPagination() {
        Page<String> page = new PageImpl<>(
                List.of("a", "b"),
                PagingUtils.pageRequest(0),
                100
        );

        PagedResponse<String> response = PagedResponse.of(page);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("요청 성공");
        assertThat(response.getData()).containsExactly("a", "b");
        assertThat(response.getPagination().getPage()).isZero();
        assertThat(response.getPagination().getSize()).isEqualTo(30);
        assertThat(response.getPagination().getTotalElements()).isEqualTo(100);
        assertThat(response.getPagination().getTotalPages()).isEqualTo(4);
        assertThat(response.getPagination().isHasNext()).isTrue();
        assertThat(response.getPagination().isHasPrevious()).isFalse();
    }

    @Test
    void ofContentAndPageReturnsConvertedContentAndOriginalPagination() {
        Page<Integer> page = new PageImpl<>(
                List.of(1, 2),
                PagingUtils.pageRequest(2),
                100
        );
        List<String> content = List.of("one", "two");

        PagedResponse<String> response = PagedResponse.of(content, page);

        assertThat(response.getData()).containsExactly("one", "two");
        assertThat(response.getPagination().getPage()).isEqualTo(2);
        assertThat(response.getPagination().getSize()).isEqualTo(30);
        assertThat(response.getPagination().getTotalElements()).isEqualTo(100);
        assertThat(response.getPagination().getTotalPages()).isEqualTo(4);
        assertThat(response.getPagination().isHasNext()).isTrue();
        assertThat(response.getPagination().isHasPrevious()).isTrue();
    }
}
