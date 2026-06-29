package com.example.miniboard.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "posts")
@SQLDelete(sql = "UPDATE posts SET deleted_at = NOW() WHERE id = ?") // ★ 논리삭제
@SQLRestriction("deleted_at IS NULL") // ★ 자동 필터
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    // import 패키지 jakarta
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // ★ 반드시 LAZY (N+1 방지의 시작)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private LocalDateTime deletedAt;

    @Builder
    private Post(User user, String title, String content) {
        this.user = user;
        this.title = title;
        this.content = content;
    }

    // 비즈니스 메서드 — 엔티티가 자기 상태 변경을 책임진다
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    // ★ 소유권 판정 — IDOR 검증의 핵심 로직
    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }
}