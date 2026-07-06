CREATE TABLE comments (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    post_id     BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    parent_id   BIGINT       NULL,               -- NULL이면 원댓글
    content     VARCHAR(1000) NOT NULL,          -- 길이 상한 
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    deleted_at  DATETIME     NULL,               -- Soft Delete + 플레이스홀더
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_post   FOREIGN KEY (post_id)   REFERENCES posts(id),
    CONSTRAINT fk_comments_user   FOREIGN KEY (user_id)   REFERENCES users(id),
    CONSTRAINT fk_comments_parent FOREIGN KEY (parent_id) REFERENCES comments(id),
    INDEX idx_comments_post (post_id),           -- 조회 성능: WHERE post_id = ? 최적화
    INDEX idx_comments_parent (parent_id)       -- FK 걸었다고 인덱스가 자동으로 최적화되는 것 X. 조회 조건으로 쓰는 컬럼엔 명시적으로 인덱스
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;