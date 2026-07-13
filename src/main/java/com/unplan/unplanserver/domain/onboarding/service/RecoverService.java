package com.unplan.unplanserver.domain.onboarding.service;

import com.unplan.unplanserver.domain.onboarding.dto.request.RecoverRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.RecoverResponse;
import com.unplan.unplanserver.domain.onboarding.entity.Recover;
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

        List<Recover> recoverEntities = new ArrayList<>();

        for (RecoveryMethodType method : request.defaultMethods()) {
            recoverEntities.add(
                    Recover.builder()
                            .memberId(memberId)
                            .defaultMethod(method)
                            .customMethod(null)
                            .build()
            );
        }

        for (String customMethod : request.customMethods()) {
            recoverEntities.add(
                    Recover.builder()
                            .memberId(memberId)
                            .defaultMethod(null)
                            .customMethod(customMethod.trim())
                            .build()
            );
        }

        List<Recover> savedEntities = recoverRepository.saveAll(recoverEntities);

        return RecoverResponse.UpdateMethods.from(memberId, savedEntities);
    }

    @Transactional(readOnly = true)
    public RecoverResponse.GetMethods getMethods(Long memberId) {
        List<Recover> recoverEntities = recoverRepository.findByMemberId(memberId);

        return RecoverResponse.GetMethods.from(recoverEntities);
    }

    /**
     * 회원이 설정한 컨디션 회복 수단의 표시명 목록 (저장 순서 보존).
     * 기본 방법은 한글 설명("짧은 낮잠" 등), 직접 입력은 그 문자열을 그대로 사용한다.
     * '기력 회복' 추천의 회복 수단 후보 옵션 노출·선택 검증에 쓰인다.
     */
    @Transactional(readOnly = true)
    public List<String> getRecoveryMeanLabels(Long memberId) {
        return recoverRepository.findByMemberIdOrderByIdAsc(memberId).stream()
                .map(r -> r.getDefaultMethod() != null ? r.getDefaultMethod().getDescription() : r.getCustomMethod())
                .filter(label -> label != null && !label.isBlank())
                .toList();
    }
}