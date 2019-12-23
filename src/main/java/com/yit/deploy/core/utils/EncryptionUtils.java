package com.yit.deploy.core.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.File;
import java.util.Base64;

/**
 * Created by nick on 07/09/2017.
 */
public class EncryptionUtils {
    private static final String ENCRYPTED_TEXT_PREFIX = "DES:";
    private static final int ENCRYPTED_SPLIT_WIDTH = 80;
    private final String secretKey;

    public static boolean isEncrypted(String text) {
        return text != null && text.startsWith(ENCRYPTED_TEXT_PREFIX);
    }

    public static boolean isEncrypted(byte[] bytes) {
        if (bytes == null) return false;
        byte[] mark = ENCRYPTED_TEXT_PREFIX.getBytes(Utils.DefaultCharset);
        if (mark.length > bytes.length) return false;
        for (int i = 0; i < mark.length; i++) {
            if (mark[i] != bytes[i]) return false;
        }
        return true;
    }

    public EncryptionUtils(String envtype) {
        String passpath = System.getProperty("user.home") + "/.ansible-vault-" + envtype + ".pass";
        secretKey = IO.readToString(new File(passpath)).trim();
    }

    public String encryptFromText(String text) {
        return encrypt(text.getBytes(Utils.DefaultCharset));
    }

    public String decryptToText(String text) {
        return new String(decrypt(text), Utils.DefaultCharset);
    }

    public String encrypt(byte[] bytes) {
        bytes = encryptOrDecrypt(Cipher.ENCRYPT_MODE, bytes);
        String encrypted = Base64.getEncoder().encodeToString(bytes);
        String text = ENCRYPTED_TEXT_PREFIX + encrypted;
        if (text.length() < ENCRYPTED_SPLIT_WIDTH) return text;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i += ENCRYPTED_SPLIT_WIDTH) {
            if (i > 0) sb.append('\n');
            sb.append(text.substring(i, Math.min(i + ENCRYPTED_SPLIT_WIDTH, text.length())));
        }
        return sb.toString();
    }

    public byte[] decrypt(String text) {
        try {
            text = text.replace("\n", "");
            if (!text.startsWith(ENCRYPTED_TEXT_PREFIX)) {
                throw new IllegalArgumentException("text is not a valid encrypted string");
            }
            String encrypted = text.substring(ENCRYPTED_TEXT_PREFIX.length());
            byte[] bytes = Base64.getDecoder().decode(encrypted);
            return encryptOrDecrypt(Cipher.DECRYPT_MODE, bytes);
        } catch (Exception e) {
            throw new IllegalArgumentException("faild to decrypt " + text, e);
        }
    }

    private byte[] encryptOrDecrypt(int mode, byte[] input) {
        try {
            DESKeySpec dks = new DESKeySpec(secretKey.getBytes(Utils.DefaultCharset));
            SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
            SecretKey desKey = skf.generateSecret(dks);
            Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
            cipher.init(mode, desKey);
            return cipher.doFinal(input);
        } catch (Exception e) {
            throw new RuntimeException("encrypt/decrypt failed: " + e.getMessage(), e);
        }
    }
}
