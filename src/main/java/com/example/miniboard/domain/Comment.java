package com.example.miniboard.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comments_post", columnList = "post_id"),
        @Index(name = "idx_comments_parent", columnList = "parent_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA용 기본 생성자. 외부 무분별한 생성 차단
public class Comment extends BaseTimeEntity { // created_at / updated_at 은 P1의 BaseTimeEntity 상속

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
    private Long id;

    // ── 소속 게시글 (N:1) ──
    @ManyToOne(fetch = FetchType.LAZY, optional = false) // optional = false 연결관계가 항상 존재
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    // ── 작성자 (N:1) : IDOR 검증·닉네임 표시용 ──
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    // ── 부모 댓글 (자기참조 N:1) : NULL이면 원댓글 ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") // nullable = true (기본값). 원댓글은 부모 없음
    private Comment parent;

    @Column(nullable = false, length = 1000) // DB 레벨 1차 방어선
    private String content;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // Soft Delete. NULL=생존

    // ── 정적 팩토리 : 생성 의도를 이름으로 드러낸다 ──
    // 팩토리 메서드 : 인스턴스를 만들어서 돌려주는 것 -> 인스턴스없이 클래스에 직접 부름
    public static Comment ofRoot(Post post, User author, String content) {
        Comment c = new Comment();
        c.post = post;
        c.author = author;
        c.parent = null;
        c.content = content;
        return c;
    }

    public static Comment ofReply(Post post, User author, Comment parent, String content) {
        Comment c = new Comment();
        c.post = post;
        c.author = author;
        c.parent = parent; // 1-depth 검증은 Service 책임. 여기선 구조만.
        c.content = content;
        return c;
    }

    // ── 도메인 행위 ──
    public boolean isOwnedBy(Long userId) { // IDOR 검증 (P1 Post 패턴 그대로)
        return this.author.getId().equals(userId);
    }

    public boolean isReply() { // 1-depth 강제용
        return this.parent != null;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void delete() { // Soft Delete
        this.deletedAt = LocalDateTime.now();
    }

    public Long getParentId() { // 프록시 load 없이 FK만 읽기 (아래 리뷰 참조)
        return (this.parent != null) ? this.parent.getId() : null;
    }
}
