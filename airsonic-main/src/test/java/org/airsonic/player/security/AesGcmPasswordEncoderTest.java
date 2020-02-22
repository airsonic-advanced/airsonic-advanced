package org.airsonic.player.security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class AesGcmPasswordEncoderTest {

    private static final String testPass = "testpass";
    private static final String testSalt = "2D9AAC35B41C0DB14C8170BE555DC0C9";

    AesGcmPasswordEncoder encoder = new AesGcmPasswordEncoder(testPass, testSalt);

    @Test
    public void testEncryptionAndDecryption() throws Exception {
        String plaintext = "randomstring";
        assertThat(encoder.decode(encoder.encode(plaintext))).isEqualTo(plaintext);
    }

    @Test
    public void testEncryptionAndDecryption_anotherEncoder_samePassword() throws Exception {
        String plaintext = "randomstring";
        AesGcmPasswordEncoder encoder2 = new AesGcmPasswordEncoder(testPass, testSalt);
        assertThat(encoder.decode(encoder2.encode(plaintext))).isEqualTo(plaintext);
    }

    @Test
    public void testEncryptionAndDecryption_anotherEncoder_differentPassword() throws Exception {
        String plaintext = "randomstring";
        AesGcmPasswordEncoder encoder2 = new AesGcmPasswordEncoder(testPass + "a", testSalt);
        assertThatExceptionOfType(Exception.class).isThrownBy(() -> encoder.decode(encoder2.encode(plaintext)));
        AesGcmPasswordEncoder encoder3 = new AesGcmPasswordEncoder(testPass, testSalt + "55");
        assertThatExceptionOfType(Exception.class).isThrownBy(() -> encoder.decode(encoder3.encode(plaintext)));
    }

    @Test
    public void testMultipleEncryptionsYieldsDifferentResults() throws Exception {
        String plaintext = "randomstring";
        assertThat(encoder.encode(plaintext)).isNotEqualTo(encoder.encode(plaintext));
    }

    @Test
    public void testMatch() throws Exception {
        String plaintext = "randomstring";
        String encoded1 = encoder.encode(plaintext);
        String encoded2 = encoder.encode(plaintext);
        assertThat(encoder.matches(plaintext, encoded1)).isTrue();
        assertThat(encoder.matches(plaintext, encoded2)).isTrue();
        assertThat(encoder.matches(plaintext + "a", encoded2)).isFalse();
    }

    @Test
    public void testNonMatch_differentEncoder_differentPassword() throws Exception {
        String plaintext = "randomstring";
        AesGcmPasswordEncoder encoder2 = new AesGcmPasswordEncoder(testPass + "a", testSalt);
        assertThat(encoder2.matches(plaintext, encoder.encode(plaintext))).isFalse();
    }
}
