package com.iambadatplaying.modules.accounts;

import com.iambadatplaying.modules.BasicConfig;

import java.nio.file.Path;

public class AccountConfiguration implements BasicConfig {

    private long   version = 1L;
    private String accountListFileName = "accountLists.json";

    @Override
    public long getVersion() {
        return version;
    }

    public String getAccountListFileName() {
        return accountListFileName;
    }
}
