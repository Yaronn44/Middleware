package com.alma.pay2bid.server;

import java.rmi.Remote;
import java.rmi.RemoteException;

import com.alma.pay2bid.bean.AuctionBean;
import com.alma.pay2bid.client.IClient;

/**
 * The interface that specifies the actions of a server in our model
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public interface IServer extends Remote {
    /**
     * @param auction
     * @throws RemoteException
     */
    void placeAuction(AuctionBean auction) throws RemoteException;

    /**
     * @param client
     * @throws RemoteException
     * @throws InterruptedException
     */
    void register(IClient client) throws RemoteException, InterruptedException;

    void disconnect(IClient client) throws RemoteException, InterruptedException;

    /**
     * @param client
     * @param newBid
     * @throws RemoteException
     */
    void raiseBid(IClient client, int newBid) throws RemoteException;

    /**
     * @param client
     * @throws RemoteException
     * @throws InterruptedException
     */
    void timeElapsed(IClient client) throws RemoteException, InterruptedException;

    IClient getWinner() throws RemoteException;
    
    AuctionBean getCurrentAuction() throws RemoteException;
}
