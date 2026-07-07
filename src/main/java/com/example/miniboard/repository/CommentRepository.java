package com.example.miniboard.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.miniboard.domain.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 상세 페이지용: 특정 글의 '모든' 댓글을 등록순으로 조회.
     * - 삭제된 댓글도 포함해서 가져온다 (플레이스홀더 판단은 Service가 함).
     * - author를 JOIN FETCH → 작성자 닉네임 N+1 방지 (P1 원칙 그대로).
     * - parent는 FETCH 안 함 → getParentId()로 ID만 읽어 트리 조립.
     */
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "WHERE c.post.id = :postId " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findAllByPostIdWithAuthor(@Param("postId") Long postId);

    /**
     * 삭제 분기용: 이 댓글에 '살아있는' 대댓글이 있는가?
     * - true → 플레이스홀더로 남겨야 함 (200 + fragment)
     * - false → 화면에서 제거해도 됨 (204)
     */
    boolean existsByParent_IdAndDeletedAtIsNull(Long parentId);

    /**
     * 목록 화면 댓글 수: N+1 정면 처리 (Step 4의 (A) 집계 방식).
     * - 게시글 여러 개의 댓글 수를 '한 방'에 집계.
     * - deleted_at IS NULL 필수! Soft Delete된 댓글은 카운트에서 제외.
     */
    @Query("SELECT c.post.id AS postId, COUNT(c) AS cnt " +
            "FROM Comment c " +
            "WHERE c.post.id IN :postIds AND c.deletedAt IS NULL " +
            "GROUP BY c.post.id")
    List<CommentCountProjection> countActiveByPostIds(@Param("postIds") List<Long> postIds);

    // 인터페이스 프로젝션: [postId, 개수] 쌍만 뽑는다. 엔티티 통째로 안 긁음
    interface CommentCountProjection {
        Long getPostId();

        long getCnt();
    }
}