package com.unplan.unplanserver.domain.measurement.validation;

import com.unplan.unplanserver.domain.measurement.dto.request.ConditionRequest;
import com.unplan.unplanserver.domain.measurement.dto.request.SleepRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KstDateTimeValidationTest {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Autowired
    private Validator validator;

    @Test
    void conditionAcceptsKstPastAndRejectsKstFuture() {
        ConditionRequest.ConditionCreate pastRequest = conditionRequest(
                LocalDateTime.now(KST_ZONE_ID).minusMinutes(1)
        );
        ConditionRequest.ConditionCreate futureRequest = conditionRequest(
                LocalDateTime.now(KST_ZONE_ID).plusDays(1)
        );

        assertThat(validator.validate(pastRequest)).isEmpty();
        assertThat(messages(validator.validate(futureRequest)))
                .contains("미래 시각은 입력할 수 없습니다.");
    }

    @Test
    void sleepAcceptsKstPastAndRejectsKstFutureForBedAndWakeUpTime() {
        LocalDateTime now = LocalDateTime.now(KST_ZONE_ID);
        SleepRequest.SleepCreate pastRequest = new SleepRequest.SleepCreate(
                now.minusHours(8),
                now.minusMinutes(1),
                false,
                false
        );
        LocalDateTime future = LocalDateTime.now(KST_ZONE_ID).plusDays(1);
        SleepRequest.SleepCreate futureRequest = new SleepRequest.SleepCreate(
                future,
                future.plusHours(1),
                false,
                false
        );

        assertThat(validator.validate(pastRequest)).isEmpty();
        assertThat(messages(validator.validate(futureRequest)))
                .contains("취침 시각은 미래일 수 없습니다.", "기상 시각은 미래일 수 없습니다.");
    }

    private ConditionRequest.ConditionCreate conditionRequest(LocalDateTime dateTime) {
        ConditionRequest.ConditionCreate request = new ConditionRequest.ConditionCreate();
        ReflectionTestUtils.setField(request, "bodyScore", 4);
        ReflectionTestUtils.setField(request, "mindScore", 4);
        ReflectionTestUtils.setField(request, "dateTime", dateTime);
        return request;
    }

    private Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
