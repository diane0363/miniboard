package com.example.miniboard.repository;

import com.example.miniboard.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    // ★ 목록 조회 시 작성자를 한 방에 가져온다 (N+1 이슈 차단)
    // JOIN FETCH p.user
        // 엔티티 관계를 지연로딩(LAZY) 설정시 
        // POST 목록 호출 후 작성자 정보를 뿌릴 때
        // 개시물 수(N)만큼 User조회 쿼리 추가 발생 => N+1
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

    // findAllWithUser
        // 이번 페이지 10개 호출 쿼리 & 전체 페이지 계산 쿼리 분리
        // 단순 count 쿼리


        @Query(value = "SELECT p FROM Post p JOIN FETCH p.user",
           countQuery = "SELECT COUNT(p) FROM Post p")
    Page<Post> findAllWithUser(Pageable pageable);
}