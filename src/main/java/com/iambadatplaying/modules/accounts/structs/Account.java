package com.iambadatplaying.modules.accounts.structs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import com.iambadatplaying.modules.accounts.structs.security.Decryptor;
import com.iambadatplaying.modules.accounts.structs.security.Encryptor;

import java.util.Optional;
import java.util.UUID;

public class Account implements Lockable {
    // We should never read or write these fields in the serialized form as they contain sensitive information
    private transient String loginPassword;

    private transient boolean unlocked = false;

    @SerializedName("versionId")
    private int versionId = 1;

    @SerializedName("uuid")
    private String uuid;

    @SerializedName("loginName")
    private String loginName;

    @SerializedName("password")
    private String encryptedPassword;

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

        JsonObject jsonObject = json.getAsJsonObject();
        jsonObject.addProperty("loginName", loginName);
        jsonObject.addProperty("loginPassword", loginPassword);

        return jsonObject;
    }

    public boolean unlock(Decryptor decryptor) {
        if (unlocked) {
            return true;
        }

        if (decryptor == null) {
            return false;
        }

        Optional<String> optDecryptedPassword = decryptor.decrypt(encryptedPassword, passwordSalt);

        if (!optDecryptedPassword.isPresent()) {
            return false;
        }

        loginPassword = optDecryptedPassword.get();

        onUnlock();

        return true;
    }

    private void onUnlock() {
        passwordSalt = null;
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

        Optional<Encryptor.EncryptResult> optEncryptedPassword = encryptor.encrypt(loginPassword);

        if (!optEncryptedPassword.isPresent()) {
            return false;
        }

        Encryptor.EncryptResult encryptedPassword = optEncryptedPassword.get();

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

    @Override
    public boolean isUnlocked() {
        return unlocked;
    }

    public static void main(String[] args) {

        AccountList accountList = new AccountList(
                "TestMap",
                "password1234"
        );

        Account account = new Account(
                "test",
                "password"
        );

        System.out.println(account);

        String uuid = account.getUuid();

        System.out.println("Account added ? " + accountList.addAccount(account));

        accountList.lock();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String serialized = gson.toJson(accountList);
        System.out.println(serialized);

        AccountList deserialized = gson.fromJson(serialized, AccountList.class);
        deserialized.unlock("password1234");
        Account desAcc = deserialized.getAccount(uuid);
        System.out.println(desAcc.loginName);
        System.out.println(desAcc.loginPassword);
    }
}

