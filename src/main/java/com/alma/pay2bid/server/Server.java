package com.alma.pay2bid.server;

import com.alma.pay2bid.bean.AuctionBean;
import com.alma.pay2bid.client.ClientState;
import com.alma.pay2bid.client.IClient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Logger;

/**
 * A Server handle the clients in our model and orchestrate the auction house
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public class Server extends UnicastRemoteObject implements IServer {

    /**
     * Daemon responsible of detecting closed connection with clients
     */
    private class ConnectionDaemon extends TimerTask {

        @Override
        public void run() {
            List<IClient> clientsToRemove = new ArrayList<IClient>();
            for(IClient client : clients) {
                try {
                    client.getName();
                } catch (RemoteException e) {
                    LOGGER.info("detected a closed connection with " + client.toString());
                    clientsToRemove.add(client);
                }
            }
            clients.removeAll(clientsToRemove);
            for (IClient client : clientsToRemove) {
              try {
                  timeElapsed(client);
              } catch (RemoteException e) {
                  e.printStackTrace();
              } catch (InterruptedException e) {
                  e.printStackTrace();
              }
            }
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());
    private static final long CHECK_CONN_DELAY = 5000;

    private boolean auctionInProgress = false;
    private AuctionBean currentAuction;
    private IClient winner;
    private int nbParticipants = 0;
    private List<IClient> clients = new ArrayList<IClient>();
    private Queue<AuctionBean> auctions = new LinkedList<AuctionBean>();
    private HashMap<IClient, Integer> bidByClient = new HashMap<IClient, Integer>();

    private static final int MIN_NUMBER_CLIENTS = 2;

    /**
     * Constructor
     * @throws RemoteException
     */
    public Server() throws RemoteException {
        super();
        Timer daemonTimer = new Timer();
        daemonTimer.schedule(new ConnectionDaemon(), 0, CHECK_CONN_DELAY);
    }

    /**
     * Launch a new auction
     */
    private void launchAuction() throws RemoteException {
        auctionInProgress = true;
        nbParticipants = clients.size() - 1; // we do not count the seller as an active participant

        currentAuction = auctions.poll();
        LOGGER.info("Auction '" + currentAuction.getName() + "' by "+currentAuction.getVendeur()+" launched !");

        // notify the client's that a new auction has begun
        for (IClient client : clients) {
            client.newAuction(currentAuction);
        }
    }

    /**
     * Submit a new auction in the auction's queue
     * @param auction
     * @throws RemoteException
     */
    @Override
    public synchronized void placeAuction(AuctionBean auction) throws RemoteException {
        // generate a new UUID for the incoming auction, then put it in the queue
        auction.setUuid(UUID.randomUUID());
        auctions.add(auction);
        LOGGER.info("Auction '" + auction.getName() + "' from "+auction.getVendeur()+"placed in queue");
        if (!auctionInProgress && (auctions.size() == 1) && (clients.size() >= MIN_NUMBER_CLIENTS)) {
            launchAuction();
        }
    }

    /**
     * Register a new client. If there is an auction in progress, the client will have to wait until its end.
     * @param client
     * @throws RemoteException
     */
    @Override
    public synchronized void register(IClient client) throws RemoteException {
        try {
            while (auctionInProgress) {
                wait();
            }
            clients.add(client);
            LOGGER.info("client " + client.toString() + " connected");
        } catch (InterruptedException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    /**
     * Disconnect a client from the server
     * @param client
     * @throws RemoteException
     * @throws InterruptedException
     */
    @Override
    public void disconnect(IClient client) throws RemoteException, InterruptedException {
        try {
			LOGGER.info("Disconnect : Client " + client.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.timeElapsed(client);
        clients.remove(client);
    }

    /**
     * Validate and flush all waiting registrations
     */
    private synchronized void validateRegistrations() {
        auctionInProgress = false;
        notifyAll();
    }

    /**
     * Raise the bid on an item
     * @param client
     * @param newBid
     * @throws RemoteException
     */
    @Override
    public synchronized void raiseBid(IClient client, int newBid) throws RemoteException {
        if((client.getState() == ClientState.WAITING) && (newBid > currentAuction.getPrice())) {
            bidByClient.put(client, newBid);
            client.setState(ClientState.RAISING);
            LOGGER.info("New bid '" + newBid + "' placed by client " + client.toString());
        }
    }

    /**
     * Notifies the server that a client's timer has reach zero.
     * @param client
     * @throws RemoteException
     * @throws InterruptedException
     */
    @Override
    public synchronized void timeElapsed(IClient client) throws RemoteException, InterruptedException {
        nbParticipants--;
        LOGGER.info("A client time elapsed : " + client.getName());
        if (nbParticipants == 0) {
            // case of a blank round : the auction is completed
            if(bidByClient.size() == 0) {
                // notify all the clients to show the winner
                for (IClient c : clients) {
                    c.bidSold(winner);
                }

                // validate the registrations of clients in the monitor's queue
                validateRegistrations();

                // launch the next auction if there is one available and enough clients
                if (!auctions.isEmpty() && (clients.size() >= MIN_NUMBER_CLIENTS)) {
                    launchAuction();
                }
            } else {
                // compute the winner of the current round
                int maxBid = Integer.MIN_VALUE;
                for(Map.Entry<IClient, Integer> pair : bidByClient.entrySet()) {
                    if(pair.getValue() > maxBid) {
                        maxBid = pair.getValue();
                        winner = pair.getKey();
                    }
                }
                currentAuction.setPrice(maxBid);

                LOGGER.info("End of a round. Bid = " + maxBid + " - The current winner is " + client.getName());

                // clean the data structures before the next round
                nbParticipants = clients.size() - 1;
                bidByClient.clear();

                // notify the clients of the new price & start a new round
                for (IClient c : clients) {
                    c.newPrice(currentAuction.getUUID(), maxBid);
                }
            }
        }
    }
    
    public AuctionBean getCurrentAuction() throws RemoteException{
    	return currentAuction;
    }

    public IClient getWinner() throws RemoteException{
      return winner;
    }
}
