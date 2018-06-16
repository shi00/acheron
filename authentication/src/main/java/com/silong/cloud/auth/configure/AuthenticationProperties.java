package com.silong.cloud.auth.configure;

import static java.util.Collections.emptySet;

import java.time.Duration;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * openstack4j配置信息
 *
 * @author louis sin
 * @version 1.0
 * @since 20161126
 */
@Data
@Builder
@Validated
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "authentication")
public class AuthenticationProperties {

  private static final String DEFAULT_AUTH_TOKEN_URL = "/v1.0/auth/tokens";
  private static final String DEFAULT_TOKEN_HEADER_KEY = "X-Access-Token";
  /**
   * token配置
   */
  @Valid
  @Default
  @NestedConfigurationProperty
  private Token token = new Token();
  /**
   * 鉴权请求url
   */
  @Default
  @NotBlank
  private String authUrl = DEFAULT_AUTH_TOKEN_URL;
  /**
   * 管理url
   */
  @Default
  @NotEmpty
  private Set<String> adminUrlList = emptySet();
  /**
   * 鉴权白名单
   */
  @Default
  @NotEmpty
  private Set<String> authWhiteList = emptySet();

  /**
   * token相关配置
   *
   * @author louis sin
   * @version 1.0
   * @since 20180602
   */
  @Data
  @Builder
  @Validated
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Token {

    /**
     * token超时时间
     */
    @NotNull
    @Default
    private Duration expires = Duration.ofDays(1);

    /**
     * token在请求头内的key
     */
    @Default
    @NotBlank
    private String tokenHeaderKey = DEFAULT_TOKEN_HEADER_KEY;
  }
}
