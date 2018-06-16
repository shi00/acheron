package com.silong.cloud.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;

/**
 * A service to create JWT objects, this one is used when an exchange provides basic authentication.
 * If authentication is successful, a token is added in the response
 */
public class JWTTokenService {

  /**
   * Create and sign a JWT object using information from the current authenticated principal
   *
   * @param subject Name of current principal
   * @param credentials Credentials of current principal
   * @param authorities A collection of granted authorities for this principal
   * @return String representing a valid token
   */
  public String generateToken(String subject, Object credentials,
      Collection<? extends GrantedAuthority> authorities) {
    // TODO refactor this nasty code
    // Prepare JWT with claims set
    JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(subject).issuer("rapha.io")
        .expirationTime(new Date(new Date().getTime() + 60 * 1000))
        .claim("auths", authorities.parallelStream().map(auth -> (GrantedAuthority) auth)
            .map(a -> a.getAuthority()).collect(Collectors.joining(",")))
        .build();

    SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);

    // Apply the HMAC protection
    try {
      signedJWT.sign(new JWTSignerProvider().getSigner());
    } catch (JOSEException e) {
      e.printStackTrace();
    }

    return signedJWT.serialize();
  }
}
