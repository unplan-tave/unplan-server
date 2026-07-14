package com.unplan.unplanserver.domain.recommendation.controller;

import com.unplan.unplanserver.domain.recommendation.dto.response.ConditionRecommendationResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationAcceptResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse.EmptyTime;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse.RecommendationItem;
import com.unplan.unplanserver.domain.recommendation.service.RecommendationService;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 추천 컨트롤러 웹 계층 검증 (라우팅·파라미터/본문 바인딩·JSON 직렬화·인증 주입·에러 매핑).
 * 서비스는 목으로 대체 — 로직은 RecommendationServiceTest 가 담당하고, 여기선 HTTP 경계만 본다.
 */
class RecommendationControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 7, 5);

    private RecommendationService recommendationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        recommendationService = mock(RecommendationService.class);
        mockMvc = standaloneSetup(new RecommendationController(recommendationService))
                .setCustomArgumentResolvers(new TestAuthenticationPrincipalArgumentResolver(MEMBER_ID))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 추천_목록_조회는_DTO_원본을_JSON으로_반환한다() throws Exception {
        RecommendationListResponse response = new RecommendationListResponse(
                DATE, "CORE_TASK",
                new EmptyTime(LocalTime.of(14, 0), LocalTime.of(14, 45), 45),
                List.of(new RecommendationItem(
                        10L, "과제", LocalTime.of(14, 0), LocalTime.of(14, 30), 30,
                        LocalDate.of(2026, 7, 7), "CORE_TASK", "QUEUE_CARD", "EXACT", 0, null)));
        when(recommendationService.getRecommendations(MEMBER_ID, DATE)).thenReturn(response);

        mockMvc.perform(get("/schedule/recommendations")
                        .param("date", "2026-07-05")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-07-05"))
                .andExpect(jsonPath("$.conditionTag").value("CORE_TASK"))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].title").value("과제"))
                .andExpect(jsonPath("$.recommendations[0].sourceType").value("QUEUE_CARD"))
                .andExpect(jsonPath("$.recommendations[0].matchTier").value("EXACT"))
                .andExpect(jsonPath("$.recommendations[0].displayOrder").value(0));
    }

    @Test
    void 컨디션_기반_추천_조회는_ApiResponse로_감싸서_반환한다() throws Exception {
        ConditionRecommendationResponse response = new ConditionRecommendationResponse(
                DATE,
                "SUCCESS",
                "CORE_TASK",
                "핵심 작업",
                new ConditionRecommendationResponse.EmptyTime(LocalTime.of(14, 0), LocalTime.of(15, 30), 90),
                "14:00 ~ 15:30까지, 1시간 30분 동안 스케줄이 비어 있어요\n핵심 작업에 좋은 컨디션이에요",
                List.of(new ConditionRecommendationResponse.SummaryTag("CORE_TASK", "핵심 작업")),
                List.of(new ConditionRecommendationResponse.RecommendationItem(
                        1L,
                        10L,
                        "큐 카드 일정 제목",
                        LocalTime.of(14, 0),
                        LocalTime.of(15, 0),
                        60,
                        LocalDate.of(2026, 7, 20),
                        "CORE_TASK",
                        "핵심 작업",
                        "QUEUE_CARD",
                        "EXACT",
                        0,
                        "깊게 몰입하기 좋은 컨디션이에요",
                        "일정을 끝낸 후 약간 쉴 여유가 있어요",
                        null
                ))
        );
        when(recommendationService.getConditionRecommendations(MEMBER_ID, DATE)).thenReturn(response);

        mockMvc.perform(get("/schedule/recommendations/condition")
                        .param("date", "2026-07-05")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요청 성공"))
                .andExpect(jsonPath("$.data.date").value("2026-07-05"))
                .andExpect(jsonPath("$.data.resultType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.conditionTagLabel").value("핵심 작업"))
                .andExpect(jsonPath("$.data.summaryTags[0].tag").value("CORE_TASK"))
                .andExpect(jsonPath("$.data.recommendations[0].sourceScheduleId").value(10))
                .andExpect(jsonPath("$.data.recommendations[0].conditionTagLabel").value("핵심 작업"))
                .andExpect(jsonPath("$.data.recommendations[0].suitabilityMessage").value("깊게 몰입하기 좋은 컨디션이에요"));
    }

    @Test
    void 수락은_본문_없이_호출하면_전환으로_처리된다() throws Exception {
        RecommendationAcceptResponse response = new RecommendationAcceptResponse(
                5L, 99L, "과제", DATE, LocalTime.of(14, 0), LocalTime.of(14, 30), "QUEUE_CARD", false);
        when(recommendationService.accept(MEMBER_ID, 5L, false, null)).thenReturn(response);

        mockMvc.perform(post("/schedule/recommendations/{recommendId}/accept", 5L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendId").value(5))
                .andExpect(jsonPath("$.scheduleId").value(99))
                .andExpect(jsonPath("$.title").value("과제"))
                .andExpect(jsonPath("$.created").value(false));
    }

    @Test
    void 수락은_본문의_keepQueueCard와_recoveryMean을_서비스로_전달한다() throws Exception {
        RecommendationAcceptResponse response = new RecommendationAcceptResponse(
                7L, 200L, "짧은 낮잠", DATE, LocalTime.of(14, 0), LocalTime.of(14, 30), "RECOVERY_MEAN", true);
        when(recommendationService.accept(MEMBER_ID, 7L, true, "짧은 낮잠")).thenReturn(response);

        mockMvc.perform(post("/schedule/recommendations/{recommendId}/accept", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keepQueueCard\":true,\"recoveryMean\":\"짧은 낮잠\"}")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("짧은 낮잠"))
                .andExpect(jsonPath("$.created").value(true));
    }

    @Test
    void 존재하지_않는_추천_수락은_404와_에러_본문을_반환한다() throws Exception {
        when(recommendationService.accept(MEMBER_ID, 5L, false, null))
                .thenThrow(new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        mockMvc.perform(post("/schedule/recommendations/{recommendId}/accept", 5L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("존재하지 않는 추천입니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void 이미_수락된_추천_재수락은_409를_반환한다() throws Exception {
        when(recommendationService.accept(MEMBER_ID, 5L, false, null))
                .thenThrow(new CustomException(ErrorCode.RECOMMENDATION_ALREADY_PROCESSED));

        mockMvc.perform(post("/schedule/recommendations/{recommendId}/accept", 5L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("이미 수락된 추천입니다."));
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
