package com.example.miniboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.miniboard.dto.CommentCreateRequest;
import com.example.miniboard.dto.CommentResponse;
import com.example.miniboard.security.CustomUserDetails;
import com.example.miniboard.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글/대댓글 작성.
     * 응답: 방금 만든 댓글 1개의 HTML 조각 (fragment). → 200 OK
     */
    @PostMapping("/posts/{postId}/comments")
    public String create(@PathVariable Long postId,
            @Valid @ModelAttribute CommentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {

        CommentResponse created = commentService.create(
                postId, request.getParentId(), request.getContent(), principal.getId());

        model.addAttribute("comment", created);
        // fragments/comment.html 안의 th:fragment="commentItem" 조각만 렌더해서 반환
        return "fragments/comment :: commentItem";
    }

    /**
     * 댓글 삭제 — 삭제 분기의 HTTP 번역.
     * Service가 true(살아있는 대댓글 있음) → 플레이스홀더 fragment + 200
     * Service가 false(대댓글 없음) → 본문 없음 + 204
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<String> delete(@PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails principal,
            Model model) {

        boolean keepAsPlaceholder = commentService.delete(commentId, principal.getId());

        if (keepAsPlaceholder) {
            // 200 + 플레이스홀더 조각 → JS가 기존 댓글을 이걸로 '교체'
            CommentResponse placeholder = commentService.getComment(commentId); // deleted=true 상태
            model.addAttribute("comment", placeholder);
            String html = renderFragment("fragments/comment :: commentItem", model);
            return ResponseEntity.ok(html);
        }
        // 204 No Content → JS가 해당 DOM을 '제거'
        return ResponseEntity.noContent().build();
    }
}