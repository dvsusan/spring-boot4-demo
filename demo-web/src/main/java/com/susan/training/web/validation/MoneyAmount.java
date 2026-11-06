package com.susan.training.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 限制订单金额必须落在教学业务允许的范围内。
 */
@Documented
@Constraint(validatedBy = MoneyAmountValidator.class)
@Target({FIELD, PARAMETER, ANNOTATION_TYPE})
@Retention(RUNTIME)
public @interface MoneyAmount {
    String message() default "{order.amount.range}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * 最小金额使用字符串声明，避免注解属性的 double 精度问题。
     */
    String min() default "0.01";

    String max() default "1000000.00";
}
