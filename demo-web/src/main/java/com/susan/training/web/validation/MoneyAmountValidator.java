package com.susan.training.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

/**
 * 使用 BigDecimal 校验金额范围，避免 float/double 比较误差。
 */
public class MoneyAmountValidator implements ConstraintValidator<MoneyAmount, BigDecimal> {
    private BigDecimal minimum;
    private BigDecimal maximum;

    @Override
    public void initialize(MoneyAmount annotation) {
        minimum = new BigDecimal(annotation.min());
        maximum = new BigDecimal(annotation.max());
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        // null 是否允许由 @NotNull 决定，自定义校验器不重复承担空值规则。
        if (value == null) {
            return true;
        }
        return value.compareTo(minimum) >= 0 && value.compareTo(maximum) <= 0;
    }
}
