package com.silong.cloud.dns;

/**
 * 不支持的操作系统异常
 *
 * @author louis sin
 * @version 1.0
 * @since 20180405
 */
public class UnsupportedOSException extends RuntimeException {

  private static final long serialVersionUID = 7307047913464393445L;

  /**
   * 构造方法
   *
   * @param msg 异常消息
   */
  public UnsupportedOSException(String msg) {
    super(msg);
  }

}
