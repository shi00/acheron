package com.silong.cloud.connector.configure;

import static com.silong.common.Aes256Utils.decrypt;
import static com.silong.common.enu.Constants.MAX_PORT;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import com.huawei.openstack4j.api.client.CloudProvider;
import com.huawei.openstack4j.api.identity.EndpointURLResolver;
import com.huawei.openstack4j.api.types.Facing;
import com.huawei.openstack4j.core.transport.Config;
import com.huawei.openstack4j.core.transport.ProxyHost;
import com.huawei.openstack4j.model.common.resolvers.ServiceVersionResolver;
import com.huawei.openstack4j.model.common.resolvers.StableServiceVersionResolver;
import com.huawei.openstack4j.openstack.OSFactory;
import com.huawei.openstack4j.openstack.identity.internal.DefaultEndpointURLResolver;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.validation.annotation.Validated;

/**
 * openstack4j配置信息
 *
 * @author louis sin
 * @version 1.0
 * @since 20161126
 */
@Data
@Slf4j
@Validated
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties(prefix = "connector.openstack4j")
public class Openstack4jProperties {

  /**
   * 默认服务端点解析器
   */
  public static final DefaultEndpointURLResolver DEFAULT_ENDPOINT_URL_RESOLVER = new DefaultEndpointURLResolver();

  /**
   * 默认读超时
   */
  public static final int DEFAULT_READ_TIMEOUT = 30000;

  /**
   * 针对每个路由的最大连接数
   */
  public static final int DEFAULT_MAX_CONNECTION_PER_ROUTE_SIZE = 5;

  /**
   * 默认连接池最大连接数
   */
  public static final int DEFAULT_MAX_CONNECTION_SIZE = 20;

  /**
   * 默认连接超时时间
   */
  public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
  /**
   * Init guard.
   */
  private final AtomicBoolean initGuard = new AtomicBoolean();
  /**
   * Init latch.
   */
  private final CountDownLatch initLatch = new CountDownLatch(1);
  /**
   * 连接超时(单位：毫秒)
   */
  private int connectTimeout = DEFAULT_CONNECTION_TIMEOUT;
  /**
   * 读超时(单位：毫秒)
   */
  private int readTimeout = DEFAULT_READ_TIMEOUT;
  /**
   * 主机名校验器
   */
  private HostnameVerifier hostNameVerifier;
  /**
   * 是否开启ssl认证，默认关闭
   */
  private boolean ignoreSslVerification;
  /**
   * nat主机名或ip
   */
  private String natHostOrIP;
  /**
   * 最大连接
   */
  private int maxConnections = DEFAULT_MAX_CONNECTION_SIZE;
  /**
   * 每路由最大连接数
   */
  private int maxConnectionsPerRoute = DEFAULT_MAX_CONNECTION_PER_ROUTE_SIZE;
  /**
   * url视图
   */
  private Facing perspective = Facing.PUBLIC;
  /**
   * 云服务提供者
   */
  private CloudProvider provider = CloudProvider.UNKNOWN;
  /**
   * api版本解析器，默认取稳定版，如需取最新版本请指定latest，如果使用自定义请配置定制解析器权限定名
   */
  private ServiceVersionResolver resolver = StableServiceVersionResolver.INSTANCE;
  /**
   * Resolves an Endpoint URL based on the Service Type and Facing perspective
   */
  private EndpointURLResolver endpointURLResolver = DEFAULT_ENDPOINT_URL_RESOLVER;
  /**
   * 代理配置
   */
  @Valid
  @NestedConfigurationProperty
  private Proxy proxy = new Proxy();

  /**
   * 是否开启Openstack4j日志打印
   */
  @Valid
  @NestedConfigurationProperty
  private Logging logging = new Logging();

  /**
   * 开启ssl，ssl上下文
   */
  @Valid
  @NestedConfigurationProperty
  private SSL ssl = new SSL();

  /**
   * 初始化Openstack4j的DEFAULT配置供后续使用。
   *
   * @return 默认配置
   */
  @Nonnull
  @SneakyThrows
  @PostConstruct
  private Config initConfigDefault() {
    // 多线程保护，初始化一次
    if (initGuard.compareAndSet(false, true)) {
      try {
        // 是否开启http请求日志
        OSFactory.enableHttpLoggingFilter(logging.enable);
        Config.DEFAULT.withConnectionTimeout(connectTimeout).withEndpointNATResolution(natHostOrIP)
            .withHostnameVerifier(hostNameVerifier).withEndpointURLResolver(endpointURLResolver)
            .withMaxConnections(maxConnections).withMaxConnectionsPerRoute(maxConnectionsPerRoute)
            .withProxy(proxy.toProxyHost()).withReadTimeout(readTimeout).withResolver(resolver)
            .withSSLContext(ssl.toSSLContext());
        if (ignoreSslVerification) {
          Config.DEFAULT.withSSLVerificationDisabled();
        }
      } finally {
        // 通知等待线程
        initLatch.countDown();
      }
    } else {
      if (initLatch.getCount() > 0) {
        initLatch.await();
      }
    }
    return Config.DEFAULT;
  }

  /**
   * openstack4j日志配置信息
   *
   * @author louis sin
   * @version 1.0
   * @since 20161126
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Logging {

    /**
     * 是否开启日志标识
     */
    private boolean enable;
  }

  /**
   * openstack4j代理配置信息
   *
   * @author louis sin
   * @version 1.0
   * @since 20161126
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Proxy {

    /**
     * 是否开启代理
     */
    private boolean enable;

    /**
     * 代理主机地址
     */
    @Nullable
    private String host;

    /**
     * 代理端口
     */
    @Range(max = MAX_PORT, min = 0)
    private int port;

    /**
     * 用户名
     */
    @Nullable
    private String userName;

    /**
     * 密码
     */
    @Nullable
    private transient String password;

    public void setPassword(@NotBlank String password) {
      this.password = decrypt(password);
    }

    /**
     * toString
     *
     * @return description
     */
    @Override
    public String toString() {
      return reflectionToString(this, SHORT_PREFIX_STYLE, false);
    }

    /**
     * 获取代理
     *
     * @return 代理
     */
    public ProxyHost toProxyHost() {
      return isNotBlank(password) && isNotBlank(userName) ? ProxyHost
          .of(host, port, userName, password)
          : ProxyHost.of(host, port);
    }
  }

  /**
   * openstack4j ssl配置
   *
   * @author louis sin
   * @version 1.0
   * @since 20161126
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SSL {

    /**
     * 证书文件路径
     */
    @Nullable
    private String keyStore;

    /**
     * 证书密码
     */
    @Nullable
    private String keyStorePassword;

    /**
     * 获取sslcontext
     *
     * @return sslcontext
     */
    public SSLContext toSSLContext() {
      SSLContext sslContext = null;
      if (isNotBlank(keyStorePassword) && isNotBlank(keyStore)) {
        ClassPathResource cpr = new ClassPathResource(keyStore);
        try {
          KeyStore ks = KeyStore.getInstance("PKCS12");
          try (InputStream input = cpr.getInputStream()) {
            ks.load(input, keyStorePassword.toCharArray());
          }

          // 指定tls
          sslContext = SSLContexts.custom().setProtocol("TLS")
              // ignore server verify
              .loadTrustMaterial(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                  return true;
                }
              }).loadKeyMaterial(ks, keyStorePassword.toCharArray()).build();
        } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException
            | KeyManagementException | UnrecoverableKeyException e) {
          log.error(String.format("Failed to load %s with ******", keyStore), e);
        }
      }
      return sslContext;
    }
  }
}
