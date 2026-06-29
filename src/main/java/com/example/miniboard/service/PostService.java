package com.example.miniboard.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.miniboard.domain.Post;
import com.example.miniboard.domain.User;
import com.example.miniboard.dto.PostRequest;
import com.example.miniboard.dto.PostResponse;
import com.example.miniboard.exception.PostNotFoundException;
import com.example.miniboard.repository.PostRepository;
import com.example.miniboard.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 읽기 전용
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional // 쓰기만 readOnly 해제
    public Long createPost(PostRequest request, Long loginUserId) {
        User user = userRepository.findById(loginUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        Post post = Post.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        return postRepository.save(post).getId();
    }

    public Page<PostResponse> getPosts(Pageable pageable) {
        return postRepository.findAllWithUser(pageable)
                .map(PostResponse::new); // 트랜잭션 안에서 DTO로 변환
    }

    public PostResponse getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        return new PostResponse(post);
    }

    @Transactional
    public void updatePost(Long postId, PostRequest request, Long loginUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.isOwnedBy(loginUserId)) { // ★★★ IDOR 검증 ★★★
            throw new AccessDeniedException("본인 글만 수정할 수 있습니다.");
        }
        post.update(request.getTitle(), request.getContent()); // ★ save() 없음 (dirty checking)
    }

    @Transactional
    public void deletePost(Long postId, Long loginUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (!post.isOwnedBy(loginUserId)) { // ★★★ IDOR 검증 ★★★
            throw new AccessDeniedException("본인 글만 삭제할 수 있습니다.");
        }
        postRepository.delete(post); // ★ @SQLDelete가 가로채 UPDATE로
    }
}