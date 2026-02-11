package org.airsonic.player.validator;

import org.airsonic.player.command.CredentialsManagementCommand.CredentialsCommand;
import org.airsonic.player.security.GlobalSecurityConfig;
import org.apache.commons.lang3.StringUtils;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class CredentialsManagementValidators {
    @Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = ConsistentPasswordConfirmationValidator.class)
    @Documented
    public @interface ConsistentPasswordConfirmation {
        String message() default "{usersettings.wrongpassword}";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class ConsistentPasswordConfirmationValidator implements ConstraintValidator<ConsistentPasswordConfirmation, CredentialsCommand> {
        @Override
        public boolean isValid(CredentialsCommand creds, ConstraintValidatorContext context) {
            if (creds == null) {
                return true;
            }

            return StringUtils.equals(creds.getCredential(), creds.getConfirmCredential());
        }
    }

    @Target({ ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = EncoderValidValidator.class)
    @Documented
    public @interface EncoderValid {
        String message() default "{credentialsettings.invalidencoder}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        String type() default "all";
    }

    public static class EncoderValidValidator implements ConstraintValidator<EncoderValid, String> {
        private String type;

        @Override
        public void initialize(EncoderValid constraintAnnotation) {
            this.type = constraintAnnotation.type();
        }

        @Override
        public boolean isValid(String field, ConstraintValidatorContext context) {
            if (field == null) {
                return true;
            }

            if (StringUtils.equals("nonlegacydecodable", type)) {
                return GlobalSecurityConfig.NONLEGACY_DECODABLE_ENCODERS.contains(field);
            } else if (StringUtils.equals("nonlegacynondecodable", type)) {
                return GlobalSecurityConfig.NONLEGACY_NONDECODABLE_ENCODERS.contains(field);
            }

            return GlobalSecurityConfig.ENCODERS.keySet().contains(field);
        }
    }

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = CredEncoderForAppValidValidator.class)
    @Documented
    public @interface CredEncoderForAppValid {
        String message() default "{credentialsettings.invalidencoder}";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class CredEncoderForAppValidValidator implements ConstraintValidator<CredEncoderForAppValid, CredentialsCommand> {
        @Override
        public boolean isValid(CredentialsCommand creds, ConstraintValidatorContext context) {
            if (creds == null || creds.getApp() == null) {
                return true;
            }

            boolean valid = true;
            if (!creds.getApp().getNonDecodableEncodersAllowed()) {
                valid = !GlobalSecurityConfig.NONLEGACY_NONDECODABLE_ENCODERS.contains(creds.getEncoder());
            }

            return valid;
        }
    }

    public interface CredentialCreateChecks {
    }

    public interface CredentialUpdateChecks {
    }
}
