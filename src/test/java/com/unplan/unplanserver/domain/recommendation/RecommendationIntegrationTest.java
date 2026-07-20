package com.unplan.unplanserver.domain.recommendation;

import com.unplan.unplanserver.domain.auth.dto.GoogleUserInfoDto;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.domain.recommendation.dto.response.QueueCardRecommendationResult;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationAcceptResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse;
import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationSourceType;
import com.unplan.unplanserver.domain.recommendation.repository.RecommendationRepository;
import com.unplan.unplanserver.domain.recommendation.service.RecommendationService;
import com.unplan.unplanserver.domain.schedule.entity.PersonalTag;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.PersonalTagRepository;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.repository.SchedulePersonalTagRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 추천 도메인 풀스택 통합 테스트 — 실제 Spring 컨텍스트 + JPA + H2(인메모리)로,
 * 서비스↔영속 계층 배선을 검증한다. 단위 테스트(목)가 못 보는 실제 DB 저장·전환·조인 테이블 복제를 확인한다.
 *
 * accept 경로는 Member 행이 필요 없다(getDailyRecord 미호출, Schedule.memberId 는 순수 Long).
 * generate(홈/컨디션) 경로만 컨디션 점수 산정을 위해 Member 를 시드한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationIntegrationTest {

    @Autowired private RecommendationService recommendationService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecommendationRepository recommendationRepository;
    @Autowired private PersonalTagRepository personalTagRepository;
    @Autowired private SchedulePersonalTagRepository schedulePersonalTagRepository;
    @Autowired private MemberRepository memberRepository;
    @PersistenceContext private EntityManager em;

    private static final Long MEMBER_ID = 4242L;
    private static final LocalDate TODAY = LocalDate.now();

    @Test
    @DisplayName("수락(전환): 큐 카드가 실제 DB에서 핀으로 UPDATE 되고 새 일정 INSERT 없이 추천이 ACCEPTED 된다")
    void acceptTransitionUpdatesQueueCardInPlace() {
        Schedule queue = scheduleRepository.save(queueCard(MEMBER_ID, "과제", ConditionTag.CORE_TASK, 30));
        Recommendation rec = recommendationRepository.save(pendingRec(MEMBER_ID, queue.getScheduleId()));
        long scheduleCountBefore = scheduleRepository.count();

        RecommendationAcceptResponse res =
                recommendationService.accept(MEMBER_ID, rec.getRecommendId(), false, null);

        em.flush();
        em.clear();

        // 새 일정 생성 없음 (원본 UPDATE)
        assertThat(res.created()).isFalse();
        assertThat(scheduleRepository.count()).isEqualTo(scheduleCountBefore);

        Schedule reloaded = scheduleRepository.findById(queue.getScheduleId()).orElseThrow();
        assertThat(reloaded.getIsQueue()).isFalse();
        assertThat(reloaded.getStartTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(reloaded.getEndTime()).isEqualTo(LocalTime.of(14, 30));

        Recommendation reReadRec = recommendationRepository.findById(rec.getRecommendId()).orElseThrow();
        assertThat(reReadRec.getAcceptedScheduleId()).isEqualTo(queue.getScheduleId());
    }

    @Test
    @DisplayName("수락(유지+복제): 핀 카드가 새로 INSERT 되고 개인 태그가 조인 테이블에 복제되며 원본 큐 카드·태그는 재사용된다")
    void acceptKeepQueueCardCopiesPersonalTags() {
        Schedule queue = scheduleRepository.save(queueCard(MEMBER_ID, "보고서", ConditionTag.CORE_TASK, 30));
        PersonalTag work = personalTagRepository.save(PersonalTag.builder().memberId(MEMBER_ID).name("업무").build());
        PersonalTag important = personalTagRepository.save(PersonalTag.builder().memberId(MEMBER_ID).name("중요").build());
        schedulePersonalTagRepository.save(SchedulePersonalTag.builder().schedule(queue).personalTag(work).build());
        schedulePersonalTagRepository.save(SchedulePersonalTag.builder().schedule(queue).personalTag(important).build());
        Recommendation rec = recommendationRepository.save(pendingRec(MEMBER_ID, queue.getScheduleId()));

        RecommendationAcceptResponse res =
                recommendationService.accept(MEMBER_ID, rec.getRecommendId(), true, null);

        em.flush();
        em.clear();

        // 원본과 다른 새 핀 카드가 INSERT 됨
        assertThat(res.created()).isTrue();
        assertThat(res.scheduleId()).isNotEqualTo(queue.getScheduleId());

        // 원본 큐 카드는 큐로 그대로 남음 (재추천 후보 유지)
        Schedule original = scheduleRepository.findById(queue.getScheduleId()).orElseThrow();
        assertThat(original.getIsQueue()).isTrue();

        // 새 핀 카드에 개인 태그 2개가 조인 테이블로 복제됨
        Schedule pin = scheduleRepository.findById(res.scheduleId()).orElseThrow();
        assertThat(pin.getIsQueue()).isFalse();
        List<SchedulePersonalTag> pinTags = schedulePersonalTagRepository.findBySchedule(pin);
        assertThat(pinTags).extracting(spt -> spt.getPersonalTag().getName())
                .containsExactlyInAnyOrder("업무", "중요");

        // 멤버 단위 태그라 find-or-create 로 재사용 — 중복 PersonalTag 생성 없음
        assertThat(personalTagRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("홈/컨디션 추천 생성: 실제 DB 에서 후보 큐 카드로 추천 행이 저장되고 응답에 노출된다")
    void generateHomeRecommendationsPersistsRows() {
        Member member = memberRepository.save(
                Member.fromGoogle(new GoogleUserInfoDto("oauth-int-1", "tester", "tester@example.com")));
        Long memberId = member.getMemberId();

        LocalDate targetDate = TODAY.plusDays(1); // 미래 날짜 → 하루 종일 빈 시간(시각 의존 제거)
        // RECOVERY 태그 카드는 계산된 현재 컨디션 태그와 무관하게 매칭돼 결정적이다.
        scheduleRepository.save(queueCard(memberId, "휴식 준비", ConditionTag.RECOVERY, 30));

        RecommendationListResponse res = recommendationService.generate(
                memberId, targetDate, LocalDateTime.of(TODAY, LocalTime.of(9, 0)));

        assertThat(res.recommendations()).isNotEmpty();
        assertThat(res.emptyTime()).isNotNull();
        assertThat(res.recommendations().get(0).sourceType())
                .isEqualTo(RecommendationSourceType.QUEUE_CARD.name());

        em.flush();
        em.clear();

        List<Recommendation> persisted = recommendationRepository.findAll().stream()
                .filter(r -> r.getMemberId().equals(memberId) && r.getDate().equals(targetDate))
                .toList();
        assertThat(persisted).hasSameSizeAs(res.recommendations());
        assertThat(persisted).allSatisfy(r -> {
            assertThat(r.getSourceType()).isEqualTo(RecommendationSourceType.QUEUE_CARD);
            assertThat(r.getDisplayOrder()).isNotNull();
        });
    }

    @Test
    @DisplayName("큐카드 7일 추천: 오늘부터 7일 범위에서 후보 시간대가 DB에 저장되고 성공 응답을 준다")
    void generateQueueCard7DayRecommendationsPersistsSlots() {
        Schedule queue = scheduleRepository.save(queueCard(MEMBER_ID, "면접 준비", ConditionTag.CORE_TASK, 30));

        QueueCardRecommendationResult result =
                recommendationService.getQueueCardRecommendations(MEMBER_ID, queue.getScheduleId(), 7);

        assertThat(result.hasSlots()).isTrue();

        em.flush();
        em.clear();

        List<Recommendation> persisted = recommendationRepository.findAll().stream()
                .filter(r -> r.getMemberId().equals(MEMBER_ID)
                        && queue.getScheduleId().equals(r.getSourceScheduleId()))
                .toList();
        assertThat(persisted).isNotEmpty();
        assertThat(persisted).allSatisfy(r ->
                assertThat(r.getSourceType()).isEqualTo(RecommendationSourceType.QUEUE_CARD));
    }

    @Test
    @DisplayName("패스: 패스한 큐 카드는 그날 추천에서 빠지고, 다음 날은 다시 후보로 뜬다")
    void passExcludesQueueCardForThatDayOnly() {
        Member member = memberRepository.save(
                Member.fromGoogle(new GoogleUserInfoDto("oauth-int-pass", "tester", "pass@example.com")));
        Long memberId = member.getMemberId();

        LocalDate targetDate = TODAY.plusDays(1); // 미래 날짜 → 하루 종일 빈 시간(시각 의존 제거)
        Schedule queue = scheduleRepository.save(queueCard(memberId, "휴식 준비", ConditionTag.RECOVERY, 30));
        LocalDateTime now = LocalDateTime.of(TODAY, LocalTime.of(9, 0));

        // 1. 최초 생성 → 그 큐 카드가 추천됨
        RecommendationListResponse first = recommendationService.generate(memberId, targetDate, now);
        assertThat(first.recommendations()).isNotEmpty();
        Long recommendId = first.recommendations().get(0).recommendId();

        // 2. 패스
        recommendationService.pass(memberId, recommendId);
        em.flush();
        em.clear();

        // 3. 같은 날 재생성 → 패스된 큐 카드가 유일 후보였으므로 추천 없음
        RecommendationListResponse afterPass = recommendationService.generate(memberId, targetDate, now);
        assertThat(afterPass.recommendations()).isEmpty();

        // 4. 다음 날 → 패스는 날짜 기준이라 같은 큐 카드가 다시 뜬다
        RecommendationListResponse nextDay = recommendationService.generate(memberId, targetDate.plusDays(1), now);
        assertThat(nextDay.recommendations())
                .extracting(r -> r.title()).contains("휴식 준비");
    }

    @Test
    @DisplayName("패스: 회복 수단 추천(원본 큐 카드 없음)은 패스할 수 없어 예외가 발생한다")
    void passRecoveryMeanRecommendationThrows() {
        Recommendation recoveryRec = recommendationRepository.save(Recommendation.builder()
                .memberId(MEMBER_ID).date(TODAY).title("스트레칭")
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(14, 20))
                .conditionTag(ConditionTag.RECOVERY)
                .sourceType(RecommendationSourceType.RECOVERY_MEAN)
                .sourceScheduleId(null)
                .displayOrder(0)
                .build());

        assertThatThrownBy(() -> recommendationService.pass(MEMBER_ID, recoveryRec.getRecommendId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RECOMMENDATION_NOT_PASSABLE);
    }

    // ─────────────────────────── 헬퍼 ───────────────────────────

    private Schedule queueCard(Long memberId, String title, ConditionTag tag, Integer estimatedTime) {
        return Schedule.builder()
                .memberId(memberId).title(title)
                .conditionTag(tag).estimatedTime(estimatedTime)
                .date(TODAY.plusDays(3)) // 마감일(미래)
                .isQueue(true).isRecurring(false).isConflict(false)
                .status(ScheduleStatus.TODO)
                .createdAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .build();
    }

    private Recommendation pendingRec(Long memberId, Long sourceScheduleId) {
        return Recommendation.builder()
                .memberId(memberId).date(TODAY).title("과제")
                .startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(14, 30))
                .conditionTag(ConditionTag.CORE_TASK)
                .sourceType(RecommendationSourceType.QUEUE_CARD)
                .sourceScheduleId(sourceScheduleId)
                .displayOrder(0)
                .build();
    }
}
