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
    private String login; // must be unique
    private String password; // hash
    private String name; // what other clients see

    public ClientBean(UUID uuid, String login, String password, String name) {
        this.uuid = uuid;
        this.login = login;
        this.password = password;
        this.name = name;
    }

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }
}
