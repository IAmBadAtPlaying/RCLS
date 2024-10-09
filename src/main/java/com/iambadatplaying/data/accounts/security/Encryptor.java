package com.iambadatplaying.data.accounts.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Optional;

public class Encryptor {
    private transient final String password;

    public Encryptor(String password) {
        this.password = password;
    }

    public Optional<EncryptResult> encrypt(String data) {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(data.getBytes());

            String encodedString = Base64.getEncoder().encodeToString(encrypted);
            String encodedSalt = Base64.getEncoder().encodeToString(salt);

            return Optional.of(new EncryptResult(encodedString, encodedSalt));
        } catch (Exception e) {
        }
        return Optional.empty();
    }

    public class EncryptResult {
        private final String encryptedData;
        private final String dataSalt;

        public EncryptResult(String encryptedData, String dataSalt) {
            this.encryptedData = encryptedData;
            this.dataSalt = dataSalt;
        }

        public String getEncryptedData() {
            return encryptedData;
        }

        public String getDataSalt() {
            return dataSalt;
        }
    }
}
