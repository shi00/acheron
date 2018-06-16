package com.silong.cloud.connector.configure;

import java.time.Duration;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * rest client配置信息
 *
 * @author louis sin
 * @version 1.0
 * @since 20180606
 */
@Data
@Builder
@Validated
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "connector.rest.client")
public class RestClientProperties {

  /**
   * 连接超时时间，默认10秒
   */
  @NotNull
  private Duration connectTimeout = Duration.ofSeconds(10);

  /**
   * 读超时时间，默认15秒
   */
  @NotNull
  private Duration readTimeout = Duration.ofSeconds(15);

  /**
   * 是否启用http2，默认false
   */
  private boolean enableHttp2;

  /**
   * 启用的加密协议版本集合，默认：TLS v1.2
   */
  @NotEmpty
  private String[] sslProtocols = new String[]{"TLSv1.2", "TLSv1.1", "TLSv1.0"};

}
