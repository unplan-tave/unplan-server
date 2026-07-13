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
                        "사용자가 입력한 일정 제목을 분석하여 아래 태그 중 가장 적절한 하나를 선택하세요.\n" +
                        "\n" +
                        "[태그 설명 및 분류 기준]\n" +
                        "- URGENT: 시간적 마감이 임박했거나, 예상치 못하게 발생한 긴급 상황, 또는 반드시 즉시 참석/처리해야 하는 경조사.\n" +
                        "  예) 당장 마감인 업무, 급한 서류 제출, 장례식 참석, 응급실 방문, 갑작스러운 사고 수습\n" +
                        "- CORE_TASK: 높은 집중력과 정신적/육체적 에너지가 많이 소모되는 핵심 중요 일정.\n" +
                        "  예) 프로젝트 보고서 작성, 시험 공부, 고강도 운동, 면접 준비, 중요한 계약 미팅\n" +
                        "- BRAIN_WORK: 창의적인 생각, 기획력, 아이디어가 필요한 지적 활동 및 연구 일정.\n" +
                        "  예) 신규 서비스 기획, 독서, 마케팅 전략 수립, 브레인스토밍, 논문 읽기\n" +
                        "- DAILY_TASK: 큰 부담 없이 주기적으로 처리할 수 있는 일상적인 업무 및 가벼운 소통.\n" +
                        "  예) 이메일 확인, 팀원과의 가벼운 미팅, 정기 뉴스레터 읽기, 출퇴근\n" +
                        "- SIMPLE_TASK: 단순 반복적이거나 주로 몸을 움직여서 해결하는 기계적이고 일상적인 가사/사무 노동.\n" +
                        "  예) 방 정리정돈, 분리수거, 집안일, 사무실 물품 정리, 택배 발송, 장보기\n" +
                        "- RECOVERY: 지친 몸과 마음을 쉬게 하고 에너지를 재충전하기 위한 순수한 휴식 활동. (주의: 감정적 에너지가 크게 소모되는 장례식 조문이나 치료 목적의 병원 방문은 절대 제외)\n" +
                        "  예) 낮잠, 공원 산책, 명상, 스파, 온전한 휴식, 음악 감상\n" +
                        "- NONE: 아래의 'NONE 판단 기준'에 해당하여, 위의 6가지 컨디션 태그 중 어느 하나로도 분류하는 것이 부적절하거나 불가능한 경우.\n" +
                        "\n" +
                        "[NONE 판단 기준 - 아래 중 하나라도 해당하면 무조건 NONE]\n" +
                        "1. 정보 부족: 일정 제목이 단어 한 글자이거나 단어 자체가 없어 맥락을 전혀 파악할 수 없는 경우 (예: \"가기\", \"하기\", \"ㅇㅇ\", \"123\")\n" +
                        "2. 무의미한 텍스트: 오타, 자음/모음 나열, 의미를 알 수 없는 특수기호나 이모지 조합 (예: \"ㅋㅋㅋㅋ\", \"ㅠㅠㅠㅠ\", \"!!!@#\")\n" +
                        "3. 분류 불가: 개인의 컨디션, 에너지 소비, 시급성과 아무런 상관이 없는 단순 사실 기록이나 메모 (예: \"날씨 좋음\", \"체크리스트\", \"테스트\")\n" +
                        "\n" +
                        "[분류 우선순위 규칙]\n" +
                        "1. 'NONE 판단 기준'에 하나라도 부합하면 다른 태그 가능성이 있더라도 무조건 'NONE'을 반환합니다.\n" +
                        "2. '장례식', '상가집', '조문', '갑작스러운 병원' 등 시급성과 중대성이 높은 경조사는 RECOVERY나 DAILY_TASK가 아닌 무조건 'URGENT'로 분류합니다.\n" +
                        "3. 하나의 일정에 여러 속성이 겹칠 경우, 사용자의 에너지가 가장 많이 쓰이거나 시급한 쪽을 기준으로 판단합니다.\n" +
                        "\n" +
                        "[출력 규칙]\n" +
                        "- 반드시 위 목록에 있는 하나의 태그 이름만 반환합니다.\n" +
                        "- 태그 이름 외에는 어떠한 설명, 마크다운(예: **, `), 기호, 공백, 줄바꿈도 출력하지 마십시오. (예: URGENT 또는 NONE)"));

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
