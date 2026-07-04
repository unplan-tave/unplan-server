package com.unplan.unplanserver.domain.onboarding.dto.response;

import com.unplan.unplanserver.domain.onboarding.entity.Recover;
import com.unplan.unplanserver.domain.onboarding.enums.RecoveryMethodType;

import java.util.List;
import java.util.Objects;

public class RecoverResponse {

    public record UpdateMethods(
            Long memberId,
            List<RecoveryMethodType> defaultMethods,
            List<String> customMethods
    ) {
        public static UpdateMethods from(Long memberId, List<Recover> recoverEntities) {

            List<RecoveryMethodType> defaultMethods = recoverEntities.stream()
                    .map(Recover::getDefaultMethod)
                    .filter(Objects::nonNull)
                    .toList();

            List<String> customMethods = recoverEntities.stream()
                    .map(Recover::getCustomMethod)
                    .filter(Objects::nonNull)
                    .toList();

            return new UpdateMethods(memberId, defaultMethods, customMethods);
        }
    }

    public record GetMethods(
            List<RecoveryMethodType> defaultMethods,
            List<String> customMethods
    ) {
        public static GetMethods from(List<Recover> recoverEntities) {

            List<RecoveryMethodType> defaultMethods = recoverEntities.stream()
                    .map(Recover::getDefaultMethod)
                    .filter(Objects::nonNull)
                    .toList();

            List<String> customMethods = recoverEntities.stream()
                    .map(Recover::getCustomMethod)
                    .filter(Objects::nonNull)
                    .toList();

            return new GetMethods(defaultMethods, customMethods);
        }
    }
}