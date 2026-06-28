@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // 공개: 정적 리소스 · 가입/로그인 페이지
                .requestMatchers("/", "/signup", "/login",
                                 "/css/**", "/js/**", "/images/**").permitAll()
                // ★ 쓰기 폼은 인증 필요 — 반드시 아래 GET permitAll보다 먼저!
                .requestMatchers("/posts/new", "/posts/*/edit").authenticated()
                // 읽기 공개: 목록 · 상세
                .requestMatchers(HttpMethod.GET, "/posts", "/posts/*").permitAll()
                // 변경 행위는 인증 필요 (다음 슬라이스에서 컨트롤러 붙음)
                .requestMatchers(HttpMethod.POST, "/posts").authenticated()
                .requestMatchers(HttpMethod.PUT, "/posts/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/posts/*").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")   // 이 POST는 Security가 가로챈다 (3단계 표)
                .defaultSuccessUrl("/posts")
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/posts?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );
        return http.build();
    }
}