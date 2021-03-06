package com.silong.cloud.auth;

import java.util.function.Function;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * A WebFilter that provides authorization for WebExchanges in the main filter chain, paths requests
 * marked  with authorization will be filtered to ensure they contain a valid JWT token.
 *
 * @author rafa
 */
public class JWTAuthorizationWebFilter implements WebFilter {

  private final ReactiveAuthenticationManager authenticationManager = new JWTReactiveAuthenticationManager();
  private ServerAuthenticationSuccessHandler authenticationSuccessHandler = new WebFilterChainServerAuthenticationSuccessHandler();
  private ServerWebExchangeMatcher requiresAuthenticationMatcher = ServerWebExchangeMatchers
      .pathMatchers("/api/**");
  private Function<ServerWebExchange, Mono<Authentication>> authenticationConverter = new ServerHttpBearerAuthenticationConverter();
  private ServerSecurityContextRepository securityContextRepository = NoOpServerSecurityContextRepository
      .getInstance();

  /**
   * Provide a custom filtering mechanism for WebExchanges matching a restricted path each one will
   * be verified and must contain valid Bearer token in its Authorization, then to respect a common
   * contract a dummy authentication manager will provide a successful authentication, invalid
   * tokens will be filtered one step before
   *
   * @param exchange a current WebExchange
   * @param chain a WebFilter chain to delegate the request in case it is valid
   * @return Void when filter is complete
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return this.requiresAuthenticationMatcher.matches(exchange)
        .filter(matchResult -> matchResult.isMatch())
        .flatMap(matchResult -> this.authenticationConverter.apply(exchange))
        .switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
        .flatMap(token -> authenticate(exchange, chain, token));
  }

  /**
   * A dummy authentication manager providing standard step authentication, but exchange correctness
   * will be performed in the filtering step. This is because JWT mechanism contains all required
   * information in the token itself this is called stateless session or client-side session.
   *
   * @param exchange Current WebExchange
   * @param chain Parent chain to pass successful authenticated exchanges
   * @param token The current authentication object
   * @return Void when authentication is complete
   */
  private Mono<Void> authenticate(ServerWebExchange exchange,
      WebFilterChain chain, Authentication token) {
    WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain);
    return this.authenticationManager.authenticate(token)
        .flatMap(authentication -> onAuthenticationSuccess(authentication, webFilterExchange));
  }

  /**
   * The current exchange will be passed trough the chain on successful authentication Spring
   * security will have all needed information to authorize our current exchange
   *
   * @param authentication The current authentication object
   * @param webFilterExchange Current authentication chain
   * @return Void when completing handler
   */
  private Mono<Void> onAuthenticationSuccess(Authentication authentication,
      WebFilterExchange webFilterExchange) {
    ServerWebExchange exchange = webFilterExchange.getExchange();
    SecurityContextImpl securityContext = new SecurityContextImpl();
    securityContext.setAuthentication(authentication);
    return this.securityContextRepository.save(exchange, securityContext)
        .then(this.authenticationSuccessHandler
            .onAuthenticationSuccess(webFilterExchange, authentication))
        .subscriberContext(
            ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
  }
}