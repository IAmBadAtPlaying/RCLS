package com.iambadatplaying.modules.accounts.structs.rest;

public class UpdateAccountListBody implements Validatable {
    private String name;

    public String getName() {
        return name;
    }

    @Override
    public boolean isValid() {
        return name != null;
    }
}
