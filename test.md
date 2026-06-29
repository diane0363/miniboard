com.example.miniboard
├── MiniboardApplication.java
├── config
│   ├── JpaConfig.java          # JPA Auditing 활성화
│   └── SecurityConfig.java     # 인증/인가 핵심
├── domain
│   ├── BaseTimeEntity.java     # created_at/updated_at 공통
│   └── User.java
├── repository
│   └── UserRepository.java
├── security
│   ├── CustomUserDetails.java
│   └── CustomUserDetailsService.java
├── service
│   └── UserService.java
├── controller
│   └── AuthController.java
├── dto
│   └── SignupRequest.java
└── resources/templates/auth
│   ├── signup.html
│   └── login.html



src/main/resources/
├── application.yml            ← 공통 (커밋 O)
├── application-dev.yml        ← 로컬 개발 (커밋 O, 단 비밀 없어야)
└── application-secret.yml     ← 비밀 (커밋 X, gitignore)    