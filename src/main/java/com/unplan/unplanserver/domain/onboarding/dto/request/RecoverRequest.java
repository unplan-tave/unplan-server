package com.unplan.unplanserver.domain.onboarding.dto.request;

import com.unplan.unplanserver.domain.onboarding.enums.RecoveryMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class RecoverRequest {

    public record UpdateMethods(

            @NotNull(message = "기본 회복 방법 리스트는 필수입니다.")
            List<RecoveryMethodType> defaultMethods,

            @NotNull(message = "직접 입력 리스트는 필수입니다.")
            List<@NotBlank(message = "직접 입력값은 비어 있을 수 없습니다.")
                    @Size(max = 255, message = "직접 입력값은 255자 이하로 입력해야 합니다.")
                    String> customMethods
    ) {
    }
}