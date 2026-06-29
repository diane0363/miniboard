// 폴더명
    // entity or domain 뭘 쓰든 상관없지만 통일
        // entity : DB테이블과 매핑되는 객체다 (기술적 사실)
        // domain : 우리 서비스의 핵심 개념(사용자, 게시글) (비즈니스 의미)
    // yaml, yml 모두 인식 -> 하나로 통일해서 사용하기


// JpaRepository (페이징)
    // findAllWithUser
        // 이번 페이지 10개 호출 쿼리 & 전체 페이지 계산 쿼리(findAllWithUser) 분리
        // 단순 count 쿼리

    // JOIN FETCH p.user
        // 엔티티 관계를 지연로딩(LAZY) 설정시 
        // POST 목록 호출 후 작성자 정보를 뿌릴 때
        // 개시물 수(N)만큼 User조회 쿼리 추가 발생 => N+1 (LAZY라서 그 때 그 때 추가 조회라..)
        // 이를 해결하기 위해 Join Fetch를 써서 한 번에 User데이터까지 조회

    // countQuery (성능 최적화 & 메모리 이슈 방지 -> JOIN없이 COUNT만)
        // Pageable 사용 시 전체 데이터 개수를 알아야 전체 페이지수 계산 가능
        // JPA는 JOIN FETCH가 포함된 쿼리에 페이징을 매기면
        // 쿼리가 아닌 DB 전체 데이터를 메모리르 들고와서 페이징 시도 -> OOM 가능성

    // @Query 
        // S.D. JPA는 인터페이스 메서드 이름 규칙에만 맞추면 자동 SQL쿼리 기능 제공
        // 하지만, 아래와 같은 이유로 사용
        // 1. 가독성 확보 (조회 조건이 많아지면 메서드 이름이 길어짐)
        // 2. 직접 제어권 확보 (쿼리문을 직접 명시해 의도대로 조회)
        // 3. 성능 최적화 (JOIN FETCH 사용 등) ** 

// IDOR 검증
    1. updatePost/deletePost(Service) — if (!post.isOwnedBy(loginUserId)) throw AccessDeniedException. 진짜 잠금장치
    2. Controller의 editForm — 남의 글 수정 폼에 진입 금지
    3. 템플릿의 th:if="${isOwner}" — 버튼 표시 X -> 이거만 하면 curl -X로 공격 가능

// @SQLDelete
    // postRepository.delete(post)를 호출하면 진짜 DELETE가 나가는 게 아니라, 이 어노테이션이 가로채서 UPDATE posts SET deleted_at = NOW()를 실행
    // delete()는 사실 UPDATE

// @SQLRestriction("deleted_at IS NULL")
    // 이 엔티티를 조회하는 모든 쿼리에 자동으로 WHERE deleted_at IS NULL 추가
    // 논리 삭제를 사용한 이상 모든 쿼리에 붙여야하기 때문에,,
    // 대신 지운 글을 일부러 조회하고 싶을 때 까다로워진다. @SQLRestriction이 막아버리니까. 

// JPA의 Dirty Checking (변경 감지)
    // @Transactional 안에서 조회한 엔티티는 영속성 컨텍스트가 관리 상태로 추적
    // -> 메서드 종료 시(트랜잭션이 끝날 때) JPA가 바뀐 값을 자동 체크 -> 알아서 UPDATE 쿼리 날림 
    // => save() 호출 불필요
    // 반대로 영속 상태의 엔티티 값을 바꾸면 DB에 반영된다는 걸 모르면 사고

// HTML 폼은 PUT 불가 (GET/POST만 가능)
    // HiddenHttpMethodFilter 우회
    // 폼은 method="post"로 보낸다.
    // <input type="hidden" name="_method" value="put"/>을 숨겨 넣는다.
    // 서버의 HiddenHttpMethodFilter가 이 _method 값을 읽고 "아 이건 사실 PUT이구나" 하고 변환해서 @PutMapping으로 라우팅
    // 이 필터가 Spring Boot에서 기본 비활성화
    // -> application.yml에 spring.mvc.hiddenmethod.filter.enabled: true

// HTML 폼에서 DELETE 불가
    // AJAX DELETE + CSRF (우회 불필요)
    // CSRF 끄지 마라, AJAX는 토큰이 자동으로 안 붙는다
    // 서버가 렌더링할 때 <meta> 태그에 CSRF 토큰을 심어 내려보낸다.
    // jQuery가 beforeSend에서 그 토큰을 요청 헤더에 수동으로 첨부
    // 폼은 th:action이 토큰을 자동으로 hidden 필드에 박아주지만, AJAX는 그 자동화 바깥이라 직접 헤더에 실어야 한다. 이거 안 하면 DELETE 요청이 403으로 거부

// 엔티티 노출 X -> PostResponse를 뷰로
    // getPosts, getPost -> entity가 아닌 PostResponse DTO 반환
    // LazyInitializationException 방어 — DTO 변환을 @Transactional Service 안에서 하니까, 그 안에서 post.getUser().getNickname()을 호출해 LAZY 연관을 안전하게
    // 화면에 필요한 것만 노출 — 비밀번호 해시 같은 게 실수로 뷰까지 흘러가는 걸 구조적으로 차단.



## 디버깅

// 1)번 방법으로는 계속 오류가 떠서 
// @PathVariable Long id -> @PathVariable("id") Long id로 일괄 변경
// 1) build.gradle에 아래 코드 추가
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs << '-parameters'
}




##  현재 코드 고려사항

    // OSIV(Open Session In View) — 기본 ON. Spring Boot는 OSIV가 켜져 있어서, 트랜잭션이 끝나도 영속성 컨텍스트를 뷰 렌더링까지 열어둔다. 그래서 템플릿에서 LAZY를 건드려도 안 터진다. 편하지만 DB 커넥션을 화면 렌더링 끝날 때까지 붙잡고 있어서, 트래픽 많으면 커넥션 풀이 고갈

    // OFFSET 페이징의 한계. 
        // LIMIT 10 OFFSET 100000은 앞 10만 개를 읽고 버려서 뒷페이지일수록 느려진다. 대용량 가면 커서(No-Offset) 페이징으로 전환.

    // 소프트 삭제 + UNIQUE 제약의 충돌(미래 주의). P1 Post엔 UNIQUE 컬럼이 없어 무문제지만, 나중에 "제목 중복 불가" 같은 제약을 걸면 소프트 삭제와 충돌한다(지운 글과 새 글의 제목이 같으면?). 그때 다시 고민할 카드.