package com.silong.cloud.dns;

import static com.silong.cloud.dns.DNS.DNS_SERVER;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.xbill.DNS.DClass.ANY;

import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.ResolverConfig;

/**
 * DNS服务自动配置
 *
 * @author louis sin
 * @version 1.0
 * @since 20180401
 */
@Configuration
@ConditionalOnMissingBean(DNS.class)
@ConditionalOnClass({Lookup.class, ResolverConfig.class})
@EnableConfigurationProperties(DNSProperties.class)
public class DNSAutoConfiguration {

  /**
   * dns配置属性
   */
  @Autowired
  private DNSProperties dnsProperties;

  /**
   * 注册dns工具
   */
  @Bean
  DNS dnsUtilities() {
    // 设置dns服务器列表
    System.getProperties().put(DNS_SERVER, isEmpty(dnsProperties.getServers()) ? EMPTY
        : dnsProperties.getServers().stream().collect(Collectors.joining(",")));

    // 设置查询超时
    Lookup.getDefaultResolver().setTimeout(dnsProperties.getResolveTimeout());
    Lookup.getDefaultCache(ANY).setMaxCache(dnsProperties.getDnsEntriesTimeout());
    Lookup.getDefaultCache(ANY).setMaxEntries(dnsProperties.getDnsCachedMaxEntries());
    Lookup.getDefaultCache(ANY).setMaxNCache(dnsProperties.getDnsNEntriesTimeout());
    return new DNS(dnsProperties.getRefreshHostsInterval());
  }

}
