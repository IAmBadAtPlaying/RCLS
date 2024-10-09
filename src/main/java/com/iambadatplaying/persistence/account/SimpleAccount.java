package com.iambadatplaying.persistence.account;

import java.util.Optional;

public class SimpleAccount {

    protected String id;
    protected String gameName;
    protected String gameTag;

    protected boolean locked = true;

    protected String loginName;
    protected String password;


    protected SimpleAccount() {}
}
