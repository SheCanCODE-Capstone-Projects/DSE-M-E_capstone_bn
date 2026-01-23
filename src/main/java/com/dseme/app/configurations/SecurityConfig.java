package com.dseme.app.configurations;
import com.dseme.app.filters.FacilitatorAuthorizationFilter;
import com.dseme.app.filters.JwtAuthenticationFilter;
import com.dseme.app.filters.MEOfficerAuthorizationFilter;
import com.dseme.app.services.auth.AuthEntryPointJwt;
import com.dseme.app.services.auth.CustomAccessDeniedHandler;
import com.dseme.app.services.auth.CustomOAuth2FailureHandler;
import com.dseme.app.services.auth.CustomOAuth2SuccessHandler;
import com.dseme.app.services.auth.CustomOAuth2UserService;
import com.dseme.app.services.users.UserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailService userDetailService;
    private final JwtAuthenticationFilter authenticationJwtTokenFilter;
    private final FacilitatorAuthorizationFilter facilitatorAuthorizationFilter;
    private final MEOfficerAuthorizationFilter meOfficerAuthorizationFilter;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomOAuth2FailureHandler customOAuth2FailureHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final AuthEntryPointJwt authEntryPointJwt;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(BCryptPasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailService);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    /**
     * Creates a ClientRegistrationRepository bean for OAuth2.
     * Only creates the bean if OAuth2 credentials are configured via environment variables.
     * If not configured, the bean won't be created and OAuth2 login will be disabled.
     * 
     * To enable Google OAuth2 login, set these environment variables:
     * - GOOGLE_CLIENT_ID
     * - GOOGLE_CLIENT_SECRET
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "spring.oauth2.client.registration.google",
            name = {"client-id", "client-secret"},
            matchIfMissing = false
    )
    public ClientRegistrationRepository clientRegistrationRepository(
            org.springframework.core.env.Environment environment) {
        String clientId = environment.getProperty("spring.oauth2.client.registration.google.client-id");
        String clientSecret = environment.getProperty("spring.oauth2.client.registration.google.client-secret");
        
        // Validate credentials are present and not empty
        if (clientId == null || clientId.trim().isEmpty() || clientSecret == null || clientSecret.trim().isEmpty()) {
            throw new IllegalStateException(
                "OAuth2 Google credentials are required but not found. " +
                "Please set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET environment variables."
            );
        }

        // Spring Boot auto-configuration should handle this, but we provide a fallback
        String redirectUri = environment.getProperty("spring.oauth2.client.registration.google.redirect-uri",
                "http://localhost:8088/login/oauth2/code/google");

        ClientRegistration registration = ClientRegistration
                .withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope("email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo")
                .userNameAttributeName("id") // Google v2 userinfo returns 'id', not 'sub'
                .clientName("Google")
                .build();

        log.info("Google OAuth2 login configured successfully. Google sign-in button will appear on /login page.");
        
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, 
            AuthenticationProvider authenticationProvider,
            @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authenticationProvider(authenticationProvider)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable) // Disable form login for REST API
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authEntryPointJwt) // Use custom JSON entry point
                        .accessDeniedHandler(customAccessDeniedHandler) // Use custom JSON access denied handler
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Allow preflight requests
                        .requestMatchers(
                                "/health",
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password",
                                "/api/auth/verify",
                                "/api/auth/resend-verification",
                                "/api/auth/google",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/login/**",
                                "/oauth2/**"
                        ).permitAll()
                        // UNASSIGNED users can only request roles and view profile
                        .requestMatchers("/api/users/request/role").hasRole("UNASSIGNED")
                        .requestMatchers("/api/users/profile").authenticated()
                        // ADMIN has full access
                        .requestMatchers("/api/access-requests/**").hasRole("ADMIN")
                        .requestMatchers("/api/facilitators/**").hasRole("ADMIN")
                        // ME Portal endpoints - CRITICAL: Match actual controller paths
                        .requestMatchers("/api/courses/**").hasAnyRole("ADMIN", "ME_OFFICER")
                        .requestMatchers("/api/cohorts/**").hasAnyRole("ADMIN", "ME_OFFICER", "FACILITATOR")
                        .requestMatchers("/api/participants/**").hasAnyRole("ADMIN", "ME_OFFICER", "FACILITATOR")
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "ME_OFFICER", "DONOR")
                        // Facilitator specific endpoints
                        .requestMatchers("/api/facilitator/**").hasRole("FACILITATOR")
                        // ME_OFFICER specific endpoints
                        .requestMatchers("/api/me-officer/**").hasRole("ME_OFFICER")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless for JWT
                );
        
        // Configure OAuth2 login if ClientRegistrationRepository bean is available
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .defaultSuccessUrl("/", true) // Redirect after successful login
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .successHandler(customOAuth2SuccessHandler)
                    .failureHandler(customOAuth2FailureHandler)
            );
        }
        
        // JWT authentication filter runs first to extract and validate JWT
        http.addFilterBefore(authenticationJwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
                // Authorization filters run after JWT auth to validate role-specific access
                .addFilterAfter(facilitatorAuthorizationFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(meOfficerAuthorizationFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "https://dse-me-a86v.onrender.com"
        ));
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));
        
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
