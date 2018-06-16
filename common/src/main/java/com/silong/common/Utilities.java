package com.silong.common;

import static com.silong.common.enu.Constants.MAX_PORT;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Proxy;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nonnull;
import javax.net.ssl.X509TrustManager;
import javax.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.telnet.TelnetClient;
import org.hibernate.validator.constraints.Range;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.lang.Nullable;

/**
 * 通用工具
 *
 * @author louis sin
 * @version 1.0
 * @since 20180405
 */
@Slf4j
public final class Utilities {

  public static final int DEFAULT_TEST_CONNECTIVITY_TIMEOUT = 3000;

  /**
   * 不校验证书
   */
  public static final X509TrustManager DISABLE_CHECKE = new X509TrustManager() {

    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
        throws CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
        throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }
  };

  /**
   * 主机名
   */
  public static final String HOSTNAME = "HOSTNAME";

  /**
   * 计算机名
   */
  public static final String COMPUTERNAME = "COMPUTERNAME";

  /**
   * 构造方法
   */
  private Utilities() {
  }

  /**
   * 实例化对象
   *
   * @param clazz class
   * @param beanName bean名称
   * @param args 参数
   * @return 对象
   */
  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T> T instantiate(@Nonnull String clazz, @Nonnull String beanName,
      @Nullable Object... args) {
    try {
      Class<?> forName = Class.forName(clazz);
      if (args == null || args.length == 0) {
        return (T) forName.getDeclaredConstructor().newInstance();
      } else {
        Constructor<?> declaredConstructor = forName
            .getDeclaredConstructor(
                Arrays.stream(args).map(arg -> arg.getClass()).toArray(Class[]::new));
        return (T) BeanUtils.instantiateClass(declaredConstructor, args);
      }
    } catch (Exception e) {
      throw new BeanCreationException(beanName, String.format("Failed to instantiate %s.", clazz),
          e);
    }
  }

  /**
   * 测试localhost与指定主机和端口的连通性，连接超时时间3秒
   *
   * @param host 远程主机
   * @param port 端口号
   * @return 可连通返回{@code true}，否则{@code false}
   * @throws IllegalArgumentException 参数非法
   */
  public static boolean testConnectivity(@NotBlank String host,
      @Range(min = 0, max = MAX_PORT) int port) {
    return testConnectivity(host, port, DEFAULT_TEST_CONNECTIVITY_TIMEOUT, null);
  }

  /**
   * 测试localhost与指定主机和端口的连通性
   *
   * @param host 远程主机
   * @param port 端口号
   * @param timeout 连接超时时间，单位：ms。设置为0表示不超时
   * @param proxy 连接代理，可以为null
   * @return 可连通返回{@code true}，否则{@code false}
   * @throws IllegalArgumentException 参数非法
   */
  public static boolean testConnectivity(@NotBlank String host,
      @Range(min = 0, max = MAX_PORT) int port,
      @Range(min = 0) int timeout, @Nullable Proxy proxy) {

    if (isBlank(host)) {
      throw new IllegalArgumentException("host must not be null or empty.");
    }

    if (port < 0 || port > MAX_PORT) {
      throw new IllegalArgumentException(
          String.format("port must be between 0 and %d.", MAX_PORT));
    }

    if (timeout < 0) {
      throw new IllegalArgumentException("timeout must be great than or equals to 0.");
    }

    TelnetClient client = new TelnetClient();
    client.setConnectTimeout(timeout);
    if (proxy != null) {
      client.setProxy(proxy);
    }

    try {
      client.connect(host, port);
      if (log.isInfoEnabled()) {
        log.info(String.format("localhost successfully connected to %s:%d.", host, port));
      }
      return true;
    } catch (IOException e) {
      if (log.isWarnEnabled()) {
        log.warn(String.format("localhost cannot connect to %s:%d.", host, port), e);
      }
    } finally {
      if (client != null) {
        try {
          client.disconnect();
        } catch (IOException e) {
          // ignore
        }
      }
    }
    return false;
  }

  /**
   * 根据迭代器获取Stream(按成员顺序)
   *
   * @param it 迭代器
   * @param parallel 是否并行
   * @return Stream
   */
  @Nonnull
  public static <T> Stream<T> stream(@Nonnull Iterator<T> it, boolean parallel) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
        Objects.requireNonNull(it, "iterator can not be null."), Spliterator.ORDERED), parallel);
  }

  /**
   * 根据迭代器获取Stream(按成員順序)<br> 非并行。
   *
   * @param it 迭代器
   * @return Stream
   */
  @Nonnull
  public static <T> Stream<T> stream(@Nonnull Iterator<T> it) {
    return stream(it, false);
  }

  /**
   * 获取主机名(从环境变量获取)<br> 如果获取失败则返回空字符串("")。
   *
   * @return 主机名
   */
  @Nonnull
  public static String getHostName() {
    Map<String, String> env = System.getenv();
    if (env.containsKey(COMPUTERNAME)) {
      return env.get(COMPUTERNAME);
    } else if (env.containsKey(HOSTNAME)) {
      return env.get(HOSTNAME);
    } else {
      return EMPTY;
    }
  }
}
