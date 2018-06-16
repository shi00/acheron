package com.silong.common.enu;

/**
 * http协议
 *
 * @author louis sin
 * @version 1.0
 * @since 20180609
 */
public enum Protocol {

  /**
   * http
   */
  HTTP("http"),

  /**
   * https
   */
  HTTPS("https");

  private final String protocol;

  private Protocol(String protocol) {
    this.protocol = protocol;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Enum#toString()
   */
  @Override
  public String toString() {
    return protocol;
  }
}