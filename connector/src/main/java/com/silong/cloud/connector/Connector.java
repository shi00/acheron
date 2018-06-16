package com.silong.cloud.connector;

import com.silong.common.enu.ConnectorType;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotEmpty;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 连接器接口。
 *
 * @author louis sin
 * @version 1.0
 * @since 20180407
 */
public interface Connector {

  /**
   * 订阅连接器事件
   *
   * @param listeners 监听器
   */
  void listen(@NotEmpty ConnectorListener... listeners);

  /**
   * 获取rest客户端
   *
   * @return 客户端
   */
  @Nonnull
  WebClient getRestClient();

  /**
   * 连接器类型
   *
   * @return 类型
   */
  @Nonnull
  ConnectorType type();
}
