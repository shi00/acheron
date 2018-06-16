package com.silong.common.validator.impl;

import com.silong.common.validator.annotation.EnumChecker;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * 枚举声明校验实现
 *
 * @author louis sin
 * @version 1.0
 * @since 20161124
 */
public class EnumValidatorImpl implements ConstraintValidator<EnumChecker, String> {

  /**
   * 枚举值列表
   */
  private List<String> values;

  /**
   * 校验枚举值是否合法
   *
   * @param value 值
   * @param context 校验上下文
   * @return 合法返回true，否则false
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return values.contains(value.toUpperCase());
  }

  /**
   * 初始化校验器
   *
   * @param constraintAnnotation 声明
   */
  @Override
  public void initialize(EnumChecker constraintAnnotation) {
    Class<? extends Enum<?>> enumClass = constraintAnnotation.enumClazz();

    @SuppressWarnings("rawtypes")
    Enum[] enumValArr = enumClass.getEnumConstants();

    values = Stream.of(enumValArr).map(e -> e.toString().toUpperCase())
        .collect(Collectors.toList());
  }

}
