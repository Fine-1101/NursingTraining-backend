package org.example.nursingtrainingbackend.modules.assessment.vo.learner;

/**
 * 学员端 — 保存单题答案请求
 */
public record SaveAnswerRequest(
        String selectedOptionKey
) {
}
