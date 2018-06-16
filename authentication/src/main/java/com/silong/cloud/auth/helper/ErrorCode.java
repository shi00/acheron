package com.silong.cloud.auth.helper;

/**
 * 鉴权相关错误码
 *
 * @author louis sin
 * @version 1.0
 * @since 20180602
 */
public interface ErrorCode {

  /**
   * 权限不足
   */
  String INSUFFICIENT_PERMISSIONS = "acheron.auth.0001";

  /**
   * 鉴权失败
   */
  String AUTHENTICATION_FAILED = "acheron.auth.0002";
}
