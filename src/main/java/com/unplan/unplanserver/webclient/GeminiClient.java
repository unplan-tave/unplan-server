package com.unplan.unplanserver.webclient;


import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class GeminiClient {
    private final Client client;

    public GeminiClient(
            @Value("${gemini.api-key}") String apiKey
    ) {
        this.client = Client.builder().apiKey(apiKey).build();
    }
    public Optional<ConditionTag> getRecommendedTag(String title) {
        // 시스템 프롬프트
        Content systemInstruction = Content.fromParts(Part.fromText(
                "당신은 일정 관리 앱의 컨디션 태그 추천 시스템입니다.\n" +
                "사용자가 입력한 일정 제목을 분석하여 아래 태그 중 하나를 선택하세요.\n" +
                "\n" +
                "태그 설명\n" +
                "- URGENT: 마감이나 우선순위가 모두 급한 일정. 예) 당장 마감인 업무, 급한 제출\n" +
                "- CORE_TASK: 집중력과 에너지가 모두 필요한 중요 일정. 예) 보고서 작성, 시험 공부, 운동\n" +
                "- BRAIN_WORK: 생각하고 아이디어를 내는 일정. 예) 기획, 독서, 전략 수립\n" +
                "- DAILY_TASK: 부담 없이 할 수 있는 일상 업무. 예) 메일 확인, 가벼운 미팅, 일상 루틴\n" +
                "- SIMPLE_TASK: 반복적이거나 몸을 움직이는 일정. 예) 정리정돈, 집안일, 물건 옮기기\n" +
                "- RECOVERY: 휴식과 재충전을 위한 일정. 예) 낮잠, 산책, 명상\n" +
                "- NONE: 위 태그 중 적절한 것을 판단할 수 없는 경우\n" +
                "\n" +
                "규칙\n" +
                "- 반드시 하나의 태그만 반환합니다.\n" +
                "- 태그 이름 외에는 아무것도 출력하지 않습니다.\n" +
                "- 마크다운, 기호, 줄바꿈 없이 태그 이름만 출력합니다."));

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(systemInstruction)
                .build();
        try {
            GenerateContentResponse response =
                    client.models.generateContent("gemini-3.1-flash-lite",
                            "일정제목:" + title,
                            config
                    );
            String result = response.text().trim();
            if (result.equals("NONE")) {
                return Optional.empty();
            }
            try {
                return Optional.of(ConditionTag.valueOf(result));
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_API_UNAVAILABLE);
        }
    }
}
