package com.iambadatplaying.data.accounts.structs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import com.iambadatplaying.data.accounts.security.Decryptor;
import com.iambadatplaying.data.accounts.security.Encryptor;

import java.util.Optional;
import java.util.UUID;

public class Account {
    // We should never read or write these fields in the serialized form as they contain sensitive information
    private transient String loginName;
    private transient String loginPassword;

    private transient boolean unlocked = false;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("username")
    private String encryptedUsername;

    @SerializedName("password")
    private String encryptedPassword;

    @SerializedName("usernameSalt")
    private String usernameSalt;

    @SerializedName("passwordSalt")
    private String passwordSalt;

    public Account(
            String loginName,
            String loginPassword
    ) {
        this.loginName = loginName;
        this.loginPassword = loginPassword;

        this.uuid = UUID.randomUUID().toString();
        this.unlocked = true;
    }

    public String getUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }
        return uuid;
    }

    public JsonElement serializeSensitive() {
        JsonElement json = new Gson().toJsonTree(this, Account.class);
        if (!unlocked) {
            return json;
        }

        return null;
    }

    public boolean unlock(Decryptor decryptor) {
        if (unlocked) {
            return true;
        }

        if (decryptor == null) {
            return false;
        }

        Optional<String> optDecryptedUsername = decryptor.decrypt(encryptedUsername, usernameSalt);
        Optional<String> optDecryptedPassword = decryptor.decrypt(encryptedPassword, passwordSalt);

        if (!optDecryptedUsername.isPresent() || !optDecryptedPassword.isPresent()) {
            return false;
        }

        loginName = optDecryptedUsername.get();
        loginPassword = optDecryptedPassword.get();

        onUnlock();

        return true;
    }

    private void onUnlock() {
        usernameSalt = null;
        passwordSalt = null;
        encryptedUsername = null;
        encryptedPassword = null;

        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }

        unlocked = true;
    }

    public boolean lock(Encryptor encryptor) {
        if (!unlocked) {
            return true;
        }

        if (encryptor == null) {
            return false;
        }

        Optional<Encryptor.EncryptResult> optEncryptedUsername = encryptor.encrypt(loginName);
        Optional<Encryptor.EncryptResult> optEncryptedPassword = encryptor.encrypt(loginPassword);

        if (!optEncryptedUsername.isPresent() || !optEncryptedPassword.isPresent()) {
            return false;
        }

        Encryptor.EncryptResult encryptedUsername = optEncryptedUsername.get();
        Encryptor.EncryptResult encryptedPassword = optEncryptedPassword.get();

        this.usernameSalt = encryptedUsername.getDataSalt();
        this.encryptedUsername = encryptedUsername.getEncryptedData();

        this.passwordSalt = encryptedPassword.getDataSalt();
        this.encryptedPassword = encryptedPassword.getEncryptedData();

        onLock();

        return true;
    }

    private void onLock() {
        loginName = null;
        loginPassword = null;
        unlocked = false;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public static void main(String[] args) {
        Account account = new Account(
                "test",
                "password"
        );
        account.loginName = "test";
        account.loginPassword = "testPassword!";
        System.out.println(account.loginName);
        System.out.println(account.loginPassword);

        System.out.println(account.lock(new Encryptor("password")));

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        String serialized = gson.toJson(account, Account.class);
        System.out.println(serialized);


        Account deserialized = gson.fromJson(serialized, Account.class);


        System.out.println(deserialized.unlock(new Decryptor("password")));

        System.out.println(deserialized.loginName);
        System.out.println(deserialized.loginPassword);
    }
}
