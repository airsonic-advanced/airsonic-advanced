package org.airsonic.player.validator;

import org.airsonic.player.command.CredentialsManagementCommand.AppCredSettings;
import org.airsonic.player.command.CredentialsManagementCommand.CredentialsCommand;
import org.airsonic.player.controller.CredentialsManagementController;
import org.airsonic.player.security.GlobalSecurityConfig;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;

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
    @Constraint(validatedBy = CredTypeValidValidator.class)
    @Documented
    public @interface CredTypeValid {
        String message() default "{credentialsettings.invalidtype}";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class CredTypeValidValidator implements ConstraintValidator<CredTypeValid, String> {
        @Override
        public boolean isValid(String field, ConstraintValidatorContext context) {
            if (field == null) {
                return true;
            }

            return GlobalSecurityConfig.ENCODERS.keySet().contains(field);
        }
    }

    @Target({ ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = CredTypeForLocationValidValidator.class)
    @Documented
    public @interface CredTypeForLocationValid {
        String message() default "{credentialsettings.invalidtypeforlocation}";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class CredTypeForLocationValidValidator implements ConstraintValidator<CredTypeForLocationValid, CredentialsCommand> {
        @Override
        public boolean isValid(CredentialsCommand creds, ConstraintValidatorContext context) {
            if (creds == null) {
                return true;
            }

            AppCredSettings appSettings = CredentialsManagementController.APPS_CREDS_SETTINGS.get(creds.getLocation());
            if (appSettings == null) {
                return true;
            }

            boolean valid = true;
            if (!appSettings.getNonDecodableEncodersAllowed()) {
                valid = !GlobalSecurityConfig.NONLEGACY_NONDECODABLE_ENCODERS.contains(creds.getType());
            }

            return valid;
        }
    }

    public interface CredentialCreateChecks {
    }

    public interface CredentialUpdateChecks {
    }

    @Target({ ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = EncoderTypeValidValidator.class)
    @Documented
    public @interface EncoderTypeValid {
        String message() default "{credentialsettings.invalidtypeforlocation}";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};

        boolean decodable();
    }

    public static class EncoderTypeValidValidator implements ConstraintValidator<EncoderTypeValid, String> {
        private boolean decodable;

        @Override
        public void initialize(EncoderTypeValid constraintAnnotation) {
            this.decodable = constraintAnnotation.decodable();
        }

        @Override
        public boolean isValid(String type, ConstraintValidatorContext context) {
            if (type == null) {
                return true;
            }

            if (decodable) {
                return GlobalSecurityConfig.NONLEGACY_DECODABLE_ENCODERS.contains(type);
            } else {
                return GlobalSecurityConfig.NONLEGACY_NONDECODABLE_ENCODERS.contains(type);
            }
        }
    }
}
