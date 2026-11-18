package com.canchas.web.config;

import com.canchas.web.security.CustomUserDetailsService;
import com.canchas.web.security.ActiveRoleAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Ignorar recursos estaticos en la cadena de filtros de seguridad para que
     * NUNCA requieran autenticacion ni sean guardados en el SavedRequest.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
            "/css/**",
            "/js/**",
            "/images/**",
            "/img/**",
            "/favicon.ico",
            "/fonts/**",
            "/webjars/**"
        );
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // RequestCache configurado para NUNCA guardar recursos estaticos ni APIs
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        RequestMatcher nonStaticMatcher = request -> !isStaticOrInvalidUrl(request.getRequestURI());
        requestCache.setRequestMatcher(nonStaticMatcher);

        http
            .authenticationProvider(authenticationProvider())
            .requestCache(cache -> cache.requestCache(requestCache))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/inicio",
                    "/login",
                    "/registro",
                    "/registro/validar-dni",
                    "/retenciones", "/retenciones/**",
                    "/nosotros",
                    "/torneos",
                    "/torneos/como-funciona",
                    "/canchas",
                    "/canchas/{id}",
                    "/api/canchas/{id}/disponibilidad",
                    "/api/canchas/*/disponibilidad",
                    "/disponibilidad",
                    "/api/disponibilidad/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/img/**",
                    "/favicon.ico",
                    "/error",
                    "/error/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/torneos/{slug}").permitAll()
                .requestMatchers("/torneos/{slug}/inscribirse").access((authentication, context) ->
                    new AuthorizationDecision(ActiveRoleAccess.canAccess(authentication.get(), context.getRequest(), "CLIENTE")))
                .requestMatchers("/seleccionar-rol", "/seleccionar-rol/**", "/cambiar-rol").authenticated()
                .requestMatchers("/admin/**").access((authentication, context) ->
                    new AuthorizationDecision(ActiveRoleAccess.canAccess(authentication.get(), context.getRequest(), "ADMIN")))
                .requestMatchers("/cliente/**").access((authentication, context) ->
                    new AuthorizationDecision(ActiveRoleAccess.canAccess(authentication.get(), context.getRequest(), "CLIENTE")))
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler((request, response, authentication) -> {
                    request.getSession().removeAttribute(ActiveRoleAccess.SESSION_ATTRIBUTE);
                    boolean isAdmin = authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    
                    SavedRequest savedReq = requestCache.getRequest(request, response);
                    
                    // Si el SavedRequest apunta a un recurso estatico o invalido, purgarlo
                    if (savedReq != null && isStaticOrInvalidUrl(savedReq.getRedirectUrl())) {
                        requestCache.removeRequest(request, response);
                        savedReq = null;
                    }

                    if (isAdmin) {
                        requestCache.removeRequest(request, response);
                        response.sendRedirect("/seleccionar-rol");
                    } else {
                        if (com.canchas.web.security.VisitorHoldSession.hasFlow(request.getSession(false))) {
                            requestCache.removeRequest(request, response);
                            response.sendRedirect(request.getContextPath() + "/retenciones/continuar");
                            return;
                        }
                        // CLIENTE: sigue SavedRequest de flujo funcional (ej. reservas)
                        if (savedReq != null) {
                            String redirectUrl = savedReq.getRedirectUrl();
                            if (!isStaticOrInvalidUrl(redirectUrl)) {
                                response.sendRedirect(redirectUrl);
                                return;
                            }
                        }
                        response.sendRedirect("/canchas");
                    }
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso Denegado");
                })
            );

        return http.build();
    }

    /**
     * Valida si una URL redirige a un archivo estatico o recurso no navegable
     */
    public static boolean isStaticOrInvalidUrl(String url) {
        if (url == null || url.isBlank()) return true;
        String lower = url.toLowerCase();
        return lower.contains("/img/") ||
               lower.contains("/images/") ||
               lower.contains("/css/") ||
               lower.contains("/js/") ||
               lower.contains("/api/") ||
               lower.contains("/favicon.ico") ||
               lower.endsWith(".jpg") ||
               lower.endsWith(".jpeg") ||
               lower.endsWith(".png") ||
               lower.endsWith(".svg") ||
               lower.endsWith(".ico") ||
               lower.endsWith(".css") ||
               lower.endsWith(".js") ||
               lower.endsWith(".woff") ||
               lower.endsWith(".woff2");
    }
}
