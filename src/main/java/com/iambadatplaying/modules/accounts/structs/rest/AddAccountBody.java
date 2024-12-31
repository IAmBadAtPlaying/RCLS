package com.iambadatplaying.modules.accounts.structs.rest;

public class AddAccountBody implements Validatable {

    private String loginName;
    private String loginPassword;

    public String getLoginName() {
        return loginName;
    }

    public String getLoginPassword() {
        return loginPassword;
    }

    @Override
    public boolean isValid() {
        return loginName != null && loginPassword != null;
    }
}
