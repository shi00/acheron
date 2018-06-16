package com.silong.common.validator.annotation;

import com.silong.common.validator.impl.TwovarMutuallyExclusiveValidatorImpl;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * 互斥成员变量，两者不能同时为null，也不能同时有值
 *
 * @author louis sin
 * @version 1.0
 * @since 20161124
 */
@Documented
@Constraint(validatedBy = TwovarMutuallyExclusiveValidatorImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TwovarMutuallyExclusive {

  String message() default "The specified two variables cannot be null or have value at the same time.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /**
   * @return first field
   */
  String one();

  /**
   * @return second field
   */
  String other();
}
