package com.iambadatplaying.modules.accounts.structs.rest;

public class UpdateAccountListPasswordBody implements Validatable {
    private String oldPassword;
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public boolean isValid() {
        return oldPassword != null && newPassword != null;
    }
}
