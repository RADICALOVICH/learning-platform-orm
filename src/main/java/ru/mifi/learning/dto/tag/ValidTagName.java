package ru.mifi.learning.dto.tag;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import ru.mifi.learning.domain.entity.Tag;
import ru.mifi.learning.exception.BusinessRuleException;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTagName.Validator.class)
public @interface ValidTagName {
    String message() default "Проверьте имя тега";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidTagName, String> {
        @Override
        public boolean isValid(String name, ConstraintValidatorContext context) {
            try {
                Tag.normalizeName(name);
                return true;
            } catch (BusinessRuleException ex) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(ex.getMessage()).addConstraintViolation();
                return false;
            }
        }
    }
}
