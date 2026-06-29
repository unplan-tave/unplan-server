package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.schedule.entity.PersonalTag;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import com.unplan.unplanserver.domain.schedule.repository.PersonalTagRepository;
import com.unplan.unplanserver.domain.schedule.repository.SchedulePersonalTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final PersonalTagRepository personalTagRepository;
    private final SchedulePersonalTagRepository schedulePersonalTagRepository;

    /**
     * 일정 생성 시 호출. 태그 이름 목록을 받아 멤버 태그를 find-or-create 하고 일정에 연결한다.
     * - 공백/빈 토큰 제거, 같은 요청 내 중복 제거(대소문자 무시)
     * - 이미 존재하는 태그면 재사용, 없으면 생성
     */
    @Transactional
    public void attachTags(Schedule schedule, Long memberId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;

        // 같은 요청 안의 중복 태그 제거 (대소문자 무시, 입력 순서 유지)
        Set<String> seen = new LinkedHashSet<>();
        List<String> normalized = new ArrayList<>();
        for (String raw : tagNames) {
            if (raw == null) continue;
            String name = raw.trim();
            if (name.isEmpty()) continue;
            if (seen.add(name.toLowerCase())) {
                normalized.add(name);
            }
        }

        for (String name : normalized) {
            PersonalTag tag = personalTagRepository.findByMemberIdAndName(memberId, name)
                    .orElseGet(() -> personalTagRepository.save(PersonalTag.builder()
                            .memberId(memberId)
                            .name(name)
                            .build()));

            schedulePersonalTagRepository.save(SchedulePersonalTag.builder()
                    .schedule(schedule)
                    .personalTag(tag)
                    .build());
        }
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
