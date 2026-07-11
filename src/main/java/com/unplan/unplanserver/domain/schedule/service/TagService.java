package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.schedule.dto.request.TagRecommendationRequestDto;
import com.unplan.unplanserver.domain.schedule.dto.response.TagRecommendationResponseDto;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.webclient.GeminiClient;
import com.unplan.unplanserver.domain.schedule.entity.PersonalTag;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import com.unplan.unplanserver.domain.schedule.repository.PersonalTagRepository;
import com.unplan.unplanserver.domain.schedule.repository.SchedulePersonalTagRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 개인 태그 생성·연결을 담당. 별도 컨트롤러(REST API) 없이 ScheduleService가 주입해서 사용한다.
 * 태그는 멤버 단위로 재사용되며(personal_tag), 일정과는 조인 테이블(schedule_personal_tag)로 연결된다.
 */
@Service
@RequiredArgsConstructor
public class TagService {
    private final GeminiClient geminiClient;
    // Figma 스펙: 한 계정당 개인 태그 100개까지 생성 가능 (기존 태그 재사용은 한도와 무관)
    private static final int MAX_TAGS_PER_MEMBER = 100;

    private final PersonalTagRepository personalTagRepository;
    private final SchedulePersonalTagRepository schedulePersonalTagRepository;

    // DB작업이 없어서 트랜잭션 걸지 않음
    public TagRecommendationResponseDto recommendTag(TagRecommendationRequestDto requestDto) {
        // AI api 호출
        String title = requestDto.title();
        // AI를 이용하여 태그 추천받기
        Optional<ConditionTag> conditionTag = geminiClient.getRecommendedTag(title);
        String recommendedTag = conditionTag.map(ConditionTag::name).orElse("NONE");
        return new TagRecommendationResponseDto(recommendedTag);
    }

    /**
     * 일정 생성·수정 시 호출. 태그 이름 목록을 받아 멤버 태그를 find-or-create 하고 일정에 연결한다.
     * - 공백/빈 토큰 제거, 같은 요청 내 중복 제거(대소문자 무시)
     * - 대소문자만 다른 기존 태그가 있으면 그 태그를 재사용(IgnoreCase), 없으면 생성
     * - 새 태그 생성은 계정당 100개까지 (초과 시 PERSONAL_TAG_LIMIT_EXCEEDED)
     *
     * @return 실제로 연결된 태그 이름 목록 (기존 태그 재사용 시 저장돼 있던 표기를 따른다)
     */
    @Transactional
    public List<String> attachTags(Schedule schedule, Long memberId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return List.of();

        Set<String> seen = new LinkedHashSet<>(); // 요청 내 대소문자 무시 중복 제거용
        List<String> linked = new ArrayList<>();
        for (String raw : tagNames) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty()) continue;
            if (!seen.add(name.toLowerCase())) continue;

            PersonalTag tag = personalTagRepository.findByMemberIdAndNameIgnoreCase(memberId, name)
                    .orElseGet(() -> createTag(memberId, name));

            schedulePersonalTagRepository.save(SchedulePersonalTag.builder()
                    .schedule(schedule)
                    .personalTag(tag)
                    .build());
            linked.add(tag.getName());
        }
        return linked;
    }

    private PersonalTag createTag(Long memberId, String name) {
        if (personalTagRepository.countByMemberId(memberId) >= MAX_TAGS_PER_MEMBER) {
            throw new CustomException(ErrorCode.PERSONAL_TAG_LIMIT_EXCEEDED);
        }
        return personalTagRepository.save(PersonalTag.builder()
                .memberId(memberId)
                .name(name)
                .build());
    }

    /** 일정 삭제 시 해당 일정의 태그 연결을 모두 제거 (조인 행이 FK로 남는 것 방지) */
    @Transactional
    public void detachAll(Schedule schedule) {
        schedulePersonalTagRepository.deleteBySchedule(schedule);
    }

    /** 개인 태그 검색 화면용 — 멤버가 가진 전체 태그 목록 */
    @Transactional(readOnly = true)
    public List<PersonalTag> getPersonalTags(Long memberId) {
        return personalTagRepository.findByMemberIdOrderByName(memberId);
    }

    /** 일정 상세 조회 시 해당 일정에 연결된 태그 이름 목록 */
    @Transactional(readOnly = true)
    public List<String> getTagNamesBySchedule(Schedule schedule) {
        return schedulePersonalTagRepository.findBySchedule(schedule).stream()
                .map(spt -> spt.getPersonalTag().getName())
                .toList();
    }
}
