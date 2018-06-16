package com.silong.cloud.server.v1.openstack;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.silong.cloud.connector.configure.OpenstackProperties;
import com.silong.cloud.connector.openstack.HwCloudOpenstackConnector;
import javax.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 华为云虚拟机服务基类
 *
 * @author louis sin
 * @version 1.0
 * @since 20180616
 */
@Slf4j
public abstract class AbstractHwCloudOpenstackService {

  /**
   * openstack配置
   */
  @Autowired
  private OpenstackProperties properties;

  /**
   * 连接器
   */
  @Autowired
  private HwCloudOpenstackConnector connector;

  /**
   * 获取openstack配置
   *
   * @return openstack配置
   */
  public OpenstackProperties getProperties() {
    return properties;
  }

  /**
   * 华为云连接器
   *
   * @return 连接器
   */
  public HwCloudOpenstackConnector getConnector() {
    return connector;
  }

  /**
   * 获取区域对应的租户id
   *
   * @param region 区域
   * @return 租户id
   */
  protected String getProjectId(String region) {
    return properties.getRegions().get(region).getProjectId();
  }

  /**
   * 获取区域，如果指定区域不是blank，则直接返回，否则返回default region
   *
   * @param region 区域
   * @return 区域
   */
  protected String getRegion(String region) {
    String result;
    if (isBlank(region)) {
      String defaultRegion = properties.getDefaultRegion();
      if (log.isInfoEnabled()) {
        log.info("There is no region in the request header, use the defaultRegion[{}].",
            defaultRegion);
      }
      result = defaultRegion;
    } else {
      result = region;
    }
    return result;
  }
}
