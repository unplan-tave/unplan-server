package com.unplan.unplanserver.global.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class PagingUtilsTest {

    @Test
    void pageRequestUsesFirstPageAndDefaultSizeWhenPageIsNull() {
        PageRequest pageRequest = PagingUtils.pageRequest(null);

        assertThat(pageRequest.getPageNumber()).isZero();
        assertThat(pageRequest.getPageSize()).isEqualTo(30);
    }

    @Test
    void pageRequestUsesFirstPageAndDefaultSizeWhenPageIsNegative() {
        PageRequest pageRequest = PagingUtils.pageRequest(-1);

        assertThat(pageRequest.getPageNumber()).isZero();
        assertThat(pageRequest.getPageSize()).isEqualTo(30);
    }

    @Test
    void pageRequestUsesRequestedPageAndDefaultSize() {
        PageRequest pageRequest = PagingUtils.pageRequest(2);

        assertThat(pageRequest.getPageNumber()).isEqualTo(2);
        assertThat(pageRequest.getPageSize()).isEqualTo(30);
    }
}
