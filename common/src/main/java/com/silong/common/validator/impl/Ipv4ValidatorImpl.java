package com.silong.common.validator.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.http.conn.util.InetAddressUtils.isIPv4Address;

import com.silong.common.validator.annotation.Ipv4Address;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 检查ipv4地址合法性
 *
 * @author louis sin
 * @version 1.0
 * @since 20170529
 */
public class Ipv4ValidatorImpl implements ConstraintValidator<Ipv4Address, String> {

  /**
   * 校验字符串是否合法
   *
   * @param ip 值
   * @param context 校验上下文
   * @return 合法返回true，否则false
   */
  @Override
  public boolean isValid(String ip, ConstraintValidatorContext context) {
    if (isNotBlank(ip)) {
      return isIPv4Address(ip);
    }
    return true;
  }

  /**
   * 初始化校验器
   *
   * @param constraintAnnotation 声明
   */
  @Override
  public void initialize(Ipv4Address constraintAnnotation) {
  }

}
