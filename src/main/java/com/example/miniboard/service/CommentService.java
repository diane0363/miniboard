package com.example.miniboard.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.miniboard.domain.Comment;
import com.example.miniboard.domain.Post;
import com.example.miniboard.domain.User;
import com.example.miniboard.dto.CommentResponse;
import com.example.miniboard.repository.CommentRepository;
import com.example.miniboard.repository.PostRepository;
import com.example.miniboard.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 댓글/대댓글 작성.
     * 
     * @return 방금 만든 댓글 (Controller가 fragment로 렌더)
     */
    @Transactional
    public CommentResponse create(Long postId, Long parentId, String content, Long loginUserId) {
        // 1) 내용 검증 (DTO 1차 방어를 통과했더라도 서비스에서 재확인 — 방어는 다층)
        String trimmed = (content == null) ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("댓글 내용을 입력하세요.");
        }
        if (trimmed.length() > 1000) {
            throw new IllegalArgumentException("댓글은 1000자를 넘을 수 없습니다.");
        }

        // 2) 대상 게시글 조회 (없거나 삭제된 글이면 예외 → 삭제된 글엔 댓글 못 닮)
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));

        User author = userRepository.getReferenceById(loginUserId); // 프록시로 충분 (FK만 필요)

        Comment saved;
        if (parentId == null) {
            // 3-a) 원댓글
            saved = commentRepository.save(Comment.ofRoot(post, author, trimmed));
        } else {
            // 3-b) 대댓글 — 여기서 1-depth 강제
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 부모 댓글입니다."));

            // ★ 1-depth 강제: 부모가 이미 대댓글이면 거부. 대대댓글 차단.
            if (parent.isReply()) {
                throw new IllegalArgumentException("대댓글에는 답글을 달 수 없습니다.");
            }
            // ★ 정합성: 부모가 다른 글 소속이면 거부 (URL 조작 방어)
            if (!parent.getPost().getId().equals(postId)) {
                throw new IllegalArgumentException("게시글과 부모 댓글이 일치하지 않습니다.");
            }
            saved = commentRepository.save(Comment.ofReply(post, author, parent, trimmed));
        }

        return CommentResponse.from(saved);
    }

    /**
     * 댓글 삭제 — 삭제 분기의 핵심.
     * 
     * @return true = 살아있는 대댓글 있음 → 플레이스홀더로 남김 (Controller가 200 + fragment)
     *         false = 대댓글 없음 → 화면에서 제거 (Controller가 204)
     */
    @Transactional
    public boolean delete(Long commentId, Long loginUserId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        // ★ IDOR 검증 — 본인 댓글만 삭제 (P1 Post 패턴 그대로, Service 계층 책임)
        if (!comment.isOwnedBy(loginUserId)) {
            throw new AccessDeniedException("본인 댓글만 삭제할 수 있습니다."); // → 403
        }

        comment.delete(); // Soft Delete: deleted_at 기록 (변경 감지로 UPDATE)

        // ★ 삭제 분기: 살아있는 대댓글이 있으면 플레이스홀더로 남겨야 함
        // (원댓글일 때만 의미 있음. 대댓글은 자식이 없으니 항상 false)
        boolean hasLiveReplies = !comment.isReply()
                && commentRepository.existsByParent_IdAndDeletedAtIsNull(commentId);

        return hasLiveReplies;
    }

    /**
     * 상세 페이지용: 한 글의 댓글 트리를 조립해서 반환.
     * - 삭제된 것도 일단 다 긁은 뒤, 여기서 "제외 / 플레이스홀더" 판정.
     */
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentTree(Long postId) {
        List<Comment> all = commentRepository.findAllByPostIdWithAuthor(postId);

        // 1) 원댓글 / 대댓글 분리
        Map<Long, CommentResponse> rootMap = new LinkedHashMap<>(); // 등록순 유지
        Map<Long, List<Comment>> repliesByParent = new LinkedHashMap<>();

        for (Comment c : all) {
            if (c.isReply()) {
                repliesByParent
                        .computeIfAbsent(c.getParentId(), k -> new ArrayList<>())
                        .add(c);
            }
        }

        List<CommentResponse> result = new ArrayList<>();
        for (Comment c : all) {
            if (c.isReply())
                continue; // 대댓글은 아래에서 부모에 붙임

            List<Comment> replies = repliesByParent.getOrDefault(c.getId(), List.of());
            boolean hasLiveReply = replies.stream().anyMatch(r -> !r.isDeleted());

            // ★ 플레이스홀더 판정
            if (c.isDeleted() && !hasLiveReply) {
                continue; // 삭제됐고 살아있는 자식도 없음 → 아예 제외
            }

            // 삭제됐지만 살아있는 자식이 있음 → 플레이스홀더로 표시(내용만 가림)
            CommentResponse root = CommentResponse.from(c);

            // 살아있는 대댓글만 자식으로 (삭제된 대댓글은 그냥 사라짐 — 자식엔 플레이스홀더 불필요)
            List<CommentResponse> childDtos = replies.stream()
                    .filter(r -> !r.isDeleted())
                    .map(CommentResponse::from)
                    .toList();
            root.setReplies(childDtos);

            result.add(root);
        }

        return result;
    }
}