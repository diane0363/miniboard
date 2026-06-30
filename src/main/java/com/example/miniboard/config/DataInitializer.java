package com.example.miniboard.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.miniboard.domain.Post;
import com.example.miniboard.domain.User;
import com.example.miniboard.repository.PostRepository;
import com.example.miniboard.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // ★ 이미 데이터가 있으면 또 넣지 않는다 (중복 방지)
        if (userRepository.count() > 0) {
            return;
        }

        // 1) 사용자 A, B, C, D 생성
        User a = createUser("daink", "김다인");
        User b = createUser("somink", "김소민");
        User c = createUser("yujeongj", "조유정");
        User d = createUser("hansick", "김한식");

        // 2) 기술 블로그 게시글 — (작성자, 제목, 내용)
        // =========================================================
        // 더미 게시글 — 개념 + 실제 작성 코드 (createPost 형태)
        // 작성자: a(사용자A), b(사용자B), c(사용자C), d(사용자D)
        // =========================================================

        createPost(a, "IDOR 방어 — 본인 글만 수정·삭제하기",
                "IDOR(Insecure Direct Object Reference)는 남의 글 번호를 직접 지정해 권한 없는 자원에 접근하는 취약점이다.\n"
                        + "프론트에서 버튼을 숨기는 것만으로는 못 막는다. URL이나 curl로 요청을 직접 보낼 수 있기 때문이다.\n"
                        + "그래서 서버의 Service 계층에서 '이 글의 주인이 현재 로그인 사용자인가'를 반드시 검증한다.\n\n"
                        + "[ PostService.java ]\n"
                        + "@Transactional\n"
                        + "public void updatePost(Long postId, PostRequest request, Long loginUserId) {\n"
                        + "    Post post = postRepository.findById(postId)\n"
                        + "        .orElseThrow(() -> new PostNotFoundException(postId));\n\n"
                        + "    if (!post.isOwnedBy(loginUserId)) {          // 소유권 검증\n"
                        + "        throw new AccessDeniedException(\"본인 글만 수정할 수 있습니다.\");\n"
                        + "    }\n"
                        + "    post.update(request.getTitle(), request.getContent());\n"
                        + "}");

        createPost(a, "소유권 판정은 엔티티가 책임진다",
                "소유권 검증 로직 자체는 Service에 두지만, '내 글이 맞는가'를 판정하는 책임은 Post 엔티티가 갖는다.\n"
                        + "자기 작성자 정보를 가진 객체가 스스로 판단하게 하는 것이 객체지향답다(풍부한 도메인 모델).\n\n"
                        + "[ Post.java ]\n"
                        + "public boolean isOwnedBy(Long userId) {\n"
                        + "    return this.user.getId().equals(userId);\n"
                        + "}");

        createPost(a, "BCrypt 비밀번호 해싱",
                "BCrypt는 비밀번호를 단방향으로 해싱한다. 원문으로 되돌릴 수 없어 DB가 유출돼도 비밀번호는 보호된다.\n"
                        + "자동 salt로 같은 비밀번호도 매번 다른 해시가 되고, 의도적으로 느려서 무차별 대입을 어렵게 한다.\n"
                        + "해시는 항상 60자라 컬럼을 VARCHAR(60)으로 잡는다.\n\n"
                        + "[ UserService.java ]\n"
                        + "@Transactional\n"
                        + "public Long signup(SignupRequest request) {\n"
                        + "    if (userRepository.existsByUsername(request.getUsername())) {\n"
                        + "        throw new IllegalArgumentException(\"이미 사용 중인 아이디입니다.\");\n"
                        + "    }\n"
                        + "    User user = User.builder()\n"
                        + "        .username(request.getUsername())\n"
                        + "        .password(passwordEncoder.encode(request.getPassword()))  // 해싱\n"
                        + "        .nickname(request.getNickname())\n"
                        + "        .build();\n"
                        + "    return userRepository.save(user).getId();\n"
                        + "}");

        createPost(b, "논리 삭제 — 지우지 않고 표시만 한다",
                "물리 삭제는 행을 실제로 지워 복구가 불가능하고 자식 데이터와 정합성 문제가 생긴다.\n"
                        + "논리 삭제는 deleted_at에 삭제 시각만 기록하고 행은 남긴다. Hibernate 어노테이션으로 자동화한다.\n"
                        + "@SQLDelete는 delete() 호출을 UPDATE로 바꿔치기하고, @SQLRestriction은 모든 조회에\n"
                        + "'deleted_at IS NULL'을 자동으로 붙여 필터링 실수를 막는다(옛 @Where의 대체).\n\n"
                        + "[ Post.java ]\n"
                        + "@Entity\n"
                        + "@Table(name = \"posts\")\n"
                        + "@SQLDelete(sql = \"UPDATE posts SET deleted_at = NOW() WHERE id = ?\")\n"
                        + "@SQLRestriction(\"deleted_at IS NULL\")\n"
                        + "public class Post extends BaseTimeEntity {\n"
                        + "    private LocalDateTime deletedAt;\n"
                        + "    // ...\n"
                        + "}");

        createPost(b, "N+1 문제와 fetch join",
                "목록에서 글 N개를 가져온 뒤 작성자를 한 명씩 추가 조회하면 쿼리가 1+N번 나간다. 이것이 N+1이다.\n"
                        + "연관관계는 LAZY로 두되(기본 원칙), 작성자가 꼭 필요한 목록 조회에서는 JOIN FETCH로 한 번에 가져온다.\n"
                        + "EAGER 남발은 정답이 아니다 — 필요한 곳에서만 명시적으로 함께 조회하는 것이 핵심이다.\n\n"
                        + "[ PostRepository.java ]\n"
                        + "@Query(value = \"SELECT p FROM Post p JOIN FETCH p.user\",\n"
                        + "       countQuery = \"SELECT COUNT(p) FROM Post p\")\n"
                        + "Page<Post> findAllWithUser(Pageable pageable);");

        createPost(b, "LAZY 로딩 — 연관관계는 기본 지연 로딩",
                "글을 조회할 때 작성자 정보를 항상 같이 끌어오면(EAGER) 불필요한 조인이 늘 따라붙는다.\n"
                        + "그래서 @ManyToOne은 fetch = LAZY로 두고, 작성자가 필요한 순간에만 가져오게 한다.\n\n"
                        + "[ Post.java ]\n"
                        + "@ManyToOne(fetch = FetchType.LAZY)\n"
                        + "@JoinColumn(name = \"user_id\", nullable = false)\n"
                        + "private User user;");

        createPost(c, "계층형 아키텍처 — Controller / Service / Repository",
                "요청 처리를 책임에 따라 계층으로 나눈다. Controller는 HTTP 요청·응답만, Service는 비즈니스 로직과\n"
                        + "트랜잭션, Repository는 DB 접근을 담당한다. 소유권 검증 같은 규칙은 Service에 둬야 어디서 호출하든\n"
                        + "검증이 따라온다. 컨트롤러는 정상 흐름에만 집중하게 한다.\n\n"
                        + "[ PostController.java ]\n"
                        + "@PostMapping\n"
                        + "public String create(@Valid @ModelAttribute PostRequest postRequest,\n"
                        + "                     BindingResult bindingResult,\n"
                        + "                     @AuthenticationPrincipal CustomUserDetails userDetails) {\n"
                        + "    if (bindingResult.hasErrors()) {\n"
                        + "        return \"posts/form\";\n"
                        + "    }\n"
                        + "    Long postId = postService.createPost(postRequest, userDetails.getUserId());\n"
                        + "    return \"redirect:/posts/\" + postId;          // PRG 패턴\n"
                        + "}");

        createPost(c, "PRG 패턴 — POST 후엔 리다이렉트",
                "글 작성을 처리한 뒤 화면을 바로 그리지 않고 상세 페이지로 리다이렉트한다(Post-Redirect-Get).\n"
                        + "이렇게 하면 작성 직후 새로고침(F5)을 눌러도 직전 POST가 재전송되지 않아 중복 등록을 막는다.\n"
                        + "POST 처리의 끝은 항상 리다이렉트, 라고 기억하면 된다.\n\n"
                        + "[ PostController.java ]\n"
                        + "// 작성 처리 후\n"
                        + "return \"redirect:/posts/\" + postId;\n"
                        + "// 화면을 직접 그리지 않고 GET 요청으로 다시 보낸다");

        createPost(c, "Dirty Checking — save() 없이 수정된다",
                "@Transactional 안에서 조회한 엔티티는 영속성 컨텍스트가 추적(managed)한다.\n"
                        + "트랜잭션이 끝날 때 처음 조회값과 비교해 바뀐 게 있으면 JPA가 자동으로 UPDATE를 날린다.\n"
                        + "그래서 update 메서드에 postRepository.save(post) 호출이 없다. 버그가 아니라 JPA의 변경 감지다.\n\n"
                        + "[ Post.java ]\n"
                        + "public void update(String title, String content) {\n"
                        + "    this.title = title;\n"
                        + "    this.content = content;\n"
                        + "    // save() 호출 없음 — 트랜잭션 종료 시 자동 반영\n"
                        + "}");

        createPost(d, "REST에서 HTTP Method의 의미",
                "URL은 자원(명사)을 가리키고, 행위는 HTTP Method로 표현한다. 조회 GET, 생성 POST, 수정 PUT, 삭제 DELETE.\n"
                        + "특히 GET은 데이터를 바꾸면 안 된다 — 크롤러·브라우저가 GET을 멋대로 호출하기 때문이다.\n"
                        + "GET으로 삭제를 구현하면 봇이 게시판을 돌면서 글을 전부 지울 수 있다.\n\n"
                        + "[ PostController.java ]\n"
                        + "@GetMapping(\"/{id}\")           // 조회\n"
                        + "@PostMapping                   // 생성\n"
                        + "@PutMapping(\"/{id}\")           // 수정\n"
                        + "@DeleteMapping(\"/{id}\")        // 삭제");

        createPost(d, "HTML form은 PUT을 못 보낸다 — hidden method",
                "순정 HTML form은 GET/POST만 지원한다. 그래서 수정(PUT)은 우회가 필요하다.\n"
                        + "폼은 POST로 보내되 _method=put 히든 필드를 넣으면, 서버의 HiddenHttpMethodFilter가\n"
                        + "이를 PUT으로 변환해 @PutMapping으로 라우팅한다. 이 필터는 application.yml에서 켜야 한다.\n\n"
                        + "[ posts/form.html ]\n"
                        + "<form th:action=\"@{/posts/{id}(id=${postId})}\" method=\"post\">\n"
                        + "    <input type=\"hidden\" name=\"_method\" value=\"put\"/>\n"
                        + "    <!-- ... -->\n"
                        + "</form>\n\n"
                        + "[ application.yml ]\n"
                        + "spring.mvc.hiddenmethod.filter.enabled: true");

        createPost(d, "CSRF 방어 — AJAX는 토큰을 직접 실어야 한다",
                "CSRF는 사용자의 로그인 세션을 악용해 다른 사이트가 몰래 요청을 보내는 공격이다.\n"
                        + "Spring Security는 기본으로 CSRF 토큰을 발급한다. 폼은 토큰이 자동으로 들어가지만,\n"
                        + "AJAX 요청은 헤더에 토큰을 직접 실어줘야 한다. 'POST가 403'이라고 CSRF를 끄면 보안을 끄는 것이다.\n\n"
                        + "[ posts/detail.html (삭제 AJAX) ]\n"
                        + "$.ajax({\n"
                        + "    url: \"/posts/\" + postId, type: \"DELETE\",\n"
                        + "    beforeSend: function (xhr) {\n"
                        + "        xhr.setRequestHeader(csrfHeader, csrfToken);   // 토큰 첨부\n"
                        + "    }\n"
                        + "});");

        createPost(a, "Spring Security 인증 흐름",
                "로그인 시 Security가 UserDetailsService로 사용자를 찾고, PasswordEncoder의 matches로\n"
                        + "비밀번호를 대조한 뒤 세션을 만든다. 우리는 '우리 DB에서 어떻게 찾는지'만 알려주고,\n"
                        + "인증 절차 자체는 검증된 프레임워크에 맡긴다. 바퀴를 재발명하지 않는 것이 보안의 정석이다.\n\n"
                        + "[ CustomUserDetailsService.java ]\n"
                        + "@Override\n"
                        + "public UserDetails loadUserByUsername(String username) {\n"
                        + "    User user = userRepository.findByUsername(username)\n"
                        + "        .orElseThrow(() -> new UsernameNotFoundException(\"존재하지 않는 계정입니다.\"));\n"
                        + "    return new CustomUserDetails(user);\n"
                        + "}");

        createPost(b, "전역 예외 처리 — 로그는 상세히, 화면은 친절히",
                "컨트롤러마다 try-catch를 흩뿌리는 대신 @ControllerAdvice로 예외를 한 곳에서 가로챈다.\n"
                        + "핵심은 청중을 나누는 것이다. 로그에는 개발자를 위해 상세히 남기고, 화면에는 사용자를 위해\n"
                        + "친절하고 모호한 메시지를 보여준다. 스택 트레이스를 사용자에게 노출하면 내부 구조가 새어 위험하다.\n\n"
                        + "[ GlobalExceptionHandler.java ]\n"
                        + "@ExceptionHandler(PostNotFoundException.class)\n"
                        + "public String handlePostNotFound(PostNotFoundException e, Model model) {\n"
                        + "    log.warn(\"게시글 조회 실패: {}\", e.getMessage());   // 로그: 상세\n"
                        + "    model.addAttribute(\"status\", 404);\n"
                        + "    model.addAttribute(\"message\", \"요청하신 게시글을 찾을 수 없습니다.\");  // 화면: 친절\n"
                        + "    return \"error/custom\";\n"
                        + "}");

        createPost(c, "엔티티 대신 DTO를 쓰는 이유",
                "폼 데이터를 엔티티에 직접 바인딩하면, 폼에 없는 필드까지 주입되는 취약점이 생긴다(Mass Assignment).\n"
                        + "그래서 외부에서 받을 항목만 정의한 DTO를 방패로 둔다. 검증 어노테이션도 엔티티가 아니라 DTO에 붙인다.\n\n"
                        + "[ PostRequest.java ]\n"
                        + "@Getter @Setter\n"
                        + "public class PostRequest {\n"
                        + "    @NotBlank(message = \"제목은 필수입니다.\")\n"
                        + "    @Size(max = 200, message = \"제목은 200자 이내여야 합니다.\")\n"
                        + "    private String title;\n\n"
                        + "    @NotBlank(message = \"내용은 필수입니다.\")\n"
                        + "    private String content;\n"
                        + "}");

        createPost(d, "공통 레이아웃 — Thymeleaf Layout Dialect",
                "페이지마다 반복되는 head·헤더·푸터를 base 레이아웃 하나로 묶는다. 각 페이지는 본문만 작성하고\n"
                        + "layout:decorate로 그 틀을 입는다(데코레이터 패턴). 로그인 헤더를 base에 한 번 두면 모든 페이지에\n"
                        + "자동 적용되고, 바꿀 때도 한 곳만 고치면 된다.\n\n"
                        + "[ posts/list.html ]\n"
                        + "<html xmlns:th=\"http://www.thymeleaf.org\"\n"
                        + "      xmlns:layout=\"http://www.ultraq.net.nz/thymeleaf/layout\"\n"
                        + "      layout:decorate=\"~{layout/base}\">\n"
                        + "  <main layout:fragment=\"content\">\n"
                        + "    <!-- 이 페이지 본문만 작성 -->\n"
                        + "  </main>\n"
                        + "</html>");
    }

    private User createUser(String username, String nickname) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode("11111111")) // ★ 우리 코드로 해싱
                .nickname(nickname)
                .build();
        return userRepository.save(user);
    }

    private void createPost(User user, String title, String content) {
        postRepository.save(Post.builder()
                .user(user)
                .title(title)
                .content(content)
                .build());
    }
}