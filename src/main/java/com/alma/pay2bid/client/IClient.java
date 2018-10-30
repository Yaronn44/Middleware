package com.alma.pay2bid.client;

import com.alma.pay2bid.bean.AuctionBean;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.UUID;

/**
 * The Interface of a client in our model
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface IClient extends Remote, Serializable {
    /**
     * @param auction
     * @throws RemoteException
     */
    void newAuction(UUID auctionId, AuctionBean auction) throws RemoteException;

    /**
     * @param auction
     * @throws RemoteException
     */
    void submit(AuctionBean auction) throws RemoteException;

    /**
     * @param buyer
     * @throws RemoteException
     */
    void bidSold(UUID auctionId, IClient buyer) throws RemoteException;

    /**
     * @param auctionId
     * @param price
     * @throws RemoteException
     */
    void newPrice(UUID auctionId, int price) throws RemoteException;

    /**
     * @param auctionId
     * @param price
     * @throws RemoteException
     */
    void updatePrice(UUID auctionId, int price) throws RemoteException;

    /**
     * @return
     * @throws RemoteException
     */
    String getName() throws RemoteException;

    void addAuctionWin(AuctionBean auction) throws RemoteException;

    String getIdentifier() throws RemoteException;

    void setName(String name) throws RemoteException;

    ClientState getState(UUID auctionId) throws RemoteException;

    void setState(UUID auctionId, ClientState newState) throws RemoteException;

    void setIsSeller(UUID auctionId, boolean v) throws RemoteException;
}
