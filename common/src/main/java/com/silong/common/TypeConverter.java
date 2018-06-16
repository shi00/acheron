package com.silong.common;

import java.util.Objects;
import java.util.function.Function;

/**
 * 类型转换器，可以自定义转换方法。
 *
 * @param <T> from type
 * @param <U> to type
 * @author louis sin
 * @version 1.0
 * @since 20170711
 */
public interface TypeConverter<T, U> {

  /**
   * 构造类型转换器
   *
   * @param fromType from类型
   * @param toType to类型
   * @param from 转换方法，from类型转换为to类型
   * @param to 转换方法，to类型转换为from类型
   * @return 类型转换器
   * @throws NullPointerException 入参为null时抛出
   */
  static <T, U> TypeConverter<T, U> of(Class<T> fromType, Class<U> toType,
      Function<? super T, ? extends U> from,
      Function<? super U, ? extends T> to) {
    Objects.requireNonNull(fromType, "fromType must not be null.");
    Objects.requireNonNull(toType, "toType must not be null.");
    Objects.requireNonNull(from, "from must not be null.");
    Objects.requireNonNull(to, "to must not be null.");

    return new TypeConverter<T, U>() {

      @Override
      public U from(T t) {
        return from.apply(t);
      }

      @Override
      public T to(U u) {
        return to.apply(u);
      }

      @Override
      public Class<T> fromType() {
        return fromType;
      }

      @Override
      public Class<U> toType() {
        return toType;
      }
    };
  }

  /**
   * 类型转换
   *
   * @param t source
   * @return destination
   */
  U from(T t);

  /**
   * 类型转换
   *
   * @param u source
   * @return destination
   */
  T to(U u);

  /**
   * from类型Class
   *
   * @return Class
   */
  Class<T> fromType();

  /**
   * to类型Class
   *
   * @return Class
   */
  Class<U> toType();

}
