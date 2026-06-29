package com.example.miniboard.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.miniboard.dto.PostRequest;
import com.example.miniboard.dto.PostResponse;
import com.example.miniboard.security.CustomUserDetails;
import com.example.miniboard.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @GetMapping
    public String list(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        model.addAttribute("posts", postService.getPosts(pageable));
        return "posts/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        PostResponse post = postService.getPost(id);
        Long loginUserId = (userDetails != null) ? userDetails.getUserId() : null;
        model.addAttribute("post", post);
        model.addAttribute("isOwner", post.getAuthorId().equals(loginUserId)); // 버튼 노출용(화장)
        return "posts/detail";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("postRequest", new PostRequest());
        return "posts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute PostRequest postRequest,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (bindingResult.hasErrors()) {
            return "posts/form";
        }
        Long postId = postService.createPost(postRequest, userDetails.getUserId());
        return "redirect:/posts/" + postId; // ★ PRG
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        PostResponse post = postService.getPost(id);
        if (!post.getAuthorId().equals(userDetails.getUserId())) { // 남의 글 수정폼 진입 차단
            throw new AccessDeniedException("본인 글만 수정할 수 있습니다.");
        }
        PostRequest form = new PostRequest();
        form.setTitle(post.getTitle());
        form.setContent(post.getContent());
        model.addAttribute("postRequest", form);
        model.addAttribute("postId", id); // 폼이 수정 모드임을 표시
        return "posts/form";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable("id") Long id,
            @Valid @ModelAttribute PostRequest postRequest,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("postId", id);
            return "posts/form";
        }
        postService.updatePost(id, postRequest, userDetails.getUserId());
        return "redirect:/posts/" + id; // ★ PRG
    }

    @DeleteMapping("/{id}")
    @ResponseBody // AJAX 응답이라 HTML 아님
    public ResponseEntity<Void> delete(@PathVariable("id") Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(id, userDetails.getUserId());
        return ResponseEntity.noContent().build(); // 204 No Content
    }
}