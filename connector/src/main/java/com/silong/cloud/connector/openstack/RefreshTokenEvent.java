package com.silong.cloud.connector.openstack;

import com.huawei.openstack4j.model.identity.v3.Token;
import java.util.EventObject;

/**
 * 刷新token事件
 *
 * @author louis sin
 * @version 1.0
 * @since 20180609
 */
public class RefreshTokenEvent extends EventObject {

  private static final long serialVersionUID = 4063626515343256183L;

  /**
   * 构造方法
   *
   * @param newToken 新token
   */
  public RefreshTokenEvent(Object newToken) {
    super(newToken);
  }

  /**
   * 获取新token
   *
   * @return 新token
   */
  public Token getRenewToken() {
    return (Token) getSource();
  }

}
