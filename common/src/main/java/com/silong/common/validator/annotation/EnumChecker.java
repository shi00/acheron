package com.silong.common.validator.annotation;

import com.silong.common.validator.impl.EnumValidatorImpl;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * 校验枚举类型声明
 *
 * @author louis sin
 * @version 1.0
 * @since 20161124
 */
@Documented
@Constraint(validatedBy = EnumValidatorImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EnumChecker {

  /**
   * 枚举类型
   *
   * @return 枚举class
   */
  Class<? extends Enum<?>> enumClazz();

  /**
   * 错误描述
   *
   * @return 错误描述
   */
  String message() default "Value is not valid";

  /**
   * 优先级
   *
   * @return 优先级
   */
  Class<?>[] groups() default {};

  /**
   * 负载
   *
   * @return 负载
   */
  Class<? extends Payload>[] payload() default {};
}
