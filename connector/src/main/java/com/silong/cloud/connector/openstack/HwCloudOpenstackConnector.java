package com.silong.cloud.connector.openstack;

import static com.huawei.openstack4j.api.types.ServiceType.BLOCK_STORAGE;
import static com.huawei.openstack4j.api.types.ServiceType.COMPUTE;
import static com.huawei.openstack4j.api.types.ServiceType.IDENTITY;
import static com.huawei.openstack4j.api.types.ServiceType.IMAGE;
import static com.huawei.openstack4j.api.types.ServiceType.NETWORK;
import static com.huawei.openstack4j.core.transport.ClientConstants.HEADER_X_AUTH_TOKEN;
import static com.silong.common.Utilities.testConnectivity;
import static com.silong.common.enu.Constants.OBS;
import static com.silong.common.enu.Constants.REQUEST_HEADER_REGION_KEY;
import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.reflect.FieldUtils.readField;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.ACCEPT_CHARSET;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpHeaders.USER_AGENT;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.huawei.openstack4j.api.client.IOSClientBuilder.V3;
import com.huawei.openstack4j.core.transport.Config;
import com.huawei.openstack4j.model.common.Identifier;
import com.huawei.openstack4j.model.identity.v3.Endpoint;
import com.huawei.openstack4j.model.identity.v3.Service;
import com.huawei.openstack4j.model.identity.v3.Token;
import com.huawei.openstack4j.openstack.OSFactory;
import com.huawei.openstack4j.openstack.identity.v3.domain.KeystoneService;
import com.obs.services.ObsClient;
import com.silong.cloud.connector.Connector;
import com.silong.cloud.connector.ConnectorListener;
import com.silong.cloud.connector.configure.Openstack4jProperties;
import com.silong.cloud.connector.configure.OpenstackProperties;
import com.silong.cloud.connector.configure.OpenstackProperties.Domain;
import com.silong.cloud.connector.configure.OpenstackProperties.ExtOpenstackService;
import com.silong.cloud.connector.configure.OpenstackProperties.OpenstackRegion;
import com.silong.cloud.connector.configure.OpenstackProperties.Project;
import com.silong.cloud.connector.configure.OpenstackProperties.User;
import com.silong.cloud.connector.configure.RestClientProperties;
import com.silong.common.enu.ConnectorType;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLException;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.ByteArrayDecoder;
import org.springframework.core.codec.ByteArrayEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.CodecConfigurer.CustomCodecs;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * 华为云连接器。
 *
 * @author louis sin
 * @version 1.0
 * @since 20180613
 */
@Slf4j
public class HwCloudOpenstackConnector implements Connector {

  private static final String NETTY4_HTTP_CLIENT = "ReactorNetty/0.7.7.RELEASE";
  private static final String CONNECTOR_CACHES_REFRESH_TIMER = "Connector-Caches-Refresh-Timer";
  private static final String CONTEXT_REQUEST_START = "REQUEST_START";
  private static final int TOKEN_INITIAL_CAPACITY = 5;
  private static final int TOKEN_MAXIMUM_SIZE = 50;


  /**
   * connector缓存刷新线程
   */
  private final ListeningExecutorService backgroundRefreshPools = MoreExecutors
      .listeningDecorator(
          Executors.newSingleThreadExecutor(r -> new Thread(r, CONNECTOR_CACHES_REFRESH_TIMER)));

  /**
   * token缓存。key:区域 value:token
   */
  private final LoadingCache<String, Optional<Token>> tokenCache;

  /**
   * 基础rest客户端
   */
  private final WebClient baseClient;

  /**
   * openstack环境配置
   */
  private final OpenstackProperties openstackProps;

  /**
   * openstack4j sdk配置
   */
  private final Openstack4jProperties openstack4jProps;

  /**
   * 配置信息
   */
  private final RestClientProperties restClientProps;

  /**
   * 客户端缓存。key：服务类型，value：客户端
   */
  private final Map<String, WebClient> clients = Maps.newConcurrentMap();

  /**
   * 请求计数器
   */
  private final LongAdder numberOfRequests = new LongAdder();

  /**
   * 监听器列表
   */
  private final Map<String, List<ConnectorListener>> listeners = Maps.newConcurrentMap();

