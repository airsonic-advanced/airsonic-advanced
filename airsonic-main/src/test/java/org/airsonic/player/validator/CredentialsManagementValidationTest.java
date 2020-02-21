package org.airsonic.player.validator;

import org.airsonic.player.command.CredentialsManagementCommand;
import org.airsonic.player.command.CredentialsManagementCommand.CredentialsCommand;
import org.airsonic.player.domain.UserCredential.App;
import org.airsonic.player.validator.CredentialsManagementValidators.CredentialCreateChecks;
import org.airsonic.player.validator.CredentialsManagementValidators.CredentialUpdateChecks;
import org.junit.Test;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class CredentialsManagementValidationTest {
    private static final Validator v = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void credCreation_correct() {
        CredentialsCommand cc = new CredentialsCommand("username", "bcrypt", App.AIRSONIC, null, null, null, null, null);
        cc.setCredential("c");
        cc.setConfirmCredential("c");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isEmpty();

        cc.setEncoder("legacyhex");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isEmpty();

        cc.setEncoder("noop");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isEmpty();

        cc.setApp(App.LASTFM);
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isEmpty();
    }

    @Test
    public void credCreation_emptyUser() {
        CredentialsCommand cc = new CredentialsCommand(null, "bcrypt", App.AIRSONIC, null, null, null, null, null);
        cc.setCredential("c");
        cc.setConfirmCredential("c");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();

        cc.setUsername("  ");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();
    }

    @Test
    public void credCreation_mismatchedPasswords() {
        CredentialsCommand cc = new CredentialsCommand("username", "bcrypt", App.AIRSONIC, null, null, null, null, null);
        cc.setCredential("c");
        cc.setConfirmCredential("p");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();
    }

    @Test
    public void credCreation_emptyPassword() {
        CredentialsCommand cc = new CredentialsCommand("username", "bcrypt", App.AIRSONIC, null, null, null, null, null);
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();

        cc.setCredential(" ");
        cc.setConfirmCredential(" ");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();
    }

    @Test
    public void credCreation_unknownApp() {
        CredentialsCommand cc = new CredentialsCommand("username", "bcrypt", null, null, null, null, null, null);
        cc.setCredential("c");
        cc.setConfirmCredential("c");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();
    }

    @Test
    public void credCreation_unknownEncoder() {
        CredentialsCommand cc = new CredentialsCommand("username", "bcrypt2", App.AIRSONIC, null, null, null, null, null);
        cc.setCredential("c");
        cc.setConfirmCredential("c");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();

        cc.setEncoder(null);
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();
    }

    @Test
    public void credCreation_wrongEncoderForApp() {
        CredentialsCommand cc = new CredentialsCommand("username", "bcrypt", App.LASTFM, null, null, null, null, null);
        cc.setCredential("c");
        cc.setConfirmCredential("c");
        assertThat(v.validate(cc, Default.class, CredentialCreateChecks.class)).isNotEmpty();
    }

    @Test
    public void credUpdate_correct() {
        CredentialsCommand cc = new CredentialsCommand(null, "bcrypt", null, null, null, null, null, "hash");
        assertThat(v.validate(cc, Default.class, CredentialUpdateChecks.class)).isEmpty();
    }

    @Test
    public void credUpdate_missingHash() {
        CredentialsCommand cc = new CredentialsCommand(null, "bcrypt", null, null, null, null, null, null);
        assertThat(v.validate(cc, Default.class, CredentialUpdateChecks.class)).isNotEmpty();
    }

    @Test
    public void credUpdate_unknownEncoder() {
        CredentialsCommand cc = new CredentialsCommand(null, "bcrypt2", null, null, null, null, null, "hash");
        assertThat(v.validate(cc, Default.class, CredentialUpdateChecks.class)).isNotEmpty();

        cc.setEncoder(null);
        assertThat(v.validate(cc, Default.class, CredentialUpdateChecks.class)).isNotEmpty();
    }

    @Test
    public void credUpdate_nested_correct() {
        CredentialsCommand cc = new CredentialsCommand(null, "bcrypt", null, null, null, null, null, "hash");
        assertThat(v.validate(new CredentialsManagementCommand(Collections.singletonList(cc)), Default.class, CredentialUpdateChecks.class)).isEmpty();
    }

    @Test
    public void credUpdate_nested_incorrect() {
        CredentialsCommand cc = new CredentialsCommand(null, "bcrypt2", null, null, null, null, null, "hash");
        assertThat(v.validate(new CredentialsManagementCommand(Collections.singletonList(cc)), Default.class, CredentialUpdateChecks.class)).isNotEmpty();

        cc.setEncoder("bcrypt");
        cc.setHash(null);
        assertThat(v.validate(new CredentialsManagementCommand(Collections.singletonList(cc)), Default.class, CredentialUpdateChecks.class)).isNotEmpty();
    }
}
