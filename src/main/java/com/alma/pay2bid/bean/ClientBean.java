package com.alma.pay2bid.bean;

import java.util.UUID;

/**
 * ClientBean represent the identity of a client
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public class ClientBean implements IBean {
    private UUID uuid;
    private String login; // Must be unique (but is not actually)
    private String password; // Hash
    private String identifier; // Unique identifier to find a client

    public ClientBean(UUID uuid, String login, String password, String identifier) {
        this.uuid = uuid;
        this.login = login;
        this.password = password;
        this.identifier = identifier;
    }

    @Override
    public UUID  getUuid(){ return this.uuid; }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
