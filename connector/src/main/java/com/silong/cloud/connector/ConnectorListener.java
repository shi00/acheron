package com.silong.cloud.connector;

import java.util.EventObject;

/**
 * 连接器事件监听器
 *
 * @author louis sin
 * @version 1.0
 * @since 20180609
 */
public interface ConnectorListener {

  /**
   * 获取事件类型
   *
   * @return 事件类型
   */
  Class<?> eventType();

  /**
   * 事件通知
   *
   * @param event 事件
   */
  void notify(EventObject event);
}
