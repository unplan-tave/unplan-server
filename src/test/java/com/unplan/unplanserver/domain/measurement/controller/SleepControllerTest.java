package com.unplan.unplanserver.domain.measurement.controller;

import com.unplan.unplanserver.domain.measurement.dto.response.SleepResponse;
import com.unplan.unplanserver.domain.measurement.service.SleepService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import com.unplan.unplanserver.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SleepControllerTest {

    private SleepService sleepService;
    private MockMvc mockMvc;
    private final Long authenticatedMemberId = 1L;

    @BeforeEach
    void setUp() {
        sleepService = mock(SleepService.class);
        mockMvc = standaloneSetup(new SleepController(sleepService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalArgumentResolver(authenticatedMemberId))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSleepReturnsAuthenticatedMembersSleep() throws Exception {
        Long memberId = 1L;
        Long sleepId = 45L;
        SleepResponse response = SleepResponse.builder()
                .sleepId(sleepId)
                .durationMinutes(450)
                .bedTime(LocalDateTime.of(2026, 6, 23, 23, 0))
                .wakeUpTime(LocalDateTime.of(2026, 6, 24, 7, 30))
                .isNap(false)
                .createdAt(LocalDateTime.of(2026, 6, 24, 22, 15))
                .build();

        when(sleepService.getSleep(memberId, sleepId)).thenReturn(response);

        mockMvc.perform(get("/sleeps/{sleepId}", sleepId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청 성공"))
                .andExpect(jsonPath("$.data.sleepId").value(45))
                .andExpect(jsonPath("$.data.durationMinutes").value(450))
                .andExpect(jsonPath("$.data.bedTime").value("2026-06-23T23:00:00"))
                .andExpect(jsonPath("$.data.wakeUpTime").value("2026-06-24T07:30:00"))
                .andExpect(jsonPath("$.data.isNap").value(false))
                .andExpect(jsonPath("$.data.createdAt").value("2026-06-24T22:15:00"));
    }

    @Test
    void getSleepReturnsNotFoundWhenSleepDoesNotBelongToMember() throws Exception {
        Long memberId = 1L;
        Long sleepId = 45L;

        when(sleepService.getSleep(memberId, sleepId))
                .thenThrow(new CustomException(ErrorCode.SLEEP_NOT_FOUND));

        mockMvc.perform(get("/sleeps/{sleepId}", sleepId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 수면 기록입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private record TestAuthenticationPrincipalArgumentResolver(Long memberId)
            implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            return memberId;
        }
    }
}
