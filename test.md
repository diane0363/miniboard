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
    ├── signup.html
    └── login.html