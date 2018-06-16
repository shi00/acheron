package com.silong.cloud.auth.jwt;

import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import reactor.core.publisher.Mono;

public class VerifySignedJWT {

  public static Mono<SignedJWT> check(String token) {
    try {
      return Mono.just(SignedJWT.parse(token));
    } catch (ParseException e) {
      return Mono.empty();
    }
  }
}