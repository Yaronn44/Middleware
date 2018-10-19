package com.alma.pay2bid.bean;

import java.util.UUID;

/**
 * AuctionBean represent an auction, ie an item sold by a client
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public class AuctionBean implements IBean {
    private UUID uuid;
    private int price;
    private String name;
    private String description;
    private String seller;

    public AuctionBean(int price, String name, String description, String seller) {
    	if (price > 0) this.price = price;
    	else this.price = 0;
        this.name = name;
        this.description = description;
        this.seller = seller;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

	public String getSeller() {
		return seller;
	}

	public void setVendeur(String seller) {
		this.seller = seller;
	}
    
    public String toString(){
        return "uuid : " + uuid + " \n name : " + name + " \n";
    }

}
