package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.schedule.entity.PersonalTag;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import com.unplan.unplanserver.domain.schedule.repository.PersonalTagRepository;
import com.unplan.unplanserver.domain.schedule.repository.SchedulePersonalTagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TagService 의 개인 태그 생성·연결 로직 검증 (DB 없이 Mockito 로 레포지토리 모킹).
 */
@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    private PersonalTagRepository personalTagRepository;
    @Mock
    private SchedulePersonalTagRepository schedulePersonalTagRepository;
    @InjectMocks
    private TagService tagService;

    private final Schedule schedule = Schedule.builder().scheduleId(1L).memberId(1L).build();

    @Test
    @DisplayName("attachTags — 공백/중복(대소문자 무시)/빈값/null 토큰을 정리하고 새 태그를 생성·연결")
    void attachTagsNormalizesAndCreates() {
        when(personalTagRepository.findByMemberIdAndNameIgnoreCase(eq(1L), anyString())).thenReturn(Optional.empty());
        when(personalTagRepository.save(any(PersonalTag.class))).thenAnswer(inv -> inv.getArgument(0));

        List<String> linked = tagService.attachTags(schedule, 1L,
                Arrays.asList("건강", " 건강 ", "자기계발", "", null));

        assertEquals(List.of("건강", "자기계발"), linked); // 중복(건강) 1회, 공백/null 제거
        verify(personalTagRepository, times(2)).save(any(PersonalTag.class));
        verify(schedulePersonalTagRepository, times(2)).save(any(SchedulePersonalTag.class));
    }

    @Test
    @DisplayName("attachTags — 이미 존재하는 태그는 재사용(새로 저장하지 않음)하고 저장된 표기를 반환")
    void attachTagsReusesExisting() {
        PersonalTag existing = PersonalTag.builder().personalTagId(5L).memberId(1L).name("건강").build();
        when(personalTagRepository.findByMemberIdAndNameIgnoreCase(1L, "건강")).thenReturn(Optional.of(existing));

        List<String> linked = tagService.attachTags(schedule, 1L, List.of("건강"));

        assertEquals(List.of("건강"), linked);
        verify(personalTagRepository, never()).save(any(PersonalTag.class)); // 재사용 → 생성 안 함
        verify(schedulePersonalTagRepository, times(1)).save(any(SchedulePersonalTag.class));
    }

    @Test
    @DisplayName("attachTags — null/빈 목록이면 아무 것도 하지 않고 빈 목록 반환")
    void attachTagsEmptyInput() {
        assertTrue(tagService.attachTags(schedule, 1L, null).isEmpty());
        assertTrue(tagService.attachTags(schedule, 1L, List.of()).isEmpty());
        verifyNoInteractions(personalTagRepository, schedulePersonalTagRepository);
    }

    @Test
    @DisplayName("getPersonalTags — 멤버의 태그 목록을 그대로 반환")
    void getPersonalTags() {
        List<PersonalTag> tags = List.of(PersonalTag.builder().personalTagId(1L).memberId(1L).name("건강").build());
        when(personalTagRepository.findByMemberIdOrderByName(1L)).thenReturn(tags);

        assertEquals(tags, tagService.getPersonalTags(1L));
    }

    @Test
    @DisplayName("detachAll — 일정의 태그 연결을 모두 제거")
    void detachAll() {
        tagService.detachAll(schedule);
        verify(schedulePersonalTagRepository).deleteBySchedule(schedule);
    }
}
