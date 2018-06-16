package com.silong.cloud.dns;

import java.time.Duration;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * dns配置
 *
 * @author louis sin
 * @version 1.0
 * @since 20180401
 */
@Data
@Validated
@ConfigurationProperties("client.dns")
public class DNSProperties {

  private static final int ONE_HOURS = (int) Duration.ofHours(1).toSeconds();

  /**
   * 是否开启dns
   */
  private boolean enable;

  /**
   * 刷新hosts文件时间间隔
   */
  private Duration refreshHostsInterval;

  /**
   * dns服务器列表
   */
  @Nullable
  private List<String> servers;

  /**
   * 默认dns负响应记录超时时间，单位：秒<br> 默认1小时
   */
  private int dnsNEntriesTimeout = ONE_HOURS;

  /**
   * dns记录查询超时时间，单位：秒<br> 默认30秒
   */
  @Range(min = 0)
  private int resolveTimeout = 30;

  /**
   * dns记录在缓存内的超时时间，单位：秒<br> 默认1小时
   */
  @Range(min = 0)
  private int dnsEntriesTimeout = ONE_HOURS;

  /**
   * dns本地缓存最大条目数
   */
  @Range(min = 0)
  private int dnsCachedMaxEntries = 1000;
}
