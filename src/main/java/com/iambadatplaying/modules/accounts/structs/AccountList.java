package com.iambadatplaying.modules.accounts.structs;

import com.google.gson.annotations.SerializedName;
import com.iambadatplaying.modules.accounts.structs.security.Decryptor;
import com.iambadatplaying.modules.accounts.structs.security.Encryptor;

import java.security.SecureRandom;
import java.util.*;

public class AccountList implements Lockable {

    //We will just have a map internally and not expose it
    //Doing this we allow restricted access to write operations that might otherwise override locked accounts
    @SerializedName("accounts")
    private final     Map<String, Account> accounts = Collections.synchronizedMap(new HashMap<>());
    private transient boolean              unlocked = false;

    private transient String masterPassword;
    private String encryptedSecret;

    private String salt;
    private String secretToMatch;

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    public AccountList(String name, String masterPassword) {
        this.unlocked = true;
        this.name = name;
        this.masterPassword = masterPassword;
        this.id = UUID.randomUUID().toString();
    }

    public boolean changeMasterPassword(String newPassword) {
        if (!unlocked) return false;

        masterPassword = newPassword;
        return true;
    }

    public boolean unlock(String password) {
        if (unlocked) return true;
        synchronized (this) {
            Decryptor decryptor = new Decryptor(password);

            //Check if List can be decrypted by decrypting the secret and expecting it to match the secretToMatch
            Optional<String> decryptedSecret = decryptor.decrypt(encryptedSecret, salt);
            if (!decryptedSecret.isPresent()) {
                return false;
            }

            if (!decryptedSecret.get().equals(secretToMatch)) {
                return false;
            }

            masterPassword = password;
            unlockAccounts(decryptor);
            onUnlock();

            return true;
        }
    }

    private void onUnlock() {
        salt = null;
        secretToMatch = null;
        encryptedSecret = null;

        unlocked = true;
    }

    private void unlockAccounts(Decryptor decryptor) {
        for (Account account : accounts.values()) {
            account.unlock(decryptor);
        }
    }

    public synchronized boolean lock() {
        if (!unlocked) return true;
        synchronized (this) {
            Encryptor encryptor = new Encryptor(masterPassword);

            this.secretToMatch = generateSecureRandomString(32);

            Optional<Encryptor.EncryptResult> optEncrptData = encryptor.encrypt(secretToMatch);
            if (!optEncrptData.isPresent()) {
                return false;
            }

            Encryptor.EncryptResult encryptData = optEncrptData.get();
            salt = encryptData.getDataSalt();
            encryptedSecret = encryptData.getEncryptedData();

            lockAccounts(encryptor);
            onLock();

            return true;
        }
    }

    private void lockAccounts(Encryptor encryptor) {
        for (Account account : accounts.values()) {
            account.lock(encryptor);
        }
    }

    private void onLock() {
        masterPassword = null;
        unlocked = false;
    }

    private static String generateSecureRandomString(int byteLength) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public Account getAccount(String uuid) {
        return accounts.get(uuid);
    }

    public boolean addAccount(Account account) {
        if (!unlocked) return false;
        String uuid = account.getUuid();
        if (accounts.containsKey(uuid)) return false;
        accounts.put(account.getUuid(), account);
        return true;
    }

    public boolean removeAccount(String uuid) {
        if (!unlocked) return false;
        return accounts.remove(uuid) != null;
    }

    public boolean updateAccount(String uuid, Account account) {
        if (!unlocked) return false;
        if (!accounts.containsKey(uuid)) return false;
        accounts.put(uuid, account);
        return true;
    }

    public Account getAccountByUUID(String uuid) {
        return accounts.get(uuid);
    }

    public Account[] getAccounts() {
        return accounts.values().toArray(new Account[0]);
    }

    public String getId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isUnlocked() {
        return unlocked;
    }
}
