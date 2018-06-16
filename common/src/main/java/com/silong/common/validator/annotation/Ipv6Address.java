package com.silong.common.validator.annotation;

import com.silong.common.validator.impl.Ipv6ValidatorImpl;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * 校验ipv4
 *
 * @author louis sin
 * @version 1.0
 * @since 20170529
 */
@Documented
@Constraint(validatedBy = Ipv6ValidatorImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Ipv6Address {

  String message() default "Please specify a valid ipv6 address.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
