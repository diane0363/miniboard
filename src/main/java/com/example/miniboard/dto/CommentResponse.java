// ── CommentResponse.java : 응답/렌더링용 ──
package com.example.miniboard.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.miniboard.domain.Comment;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentResponse {

    private Long id;
    private Long postId;
    private Long parentId;
    private String authorNickname;
    private Long authorId; // 화면에서 "본인 댓글이면 삭제버튼 노출" 판단용 (UX) (user_id)
    private String content;
    private boolean deleted; // true면 뷰에서 "삭제된 댓글입니다" 렌더 (플레이스홀더)
    private LocalDateTime createdAt;
    private List<CommentResponse> replies = new ArrayList<>();

    public static CommentResponse from(Comment c) {
        CommentResponse dto = new CommentResponse();
        dto.id = c.getId();
        dto.postId = c.getPost().getId(); // Comment 엔티티에서 Post의 ID를 꺼내서 매핑
        dto.parentId = c.getParentId(); // 프록시 로딩 없이 FK만 (6-1)
        dto.authorNickname = c.getAuthor().getNickname(); // author는 JOIN FETCH돼서 안전
        dto.authorId = c.getAuthor().getId();
        dto.deleted = c.isDeleted();
        // ★ 삭제된 댓글은 원문을 절대 응답에 싣지 않는다 (아래 리뷰 ③)
        dto.content = c.isDeleted() ? null : c.getContent();
        dto.createdAt = c.getCreatedAt();
        return dto;
    }
}