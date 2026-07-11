package com.unplan.unplanserver.domain.schedule.dto.response;

import com.unplan.unplanserver.domain.schedule.entity.PersonalTag;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PersonalTagResponse {

    private Long personalTagId;
    private String name;

    public static PersonalTagResponse from(PersonalTag tag) {
        return PersonalTagResponse.builder()
                .personalTagId(tag.getPersonalTagId())
                .name(tag.getName())
                .build();
    }
}
