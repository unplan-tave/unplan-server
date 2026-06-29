package com.unplan.unplanserver.domain.measurement.controller;

import com.unplan.unplanserver.domain.measurement.dto.response.ConditionResponse;
import com.unplan.unplanserver.domain.measurement.service.ConditionService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import com.unplan.unplanserver.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ConditionControllerTest {

    private ConditionService conditionService;
    private MockMvc mockMvc;
    private final Long authenticatedMemberId = 1L;

    @BeforeEach
    void setUp() {
        conditionService = mock(ConditionService.class);
        mockMvc = standaloneSetup(new ConditionController(conditionService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalArgumentResolver(authenticatedMemberId))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getConditionReturnsAuthenticatedMembersCondition() throws Exception {
        Long memberId = 1L;
        Long conditionId = 12L;
        ConditionResponse response = ConditionResponse.builder()
                .conditionId(conditionId)
                .bodyScore(4)
                .mindScore(3)
                .dateTime(LocalDateTime.of(2026, 6, 24, 22, 2))
                .build();

        when(conditionService.getCondition(memberId, conditionId)).thenReturn(response);

        mockMvc.perform(get("/conditions/{conditionId}", conditionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청 성공"))
                .andExpect(jsonPath("$.data.conditionId").value(12))
                .andExpect(jsonPath("$.data.bodyScore").value(4))
                .andExpect(jsonPath("$.data.mindScore").value(3))
                .andExpect(jsonPath("$.data.dateTime").value("2026-06-24T22:02:00"));
    }

    @Test
    void getConditionReturnsNotFoundWhenConditionDoesNotBelongToMember() throws Exception {
        Long memberId = 1L;
        Long conditionId = 12L;

        when(conditionService.getCondition(memberId, conditionId))
                .thenThrow(new CustomException(ErrorCode.CONDITION_NOT_FOUND));

        mockMvc.perform(get("/conditions/{conditionId}", conditionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 컨디션 기록입니다."))
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
