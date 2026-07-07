// ── CommentCreateRequest.java : 작성 요청 (외부 입력의 방패) ──
package com.example.miniboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommentCreateRequest {

    @NotBlank(message = "댓글 내용을 입력하세요.") // 빈값/공백 1차 방어
    @Size(max = 1000, message = "댓글은 1000자를 넘을 수 없습니다.") // 길이 1차 방어
    private String content;

    private Long parentId; // null이면 원댓글, 값 있으면 대댓글. (검증은 Service)
}