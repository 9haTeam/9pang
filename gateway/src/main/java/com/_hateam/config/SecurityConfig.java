package com._hateam.config;

import com._hateam.filter.JwtReactiveAuthenticationManager;
import com._hateam.filter.JwtSecurityContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import reactor.core.publisher.Mono;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.http.server.reactive.ServerHttpRequest;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {

    private final JwtReactiveAuthenticationManager authenticationManager;
    private final JwtSecurityContextRepository contextRepository;

    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authenticationManager(authenticationManager)
                .securityContextRepository(contextRepository)


                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/api/users/signup").permitAll()
                        .pathMatchers("/api/users/signin").permitAll()
                        .pathMatchers("/api/users/getusers").hasAuthority(UserRole.ADMIN.getRole()) 
                        .pathMatchers("/api/users/roles/**").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.PUT,"/api/users/**").hasAuthority(UserRole.ADMIN.getRole())                                  
                        .pathMatchers(HttpMethod.PUT,"/api/delivery-users/**").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.POST,"/api/delivery-users/add").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.DELETE,"/api/delivery-users/**").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.DELETE,"/api/users/**").hasAuthority(UserRole.ADMIN.getRole())       
                                   
                        .pathMatchers("/redis/**").hasAuthority(UserRole.ADMIN.getRole())

                        .pathMatchers(HttpMethod.POST,"/hubs").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.PATCH,"/hubs/**").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.DELETE,"/hubs/**").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.GET,"/hubs/**").permitAll()

                        .pathMatchers(HttpMethod.POST,"/hub-routes").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.PATCH,"/hub-routes/**").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.DELETE,"/hub-routes/**").hasAuthority(UserRole.ADMIN.getRole())
                        .pathMatchers(HttpMethod.GET,"/hub-routes/**").permitAll()

                        .pathMatchers(HttpMethod.POST,"/companies").hasAnyAuthority(UserRole.ADMIN.getRole(), UserRole.HUB.getRole())
                        .pathMatchers(HttpMethod.PATCH,"/companies/**").hasAnyAuthority(UserRole.ADMIN.getRole(), UserRole.HUB.getRole())
                        .pathMatchers(HttpMethod.DELETE,"/companies/**").hasAnyAuthority(UserRole.ADMIN.getRole(), UserRole.HUB.getRole())
                        .pathMatchers(HttpMethod.GET,"/companies/**").permitAll()

                        .pathMatchers("/products/**").permitAll()

                        .pathMatchers("/api/orders/**").permitAll()
                        .pathMatchers("/api/slack/**").permitAll()

                        .pathMatchers"/swagger-ui/**", "/v3/api-docs/**", "/*/v3/api-docs/**", "/webjars/**").permitAll()

                        .pathMatchers(HttpMethod.PUT,"/api/users/roles/**").hasAuthority(UserRole.ADMIN.getRole())

                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((exchange, ex) -> {
                            ServerHttpResponse response = exchange.getResponse();
                            response.setStatusCode(HttpStatus.FORBIDDEN);
                            String errorMessage = "{\"message\":\"적절하지 않은 토큰입니다.\"}";
                            DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes(StandardCharsets.UTF_8));
                            return response.writeWith(Mono.just(buffer));
                        })
                        .accessDeniedHandler((exchange, denied) -> {
                            ServerHttpResponse response = exchange.getResponse();
                            response.setStatusCode(HttpStatus.UNAUTHORIZED);
                            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                            String errorMessage = "{\"message\":\"적절한 권한이 아닙니다.\"}";
                            DataBuffer buffer = response.bufferFactory().wrap(errorMessage.getBytes(StandardCharsets.UTF_8));

                            return response.writeWith(Mono.just(buffer));
                        })
                )
                .build();
    }
}
