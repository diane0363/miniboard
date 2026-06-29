package com.example.miniboard.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ① 404 — 없는 게시글
    @ExceptionHandler(PostNotFoundException.class)
    public String handlePostNotFound(PostNotFoundException e, Model model) {
        log.warn("게시글 조회 실패: {}", e.getMessage()); // 로그엔 상세, 화면엔 친절히
        model.addAttribute("status", 404);
        model.addAttribute("message", "요청하신 게시글을 찾을 수 없습니다.");
        return "error/custom";
    }

    // ② 403 — 권한 없음 (IDOR 차단)
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException e, Model model) {
        log.warn("권한 없는 접근 시도: {}", e.getMessage());
        model.addAttribute("status", 403);
        model.addAttribute("message", "접근 권한이 없습니다.");
        return "error/custom";
    }

    // ③ 400 — 잘못된 요청 (잘못된 파라미터 등)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException e, Model model) {
        log.warn("잘못된 요청: {}", e.getMessage());
        model.addAttribute("status", 400);
        model.addAttribute("message", "잘못된 요청입니다.");
        return "error/custom";
    }

    // ④ 500 — 그 외 모든 예외 (최후의 방어선)
    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception e, Model model) {
        log.error("예상치 못한 서버 오류", e); // ★ error 레벨 + 스택 전체 기록
        model.addAttribute("status", 500);
        model.addAttribute("message", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        return "error/custom";
    }

    // ★ 정적 리소스 못 찾음 (크롬 잡요청 등) — 조용히 404, 로그 최소화
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResource(NoResourceFoundException e) {
        // 로그조차 안 남기거나, 남겨도 trace 레벨로 (스택 트레이스 X)
        return "error/custom"; // 혹은 그냥 404 빈 응답
    }
}
