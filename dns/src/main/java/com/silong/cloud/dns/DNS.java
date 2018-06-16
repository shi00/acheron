package com.silong.cloud.dns;

import static com.silong.common.Utilities.testConnectivity;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.apache.commons.lang3.StringUtils.substringBetween;
import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

/**
 * dns工具
 *
 * @author louis sin
 * @version 1.0
 * @since 20180401
 */
@Slf4j
public class DNS {

  static final String DNS_SERVER = "dns.server";
  private static final int WAITING_REFRESH_TIMEOUT = 30;
  private static final String REFRESH_HOSTS_TIMER = "Refresh-Hosts-Timer";
  private static final String START_FLAG = "######################### START-INSERT #########################";
  private static final String END_FLAG = "######################### END-INSERT   #########################";
  private static final String HOSTS_SPLITER = "============================== hosts ===============================";
  /**
   * linux OS hosts文件路径
   */
  private static final String LINUX_HOSTS_FILE = "/etc/hosts";

  /**
   * windows OS hosts文件路径
   */
  private static final String WINDOWS_HOSTS_FILE = "C:\\Windows\\System32\\drivers\\etc\\hosts";
  /**
   * hosts文件
   */
  private final File hosts;
  /**
   * 待解析的url集合
   */
  private final Collection<URL> resolvable = Lists.newLinkedList();
  /**
   * hosts文件刷新间隔
   */
  private final Duration refreshInterval;
  /**
   * dns服务器列表
   */
  private String servers =
      isBlank(System.getProperty(DNS_SERVER)) ? EMPTY : System.getProperty(DNS_SERVER);
  /**
   * 定时刷新hosts线程
   */
  private ScheduledExecutorService executor = Executors
      .newSingleThreadScheduledExecutor(r -> new Thread(r, REFRESH_HOSTS_TIMER));

  /**
   * 构造方法
   *
   * @param refreshInterval hosts文件刷新间隔
   */
  public DNS(Duration refreshInterval) {
    this.refreshInterval = Objects
        .requireNonNull(refreshInterval, "refreshInterval must not be null.");
    if (IS_OS_LINUX) {
      hosts = new File(LINUX_HOSTS_FILE);
    } else if (IS_OS_WINDOWS) {
      hosts = new File(WINDOWS_HOSTS_FILE);
    } else {
      throw new UnsupportedOSException("The system can only run on windows or linux.");
    }
  }

  /**
   * 启动定时任务
   */
  @javax.annotation.PostConstruct
  private void start() {
    executor.scheduleAtFixedRate(() -> refreshHosts(resolvable), 0, refreshInterval.toSeconds(),
        TimeUnit.SECONDS);
  }

  private String getHostsMapping(@Nonnull Collection<URL> urls) {
    return urls.stream().map(url -> {

      String host = url.getHost();
      if (InetAddresses.isInetAddress(host)) {
        return null;
      }

      // 先查询ipv4
      List<Record> resolveRecord = resolveRecord(host, Type.A, DClass.IN);
      if (isEmpty(resolveRecord)) {
        // 如果没有则查询ipv6
        resolveRecord = resolveRecord(host, Type.AAAA, DClass.IN);
      }

      if (isEmpty(resolveRecord)) {
        return null;
      }

      return resolveRecord.stream()
          .map(
              record -> record instanceof ARecord ? ((ARecord) record).getAddress().getHostAddress()
                  : ((AAAARecord) record).getAddress().getHostAddress())
          .filter(ipStr -> testConnectivity(ipStr, getPort(url)))
          .map(ipStr -> String.format("%s %s", ipStr, host)).collect(
              Collectors
                  .joining(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()));
    }).filter(StringUtils::isNotBlank)
        .collect(Collectors
            .joining(System.lineSeparator(), System.lineSeparator(), System.lineSeparator()));
  }

  private int getPort(URL url) {
    int port = url.getPort();
    if (-1 == port) {
      port = url.getDefaultPort();
    }
    return port;
  }

