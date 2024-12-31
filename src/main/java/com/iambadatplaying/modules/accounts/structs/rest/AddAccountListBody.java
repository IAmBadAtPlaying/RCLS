package com.iambadatplaying.modules.accounts.structs.rest;

public class AddAccountListBody implements Validatable {
    private String name;
    private String masterPassword;

    public String getName() {
        return name;
    }

    public String getMasterPassword() {
        return masterPassword;
    }

    @Override
    public boolean isValid() {
        return name != null && masterPassword != null;
    }
}
