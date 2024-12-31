package com.iambadatplaying.modules.accounts.structs.rest;

public class UnlockAccountListBody implements Validatable {
    private String password;

    public String getPassword() {
        return password;
    }

    @Override
    public boolean isValid() {
        return password != null;
    }
}
