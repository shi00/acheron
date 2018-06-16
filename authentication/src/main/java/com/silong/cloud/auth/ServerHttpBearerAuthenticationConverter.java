package com.silong.cloud.auth;

import com.silong.cloud.auth.jwt.JWTAuthorizationPayload;
import com.silong.cloud.auth.jwt.UsernamePasswordAuthenticationFromJWTToken;
import com.silong.cloud.auth.jwt.VerifySignedJWT;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * This converter extracts a bearer token from a WebExchange and returns an Authentication object if
 * the JWT token is valid. Validity means is well formed and signature is correct
 */
public class ServerHttpBearerAuthenticationConverter implements
    Function<ServerWebExchange, Mono<Authentication>> {

  private static final String BEARER = "Bearer ";
  private static final Predicate<String> matchBearerLength = authValue -> authValue.length()
      > BEARER.length();
  private static final Function<String, String> isolateBearerValue = authValue -> authValue
      .substring(BEARER.length(), authValue.length());

  /**
   * Apply this function to the current WebExchange, an Authentication object is returned when
   * completed.
   */
  @Override
  public Mono<Authentication> apply(ServerWebExchange serverWebExchange) {
    return Mono.justOrEmpty(serverWebExchange)
        .map(JWTAuthorizationPayload::extract)
        .filter(Objects::nonNull)
        .filter(matchBearerLength)
        .map(isolateBearerValue)
        .filter(token -> !token.isEmpty())
        .map(VerifySignedJWT::check)
        .map(UsernamePasswordAuthenticationFromJWTToken::create)
        .filter(Objects::nonNull);
  }
}
