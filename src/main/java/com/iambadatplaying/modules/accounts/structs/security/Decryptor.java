package com.iambadatplaying.modules.accounts.structs.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Optional;

public class Decryptor {
    private transient final String password;

    public Decryptor(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public Optional<String> decrypt(String encryptedData, String dataSalt) {
        if (encryptedData == null || dataSalt == null) return Optional.empty();
        try {
            byte[] salt = Base64.getDecoder().decode(dataSalt);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(decodedBytes);

            String decryptedString = new String(decrypted);

            return Optional.of(decryptedString);
        } catch (Exception e) {}
        return Optional.empty();
    }
}
