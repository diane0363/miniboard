package com.example.miniboard.exception;

public class PostNotFoundException extends RuntimeException {
    // 예외 던지기! 응답 처리는 전역 핸들러에서
    // AccessDeniedException의 경우 SpringSecurity가 알아서 403 처리
    public PostNotFoundException(Long id) {
        super("게시글을 찾을 수 없습니다. id=" + id);
    }
}