package com.unplan.unplanserver.domain.onboarding.service;

import com.unplan.unplanserver.domain.onboarding.dto.request.RecoverRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.RecoverResponse;
import com.unplan.unplanserver.domain.onboarding.entity.RecoverEntity;
import com.unplan.unplanserver.domain.onboarding.enums.RecoveryMethodType;
import com.unplan.unplanserver.domain.onboarding.repository.RecoverRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RecoverService {

    private final RecoverRepository recoverRepository;

    public RecoverResponse.UpdateMethods updateMethods(
            Long memberId,
            RecoverRequest.UpdateMethods request
    ) {
        if (request.defaultMethods().isEmpty() && request.customMethods().isEmpty()) {
            throw new CustomException(ErrorCode.RECOVER_METHOD_REQUIRED);
        }

        recoverRepository.deleteByMemberId(memberId);

        List<RecoverEntity> recoverEntities = new ArrayList<>();

        for (RecoveryMethodType method : request.defaultMethods()) {
            recoverEntities.add(
                    RecoverEntity.builder()
                            .memberId(memberId)
                            .defaultMethod(method)
                            .customMethod(null)
                            .build()
            );
        }

        for (String customMethod : request.customMethods()) {
            recoverEntities.add(
                    RecoverEntity.builder()
                            .memberId(memberId)
                            .defaultMethod(null)
                            .customMethod(customMethod.trim())
                            .build()
            );
        }

        List<RecoverEntity> savedEntities = recoverRepository.saveAll(recoverEntities);

        return RecoverResponse.UpdateMethods.from(memberId, savedEntities);
    }
}