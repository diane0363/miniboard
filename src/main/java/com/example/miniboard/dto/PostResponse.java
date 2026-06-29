package com.example.miniboard.dto;

import java.time.LocalDateTime;

import com.example.miniboard.domain.Post;

import lombok.Getter;

@Getter
public class PostResponse { // 엔티티를 뷰에 직접 노출X)

    private final Long id;
    private final String title;
    private final String content;
    private final String authorNickname;
    private final Long authorId; // 소유권 비교·버튼 노출 판정용
    private final LocalDateTime createdAt;

    public PostResponse(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.authorNickname = post.getUser().getNickname();
        this.authorId = post.getUser().getId();
        this.createdAt = post.getCreatedAt();
    }
}