  /**
   * 解析url，并写入hosts文件，如果给定url列表为{@code null}或empty则直接返回
   *
   * @param urls urls
   */
  public void refreshHosts(@Nullable Collection<URL> urls) {
    if (isNotEmpty(urls)) {

      // 此任务只能在刷新线程执行
      if (Thread.currentThread().getName().equals(REFRESH_HOSTS_TIMER)) {
        String newContent = getHostsMapping(urls);

        try {
          String oldContent = FileUtils.readFileToString(hosts, Charset.defaultCharset());

          // 如果是首次写入，在hosts文件尾追加
          int startFlagCount = countMatches(oldContent, START_FLAG);
          int endFlagCount = countMatches(oldContent, END_FLAG);
          if (0 == startFlagCount && 0 == endFlagCount) {
            FileUtils.writeStringToFile(hosts,
                System.lineSeparator() + START_FLAG + newContent + END_FLAG,
                Charset.defaultCharset(), true);
          }

          // 如果已经写入，则替换已有内容
          else if (1 == startFlagCount && 1 == endFlagCount
              && indexOf(oldContent, START_FLAG) < indexOf(oldContent, END_FLAG)) {

            FileUtils.writeStringToFile(hosts,
                replace(oldContent,
                    START_FLAG + substringBetween(oldContent, START_FLAG, END_FLAG) + END_FLAG,
                    START_FLAG + newContent + END_FLAG, 1),
                Charset.defaultCharset(), false);
          } else {
            log.error(
                "Failed to resolve hosts and cannot update the mapping between ip address and domain name in it."
                    + System.lineSeparator() + HOSTS_SPLITER + System.lineSeparator() + oldContent
                    + System.lineSeparator() + HOSTS_SPLITER);
          }
        } catch (IOException e) {
          log.error("Failed to read/write " + hosts, e);
        }
      } else {
        try {
          executor.submit(() -> refreshHosts(resolvable))
              .get(WAITING_REFRESH_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
          log.error(String.format("Failed to refresh hosts with %s.",
              urls.stream().map(URL::toString).collect(Collectors.joining(", ", "[", "]"))), e);
        }
      }
    }
  }

  /**
   * 解析DNS记录<br> 如果给定的主机为ip则直接返回空列表
   *
   * @param host 主机，待解析的域名
   * @param type 查询的记录类型 {@link org.xbill.DNS.Type}
   * @param dclass 查询的记录类型 {@link org.xbill.DNS.DClass}
   * @return 记录集合
   * @throws IllegalArgumentException 参数非法
   * @throws IllegalArgumentException The name is not a valid DNS name
   */
  @Nonnull
  public List<Record> resolveRecord(@NotBlank String host, int type, int dclass) {
    List<Record> result = Collections.emptyList();
    if (InetAddresses.isInetAddress(host)) {
      return result;
    }

    try {
      Lookup lookup = new Lookup(host, type, dclass);
      Record[] records = lookup.run();
      int resultCode = lookup.getResult();
      switch (resultCode) {
        case Lookup.SUCCESSFUL:
          result = Arrays.asList(records);
          if (log.isInfoEnabled()) {
            log.info(String.format(
                "Successfully query DNS records against [%s]. Params[host:%s, type:%d, dclass:%d]->Result[%s]",
                servers, host, type, dclass,
                result.stream().map(Record::rdataToString).collect(Collectors.joining(", "))));
          }
          break;
        case Lookup.TRY_AGAIN:

          if (log.isWarnEnabled()) {
            log.warn(String.format(
                "Failed to query DNS records against [%s]. Params[host:%s, type:%d, dclass:%d]->Try again.",
                servers, host, type, dclass,
                result.stream().map(Record::rdataToString).collect(Collectors.joining(", "))));
          }

          lookup = new Lookup(host, type, dclass);
          records = lookup.run();
          resultCode = lookup.getResult();

          if (resultCode == Lookup.SUCCESSFUL) {
            if (log.isInfoEnabled()) {
              log.info(String.format(
                  "Successfully query DNS records against [%s]. Params[host:%s, type:%d, dclass:%d]->Result[%s]",
                  servers, host, type, dclass,
                  result.stream().map(Record::rdataToString).collect(Collectors.joining(", "))));
            }
            break;
          }

        case Lookup.TYPE_NOT_FOUND:
        case Lookup.UNRECOVERABLE:
        case Lookup.HOST_NOT_FOUND:
        default:
          log.error(String.format(
              "Failed to resolve record against [%s]. Params[host:%s, type:%d, dclass:%d]->Result[code:%d, desc:%s]",
              servers, host, type, dclass, resultCode, lookup.getErrorString()));
      }
    } catch (TextParseException e) {
      throw new IllegalArgumentException(e);
    }
    return result;
  }

}
