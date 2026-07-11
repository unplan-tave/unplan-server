package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.schedule.dto.request.TagRecommendationRequestDto;
import com.unplan.unplanserver.domain.schedule.dto.response.TagRecommendationResponseDto;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.webclient.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TagService {
    private final GeminiClient geminiClient;
    public TagRecommendationResponseDto recommendTag(TagRecommendationRequestDto requestDto) {
        // AI api 호출
        String title = requestDto.title();
        // AI를 이용하여 태그 추천받기
        Optional<ConditionTag> conditionTag = geminiClient.getRecommendedTag(title);
        String recommendedTag = conditionTag.map(ConditionTag::name).orElse("NONE");
        return new TagRecommendationResponseDto(recommendedTag);
    }
}
