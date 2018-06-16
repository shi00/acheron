package com.silong.common.validator.impl;

import com.silong.common.validator.annotation.TwovarMutuallyExclusive;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 指定的成员变量不能同时为null，也不能同时有值
 *
 * @author louis sin
 * @version 1.0
 * @since 20170529
 */
public class TwovarMutuallyExclusiveValidatorImpl implements
    ConstraintValidator<TwovarMutuallyExclusive, Object> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(TwovarMutuallyExclusiveValidatorImpl.class.getName());
  private String firstField;
  private String secondField;

  /**
   * 初始化校验器
   *
   * @param constraintAnnotation 声明
   */
  @Override
  public void initialize(TwovarMutuallyExclusive constraintAnnotation) {
    firstField = constraintAnnotation.one();
    secondField = constraintAnnotation.other();
  }

  /**
   * 跨field校验
   *
   * @param value 值
   * @param context 校验上下文
   * @return 合法返回true，否则false
   */
  @Override
  public boolean isValid(Object value, final ConstraintValidatorContext context) {
    try {
      // 不能同时为null
      Object readField = FieldUtils.readField(value, firstField, true);
      Object readField2 = FieldUtils.readField(value, secondField, true);
      return (readField == null || readField2 != null) || (readField != null || readField2 == null);
    } catch (Exception e) {
      LOGGER.error("Failed to read value from " + value, e);
      return false;
    }
  }
}