package com.alma.pay2bid.server;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.UUID;

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
    void raiseBid(UUID auctionId, IClient client, int newBid) throws RemoteException;

    /**
     * @param client
     * @throws RemoteException
     * @throws InterruptedException
     */
    void timeElapsed(UUID auctionId, IClient client) throws RemoteException, InterruptedException;

    /**
     * Notifies the server that a client's timer has reach zero.
     * @param client
     * @throws RemoteException
     * @throws InterruptedException
     */
    void clientDisconnection(IClient client) throws RemoteException, InterruptedException;

    /**
     * Notifies the server that a clients have crashed
     * @param clientsCrashed
     * @throws RemoteException
     */
     void clientsCrashed(List<IClient> clientsCrashed) throws RemoteException;

    IClient getWinner(UUID auctionId) throws RemoteException;

    AuctionBean getAuction(UUID auctionId) throws RemoteException;

}