  /**
   * 构造方法
   *
   * @param openstackProps openstack配置
   * @param openstack4jProps openstack4j配置
   * @param restClientProps client配置
   */
  public HwCloudOpenstackConnector(@Nonnull OpenstackProperties openstackProps,
      @Nonnull Openstack4jProperties openstack4jProps,
      @Nonnull RestClientProperties restClientProps) {
    this.openstackProps = Objects
        .requireNonNull(openstackProps, "openstackProps must not be null.");
    this.openstack4jProps = Objects
        .requireNonNull(openstack4jProps, "openstack4jProps must not be null.");
    this.restClientProps = Objects
        .requireNonNull(restClientProps, "restClientProps must not be null.");
    this.tokenCache = initTokenCaches();
    this.baseClient = buildRestClient();
  }

  /**
   * 构造openstack token缓存
   *
   * @return token缓存
   */
  private LoadingCache<String, Optional<Token>> initTokenCaches() {
    return CacheBuilder.newBuilder().maximumSize(TOKEN_MAXIMUM_SIZE)
        .initialCapacity(TOKEN_INITIAL_CAPACITY)
        .recordStats()
        .removalListener(notification -> {
          if (log.isInfoEnabled()) {
            log.info(String
                .format("The token in the cache is removed. %s[%s: %s]", notification.getCause(),
                    notification.getKey(), notification.getValue()));
          }
        }).concurrencyLevel(openstackProps.getTokenConcurrentAccessLevel())
        .refreshAfterWrite(openstackProps.getTokenTimeout().toMinutes(), TimeUnit.MINUTES)
        .build(new CacheLoader<String, Optional<Token>>() {

          @Override
          @SuppressWarnings("unchecked")
          public Optional<Token> load(String region) throws Exception {
            //通过账号密码获取token
            Token token = getToken(region);

            //把扩展服务添加到token catalog内
            List<KeystoneService> catalog = (List<KeystoneService>) readField(token.getClass(),
                "catalog",
                true);
            catalog.addAll(openstackProps.getRegions().get(region).getExtServices());
            return Optional.of(token);
          }

          @Override
          public ListenableFuture<Optional<Token>> reload(String key, Optional<Token> oldValue)
              throws Exception {
            return backgroundRefreshPools.submit(() -> load(key));
          }
        });
  }


  /**
   * 根据账号密码获取token
   *
   * @param regionKey 区域
   * @return token
   */
  @Nonnull
  private Token getToken(@NotBlank String regionKey) {
    OpenstackRegion region = openstackProps.getRegions().get(regionKey);
    User user = region.getUser();
    String userId = user.getId();
    String userName = user.getName();
    String password = user.getPassword();
    Domain domain = region.getDomain();
    Project project = region.getProject();
    Identifier domainIdentifier = isNotBlank(domain.getId()) ? Identifier.byName(domain.getId())
        : Identifier.byName(domain.getName());

    V3 v3 = OSFactory.builderV3().endpoint(region.getEndpoint()).withConfig(Config.DEFAULT)
        .perspective(openstack4jProps.getPerspective()).provider(openstack4jProps.getProvider());
    if (isNotBlank(userId)) {
      v3 = v3.credentials(userId, password);
    } else {
      v3 = v3.credentials(userName, password, domainIdentifier);
    }

    if (isNotBlank(project.getId())) {
      v3 = v3.scopeToProject(Identifier.byId(project.getId()));
    } else {
      v3 = v3.scopeToProject(Identifier.byId(project.getName()), domainIdentifier);
    }
    return v3.authenticate().getToken();
  }

