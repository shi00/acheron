package com.silong.cloud.connector.configure;

import com.silong.cloud.connector.openstack.HwCloudOpenstackConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 华为云连接器自动配置。
 *
 * @author louis sin
 * @version 1.0
 * @since 20180407
 */
@Configuration
@ConditionalOnProperty(name = "connector.openstack.default-region", havingValue = "", matchIfMissing = false)
@ConditionalOnClass(value = {HwCloudOpenstackConnector.class})
@ConditionalOnMissingBean(HwCloudOpenstackConnector.class)
@EnableConfigurationProperties(value = {RestClientProperties.class, OpenstackProperties.class,
    Openstack4jProperties.class})
public class HwCloudConnectorAutoConfiguration {

  /**
   * openstack环境配置
   */
  @Autowired
  private OpenstackProperties openstackProps;

  /**
   * openstack4j sdk配置
   */
  @Autowired
  private Openstack4jProperties openstack4jProps;

  /**
   * 配置信息
   */
  @Autowired
  private RestClientProperties restClientProps;

  /**
   * 华为云连接器
   *
   * @return 连接器
   */
  @Bean
  HwCloudOpenstackConnector HwCloudOpenstackConnector() {
    return new HwCloudOpenstackConnector(openstackProps, openstack4jProps, restClientProps);
  }

}
