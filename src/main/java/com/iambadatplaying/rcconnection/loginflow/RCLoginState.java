package com.iambadatplaying.rcconnection.loginflow;

public enum RCLoginState {
    NOT_LOGGED_IN,
    LOGGED_IN,
    LOGGED_OUT,
    LOGIN_ERROR,
    LOGOUT_ERROR,
    MULTIFACTOR_AUTHENTICATION_REQUIRED;
}