  @Nonnull
  private ApplicationProtocolConfig getApplicationProtocolConfig() {
    return restClientProps.isEnableHttp2()
        ? new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
        SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_2,
        ApplicationProtocolNames.HTTP_1_1)
        : new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.NO_ADVERTISE,
            SelectedListenerFailureBehavior.ACCEPT, ApplicationProtocolNames.HTTP_1_1);
  }

  @Nullable
  private SslContext getSslContext() {
    SslContext sc = null;
    try {
      sc = SslContextBuilder.forClient()
          .sslProvider(OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK)
          .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
          .protocols(restClientProps.getSslProtocols())
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .applicationProtocolConfig(getApplicationProtocolConfig()).build();
    } catch (SSLException e) {
      log.error("Failed to build SslContext for webClient.", e);
    }
    return sc;
  }

  @Nonnull
  private UriBuilderFactory getUriBuilderFactory() {
    return new DefaultUriBuilderFactory();
  }

  /**
   * 定制解码器
   *
   * @return 请求处理策略
   */
  @Nonnull
  private ExchangeStrategies getExchangeStrategies() {
    return ExchangeStrategies.builder().codecs(configurer -> {
      CustomCodecs cc = configurer.customCodecs();
      cc.decoder(new ByteArrayDecoder());
      cc.decoder(new Jackson2JsonDecoder());
      cc.decoder(StringDecoder.allMimeTypes());
      cc.encoder(new ByteArrayEncoder());
      cc.encoder(new Jackson2JsonEncoder());
    }).build();
  }

  /**
   * reactor http connector
   *
   * @return connector
   */
  private ReactorClientHttpConnector reactorClientHttpConnector() {
    return new ReactorClientHttpConnector(
        options -> options.compression(true).sslContext(getSslContext())
            .option(CONNECT_TIMEOUT_MILLIS, (int) restClientProps.getConnectTimeout().toMillis())
            .afterNettyContextInit(ctx -> ctx.addHandlerLast(
                new ReadTimeoutHandler(restClientProps.getReadTimeout().toMillis(),
                    MILLISECONDS))));
  }

  /**
   * 日志打印filter
   *
   * @return 过滤器
   */
  private ExchangeFilterFunction loggingFilter() {
    return (request, next) -> {
      numberOfRequests.increment();
      return next.exchange(request).zipWith(Mono.subscriberContext())
          // this must placed before
          // subscriberContext,
          // otherwise it
          // would be called in invalid order
          .doOnSubscribe(subscription -> {
            if (log.isInfoEnabled()) {
              log.info("{} started: {}", request.method(), request.url().toString());
            }

            if (log.isDebugEnabled()) {
              log.debug("headers:{} {}", System.lineSeparator(),
                  request.headers().entrySet().stream()
                      .map(e -> String.format("%s:%s", e.getKey(),
                          e.getValue().stream().collect(Collectors.joining("; "))))
                      .collect(Collectors.joining(System.lineSeparator())));
            }
          })

          // 缓存请求起始时间
          .subscriberContext(context -> context.put(CONTEXT_REQUEST_START, new Date()))

          // 打印响应结束时间
          .doOnNext(tuple -> {
            if (log.isInfoEnabled()) {
              Date startTime = tuple.getT2().get(CONTEXT_REQUEST_START);
              Date endTime = new Date();
              long delta = endTime.getTime() - startTime.getTime();
              log.info("{} finished {}: {} in {} ms", request.method(),
                  tuple.getT1().statusCode().toString(), request.url().toString(), delta);
            }
          }).map(Tuple2::getT1);
    };
  }

  /**
   * 鉴权失败时重新鉴权，避免token失效导致请求失败
   *
   * @return 过滤器
   */
  private ExchangeFilterFunction reAuthticationFilter() {
    // 对认证失败错误进行一次重试
    return (request, next) -> next.exchange(request).filter(cr -> cr.statusCode() == UNAUTHORIZED)
        .map(cr -> ClientRequest.from(request).headers(header -> {

          //读取请求头内的区域
          String region = header.getFirst(REQUEST_HEADER_REGION_KEY);

          // 清空缓存token
          tokenCache.invalidate(region);

          // 重新获取token，并通知token刷新事件
          Token token = getCachedToken(region);
          if (token != null) {
            dispatchEvent(new RefreshTokenEvent(token));
            header.replace(HEADER_X_AUTH_TOKEN, Lists.newArrayList(token.getId()));
            if (log.isInfoEnabled()) {
              log.info("Refresh the token and try again, {} {}.", request.method(),
                  request.url().toString());
            }
          } else {
            log.error("Failed to refresh request token cause by UNAUTHORIZED.");
          }
        }).build())

        // 确保token正确
        .filter(cr -> isNotBlank(cr.headers().getFirst(HEADER_X_AUTH_TOKEN))
        ).flatMap(next::exchange);
  }

  /**
   * 构造token过滤器
   *
   * @return 过滤器
   */
  private ExchangeFilterFunction addOpenstackTokenFilter() {
    return (request, next) -> next
        .exchange(
            ClientRequest.from(request).headers(
                headers -> headers.add(HEADER_X_AUTH_TOKEN, getTokenIdFromCache(headers.getFirst(
                    REQUEST_HEADER_REGION_KEY)))
            ).build());
  }

  /**
   * 构造rest client
   *
   * @return rest client
   */
  private WebClient buildRestClient() {
    return WebClient.builder()
        // 添加日志打印过滤器，打印请求时间
        .filter(loggingFilter())

        // 添加过滤器，为请求添加openstack token
        .filter(addOpenstackTokenFilter())

        // 对于鉴权错误的请求进行重试
        .filter(reAuthticationFilter())

        .defaultHeader(USER_AGENT, NETTY4_HTTP_CLIENT)
        .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_UTF8_VALUE)
        .defaultHeader(ACCEPT, APPLICATION_JSON_UTF8_VALUE)
        .defaultHeader(ACCEPT_CHARSET, UTF_8.name())
        .uriBuilderFactory(getUriBuilderFactory()).exchangeStrategies(getExchangeStrategies())
        .clientConnector(reactorClientHttpConnector()).build();
  }

  /**
   * 从缓存内读取token id
   *
   * @param region 区域
   * @return token id
   */
  @Nonnull
  private String getTokenIdFromCache(String region) {
    String result = EMPTY;
    try {
      Token token = tokenCache.get(region).orElse(null);
      if (token != null) {
        result = token.getId();
      }
    } catch (ExecutionException e) {
      log.error("Failed to get token from tokenCache.", e.getCause());
    }
    return result;
  }

  /**
   * 获取连接器类型
   *
   * @return 连接器类型
   */
  @Override
  @Nonnull
  public ConnectorType type() {
    return ConnectorType.HWCLOUD;
  }

  /**
   * 获取默认区域的client
   *
   * @return client
   */
  @Nonnull
  public WebClient getComputeRestClient() {
    return getComputeRestClient(openstackProps.getDefaultRegion());
  }

  /**
   * 获取默认区域的client
   *
   * @return client
   */
  @Nonnull
  public WebClient getIdentityRestClient() {
    return getComputeRestClient(openstackProps.getDefaultRegion());
  }

  /**
   * 获取默认区域的client
   *
   * @return client
   */
  @Nonnull
  public WebClient getImageRestClient() {
    return getComputeRestClient(openstackProps.getDefaultRegion());
  }

  /**
   * 获取默认区域的client
   *
   * @return client
   */
  @Nonnull
  public WebClient getNetworkRestClient() {
    return getComputeRestClient(openstackProps.getDefaultRegion());
  }

  /**
   * 获取默认区域的client
   *
   * @return client
   */
  @Nonnull
  public WebClient getVolumeRestClient() {
    return getComputeRestClient(openstackProps.getDefaultRegion());
  }

  /**
   * 获取openstack compute服务客户端
   *
   * @param region 区域
   * @return 客户端
   */
  @Nonnull
  public WebClient getComputeRestClient(@NotBlank String region) {
    return getRestClient(COMPUTE.getType(), region);
  }

  /**
   * 获取openstack network服务客户端
   *
   * @param region 区域
   * @return 客户端
   */
  @Nonnull
  public WebClient getNetworkRestClient(@NotBlank String region) {
    return getRestClient(NETWORK.getType(), region);
  }

  /**
   * 获取openstack cinder服务客户端
   *
   * @param region 区域
   * @return 客户端
   */
  @Nonnull
  public WebClient getVolumeRestClient(@NotBlank String region) {
    return getRestClient(BLOCK_STORAGE.getType(), region);
  }

  /**
   * 获取openstack identity服务客户端
   *
   * @param region 区域
   * @return 客户端
   */
  @Nonnull
  public WebClient getIdentityRestClient(@NotBlank String region) {
    return getRestClient(IDENTITY.getType(), region);
  }

  /**
   * 获取openstack image服务客户端
   *
   * @param region 区域
   * @return 客户端
   */
  @Nonnull
  public WebClient getImageRestClient(@NotBlank String region) {
    return getRestClient(IMAGE.getType(), region);
  }

  /**
   * 返回Rest基础客户端
   *
   * @return client
   */
  @Override
  public WebClient getRestClient() {
    return baseClient;
  }

  /**
   * 根据服务类型或名称获取rest client
   *
   * @param nameOrType 服务名或类型
   * @param region 区域
   * @return client
   */
  @Nonnull
  public WebClient getRestClient(@NotBlank String nameOrType, @NotBlank String region) {
    if (isBlank(nameOrType)) {
      throw new IllegalArgumentException("nameOrType must not be null or blank.");
    }
    if (isBlank(region)) {
      throw new IllegalArgumentException("region must not be null or blank.");
    }
    return clients.computeIfAbsent(nameOrType,
        key -> baseClient.mutate().baseUrl(selectEndpointUrl(key, region)).build());
  }

  /**
   * 根据服务名或类型查询服务url
   *
   * @param nameOrType 服务名或类型
   * @param region 区域
   * @return url
   */
  @Nonnull
  private String selectEndpointUrl(@NotBlank String nameOrType, @NotBlank String region) {
    String result = EMPTY;
    try {
      Token token = tokenCache.get(region).orElse(null);
      if (token != null) {
        result = selectTargetUrl(selectTargetService(nameOrType, token.getCatalog()));
      }
    } catch (ExecutionException e) {
      log.error("Failed to get token from tokenCache.", e.getCause());
    }
    return result;
  }


  @Nullable
  private Service selectTargetService(@NotBlank String nameOrType,
      @NotEmpty Collection<? extends Service> services) {
    if (isEmpty(services)) {
      log.error("The catalog of token is empty or null.");
      return null;
    }

    Service result = services.stream().filter(Service::isEnabled)
        .filter(service -> equalsIgnoreCase(service.getType(), nameOrType) || equalsIgnoreCase(
            service.getName(),
            nameOrType))
        .findAny().orElse(null);
    if (result == null) {
      log.error("Can't find target service of specified nameOfType[{}] in {}.", nameOrType,
          services);
    }
    return result;
  }

  @Nonnull
  private String selectTargetUrl(@Nonnull Service service) {
    if (service == null) {
      return EMPTY;
    }

    List<? extends Endpoint> endpoints = service.getEndpoints();
    if (isEmpty(endpoints)) {
      log.error("The endpoints in {} are empty or null.", service);
      return EMPTY;
    }

    //过滤所有可用端点
    endpoints = endpoints.stream().filter(Endpoint::isEnabled)
        .collect(Collectors.toList());
    int size = endpoints.size();

    //如果只有一个端点地址则无需测试连通性，直接返回，否则测试连通性，选择一个可达端点
    return size == 0 ? EMPTY
        : size == 1 ? endpoints.get(0).getUrl().toString()
            : endpoints.parallelStream()
                .filter(e -> testConnectivity(e.getUrl().getHost(),
                    e.getUrl().getPort() == -1 ? e.getUrl().getDefaultPort()
                        : e.getUrl().getPort()))
                .findAny().get().toString();
  }

  /**
   * 获取HuaweiCloud OBS客户端，使用默认区域
   *
   * @return obs客户端
   */
  @Nonnull
  public ObsClient getObsClient() {
    return getObsClient(openstackProps.getDefaultRegion());
  }

  /**
   * 获取HuaweiCloud OBS客户端
   *
   * @param region 区域
   * @return obs客户端
   */
  @Nonnull
  public ObsClient getObsClient(@NotBlank String region) {
    Token cachedToken = getCachedToken(region);
    if (cachedToken == null) {
      throw new IllegalStateException(
          String.format("Failed to get cached token by region[%s].", region));
    }

    ExtOpenstackService extService;
    Service service = selectTargetService(OBS, cachedToken.getCatalog());
    if (service instanceof ExtOpenstackService) {
      extService = (ExtOpenstackService) service;
    } else {
      throw new IllegalStateException(
          "Failed to find Obs service from catalog of cached token.");
    }
    return new ObsClient(extService.getAk(), extService.getSk(), selectEndpointUrl(OBS, region));
  }

  /**
   * 注册事件监听器
   *
   * @param ls 监听器列表
   */
  @Override
  public void listen(@NotEmpty ConnectorListener... ls) {
    if (isNotEmpty(ls)) {
      throw new IllegalArgumentException("ls must not be null or empty.");
    }
    Arrays.stream(ls).forEach(listener -> {
      listeners.computeIfAbsent(listener.eventType().getName(), flag -> Lists.newArrayList())
          .add(listener);
    });
  }

  /**
   * 分发事件(异步)，通知监听器
   *
   * @param event 事件
   */
  private void dispatchEvent(EventObject event) {
    ForkJoinPool.commonPool().execute(() -> {
      List<ConnectorListener> eventListener = listeners.get(event.getClass().getName());
      if (isNotEmpty(eventListener)) {
        eventListener.forEach(ls -> ls.notify(event));
      }
    });
  }

  /**
   * 获取缓存内的token
   *
   * @param region 区域
   * @return 缓存token
   */
  @Nullable
  public Token getCachedToken(@NotBlank String region) {
    if (isBlank(region)) {
      throw new IllegalArgumentException("region must not be null or blank.");
    }
    Token result = null;
    try {
      result = tokenCache.get(region).orElse(null);
    } catch (ExecutionException e) {
      log.error(String.format("Failed to get token from cache with region[%s].", region),
          e.getCause());
    }
    return result;
  }
}
