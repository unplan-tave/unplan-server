package com.unplan.unplanserver.domain.measurement.controller;

import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.ConditionRecord;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.SleepRecord;
import com.unplan.unplanserver.domain.measurement.service.MeasurementService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MeasurementControllerTest {

    private MeasurementService measurementService;
    private MockMvc mockMvc;
    private final Long authenticatedMemberId = 1L;

    @BeforeEach
    void setUp() {
        measurementService = mock(MeasurementService.class);
        mockMvc = standaloneSetup(new MeasurementController(measurementService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalArgumentResolver(authenticatedMemberId))
                .build();
    }

    @Test
    void getDailyRecordReturnsMeasurementRecord() throws Exception {
        LocalDate date = LocalDate.of(2026, 6, 24);
        MeasurementRecordResponse response = new MeasurementRecordResponse(
                date,
                50,
                "보통 강도 작업 가능",
                "일상 작업",
                50,
                33,
                84,
                420,
                List.of(new ConditionRecord(
                        12L,
                        3,
                        2,
                        50,
                        33,
                        LocalDateTime.of(2026, 6, 24, 10, 0)
                )),
                List.of(new SleepRecord(
                        45L,
                        420,
                        LocalDateTime.of(2026, 6, 23, 23, 30),
                        LocalDateTime.of(2026, 6, 24, 6, 30),
                        false,
                        LocalDateTime.of(2026, 6, 24, 7, 0)
                ))
        );

        when(measurementService.getDailyRecord(authenticatedMemberId, date)).thenReturn(response);

        mockMvc.perform(get("/measurements")
                        .param("date", "2026-06-24")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청 성공"))
                .andExpect(jsonPath("$.data.date").value("2026-06-24"))
                .andExpect(jsonPath("$.data.finalConditionScore").value(50))
                .andExpect(jsonPath("$.data.conditionLevel").value("보통 강도 작업 가능"))
                .andExpect(jsonPath("$.data.conditionTag").value("일상 작업"))
                .andExpect(jsonPath("$.data.bodyScorePercent").value(50))
                .andExpect(jsonPath("$.data.mindScorePercent").value(33))
                .andExpect(jsonPath("$.data.sleepScore").value(84))
                .andExpect(jsonPath("$.data.sleepDurationMinutes").value(420))
                .andExpect(jsonPath("$.data.conditions[0].conditionId").value(12))
                .andExpect(jsonPath("$.data.conditions[0].bodyScorePercent").value(50))
                .andExpect(jsonPath("$.data.conditions[0].mindScorePercent").value(33))
                .andExpect(jsonPath("$.data.sleeps[0].sleepId").value(45))
                .andExpect(jsonPath("$.data.sleeps[0].isNap").value(false));
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
