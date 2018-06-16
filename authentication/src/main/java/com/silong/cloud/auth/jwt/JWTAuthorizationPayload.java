package com.silong.cloud.auth.jwt;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

public class JWTAuthorizationPayload {

  public static String extract(ServerWebExchange serverWebExchange) {
    return serverWebExchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
  }
}