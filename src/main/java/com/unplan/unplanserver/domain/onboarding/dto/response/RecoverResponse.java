package com.unplan.unplanserver.domain.onboarding.dto.response;

import com.unplan.unplanserver.domain.onboarding.entity.RecoverEntity;
import com.unplan.unplanserver.domain.onboarding.enums.RecoveryMethodType;

import java.util.List;
import java.util.Objects;

public class RecoverResponse {

    public record UpdateMethods(
            Long memberId,
            List<RecoveryMethodType> defaultMethods,
            List<String> customMethods
    ) {
        public static UpdateMethods from(Long memberId, List<RecoverEntity> recoverEntities) {

            List<RecoveryMethodType> defaultMethods = recoverEntities.stream()
                    .map(RecoverEntity::getDefaultMethod)
                    .filter(Objects::nonNull)
                    .toList();

            List<String> customMethods = recoverEntities.stream()
                    .map(RecoverEntity::getCustomMethod)
                    .filter(Objects::nonNull)
                    .toList();

            return new UpdateMethods(memberId, defaultMethods, customMethods);
        }
    }
}