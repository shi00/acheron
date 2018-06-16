package com.silong.cloud.auth.configure;

import static com.silong.cloud.auth.helper.ErrorCode.AUTHENTICATION_FAILED;
import static com.silong.cloud.auth.helper.ErrorCode.INSUFFICIENT_PERMISSIONS;
import static org.springframework.security.config.web.server.SecurityWebFiltersOrder.FIRST;

import com.google.common.collect.Lists;
import com.silong.cloud.auth.WebFilterChainServerJWTAuthenticationSuccessHandler;
import com.silong.common.i18n.Messages;
import com.silong.common.metadata.ErrorDetails;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import reactor.core.publisher.Mono;

/**
 * 配置鉴权
 *
 * @author louis sin
 * @version 1.0
 * @since 20180602
 */
@EnableWebFluxSecurity
@EnableConfigurationProperties(AuthenticationProperties.class)
public class AuthenticationAutoConfigure {

  /**
   * 鉴权配置
   */
  @Autowired
  private AuthenticationProperties props;

  /**
   * 错误描述
   */
  @Autowired
  private Messages messages;

  /**
   * For Spring Security webflux, a chain of filters will provide user authentication and
   * authorization, we add custom filters to enable JWT token approach.
   *
   * @param http An initial object to build common filter scenarios. Customized filters are added
   * here.
   * @return SecurityWebFilterChain A filter chain for web exchanges that will provide security
   */
  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http
        // 开启跨域保护
        .csrf()

        // 开启基础鉴权
        .and().httpBasic().disable()

        // 定制权限不足异常处理
        .exceptionHandling().accessDeniedHandler((exchange, e) -> {
      ServerHttpResponse response = exchange.getResponse();
      return response.writeAndFlushWith(Mono.fromRunnable(() -> {
        ErrorDetails.builder().code(INSUFFICIENT_PERMISSIONS)
            .message(messages.get(INSUFFICIENT_PERMISSIONS, e.getMessage())).build();
      }));
    })

        // 定制鉴权失败异常处理
        .authenticationEntryPoint((exchange, e) -> {
          ServerHttpResponse response = exchange.getResponse();
          return response.writeAndFlushWith(Mono.fromRunnable(() -> {
            ErrorDetails.builder().code(AUTHENTICATION_FAILED)
                .message(messages.get(AUTHENTICATION_FAILED, e.getMessage())).build();
          }));
        })

        // 添加鉴权过滤器
        // 定制需要鉴权的请求路径
        .and().addFilterAt(getAuthWebFilter(), FIRST).authorizeExchange()
        .pathMatchers(
            Stream.concat(props.getAuthWhiteList().stream(), Stream.of(props.getAuthUrl()))
                .toArray(String[]::new))
        .permitAll().anyExchange().authenticated();

    return http.build();
  }

  @Bean
  AuthenticationWebFilter getAuthWebFilter() {
    AuthenticationWebFilter authenticationJWT = new AuthenticationWebFilter(userDetailsService());
    authenticationJWT
        .setAuthenticationSuccessHandler(new WebFilterChainServerJWTAuthenticationSuccessHandler());
    return authenticationJWT;
  }

  @Bean
  UserDetailsRepositoryReactiveAuthenticationManager userDetailsService() {
    return new UserDetailsRepositoryReactiveAuthenticationManager(userName -> Mono.fromSupplier(
        () -> new User(userName, "changeit",
            Lists.newArrayList(new SimpleGrantedAuthority("administrator")))));
  }
}
