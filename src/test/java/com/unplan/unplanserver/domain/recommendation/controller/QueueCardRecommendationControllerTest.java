package com.unplan.unplanserver.domain.recommendation.controller;

import com.unplan.unplanserver.domain.recommendation.dto.response.QueueCardRecommendationResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.QueueCardRecommendationResult;
import com.unplan.unplanserver.domain.recommendation.service.RecommendationService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 큐카드 7일 추천 컨트롤러 웹 계층 검증 (라우팅·days 파라미터·200/409 분기·JSON 직렬화).
 * 서비스는 목으로 대체 — 로직은 RecommendationServiceTest 가 담당한다.
 */
class QueueCardRecommendationControllerTest {

    private static final Long MEMBER_ID = 1L;

    private RecommendationService recommendationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        recommendationService = mock(RecommendationService.class);
        mockMvc = standaloneSetup(new QueueCardRecommendationController(recommendationService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalArgumentResolver(MEMBER_ID))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 슬롯이_있으면_200과_추천_시간대_목록을_반환한다() throws Exception {
        QueueCardRecommendationResponse response = new QueueCardRecommendationResponse(
                77L, "이력서 작성", 30, 7,
                List.of(new QueueCardRecommendationResponse.Slot(
                        100L, LocalDate.of(2026, 7, 7), LocalTime.of(14, 0), LocalTime.of(14, 30), 0)));
        when(recommendationService.getQueueCardRecommendations(MEMBER_ID, 77L, 7))
                .thenReturn(QueueCardRecommendationResult.ofSuccess(response));

        mockMvc.perform(get("/schedule/{scheduleId}/recommendations", 77L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(77))
                .andExpect(jsonPath("$.title").value("이력서 작성"))
                .andExpect(jsonPath("$.estimatedTime").value(30))
                .andExpect(jsonPath("$.rangeDays").value(7))
                .andExpect(jsonPath("$.slots", hasSize(1)))
                .andExpect(jsonPath("$.slots[0].recommendId").value(100))
                .andExpect(jsonPath("$.slots[0].startTime").value("14:00:00"));
    }

    @Test
    void 슬롯이_없으면_409와_확장_가능_플래그를_반환한다() throws Exception {
        when(recommendationService.getQueueCardRecommendations(MEMBER_ID, 77L, 7))
                .thenReturn(QueueCardRecommendationResult.ofNoSlot(true, false));

        mockMvc.perform(get("/schedule/{scheduleId}/recommendations", 77L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.canExtendTo14Days").value(true))
                .andExpect(jsonPath("$.mustChangeDuration").value(false));
    }

    @Test
    void days_14로_확장_요청을_서비스에_전달한다() throws Exception {
        when(recommendationService.getQueueCardRecommendations(MEMBER_ID, 77L, 14))
                .thenReturn(QueueCardRecommendationResult.ofNoSlot(false, true));

        mockMvc.perform(get("/schedule/{scheduleId}/recommendations", 77L)
                        .param("days", "14")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.canExtendTo14Days").value(false))
                .andExpect(jsonPath("$.mustChangeDuration").value(true));
    }

    @Test
    void days가_7이나_14가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/schedule/{scheduleId}/recommendations", 77L)
                        .param("days", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    /** @AuthenticationPrincipal Long memberId 를 고정 값으로 주입 (실제 JWT 필터 대체) */
    private record TestAuthenticationPrincipalArgumentResolver(Long memberId)
            implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return memberId;
        }
    }
}
