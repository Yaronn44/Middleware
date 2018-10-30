package com.alma.pay2bid.server;

import com.alma.pay2bid.bean.AuctionBean;
import com.alma.pay2bid.client.ClientState;
import com.alma.pay2bid.client.IClient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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
                    LOGGER.info("Detected a closed connection with " + client.toString() + "\n");
                    clientsToRemove.add(client);
                }
            }

            if(clientsToRemove.size() > 0) {
                try {
                    clientsCrashed(clientsToRemove);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());
    private static final long CHECK_CONN_DELAY = 5000;

    private HashMap<UUID, AuctionBean> auctionsList = new HashMap<UUID, AuctionBean>();
    private HashMap<UUID, IClient> winnersList = new HashMap<UUID, IClient>();
    private HashMap<UUID, HashMap<IClient, Integer>> bidByClientList = new HashMap<UUID, HashMap<IClient, Integer>>();
    private HashMap<UUID, Integer> nbParticipantsList = new HashMap<UUID, Integer>();
    private HashMap<UUID, Integer> bidThisRoundList = new HashMap<UUID, Integer>();
    private HashMap<UUID, ArrayList<String>> nameClientsBidList = new HashMap<UUID, ArrayList<String>>();

    private boolean auctionInProgress = false;
    private List<IClient> clients = new ArrayList<IClient>();

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
    private void launchAuction(UUID auctionId, AuctionBean auction) throws RemoteException {

        nbParticipantsList.put(auctionId, clients.size() - 1); // We do not count the seller as an active participant

        auctionInProgress = true;

        // Notify the clients that a new auction has begun
        for (IClient client : clients) {
            client.newAuction(auctionId, auction);
        }
    }

    /**
     * Submit a new auction in the auction's queue
     * @param auction The auction submitted
     * @throws RemoteException
     */
    @Override
    public synchronized void placeAuction(AuctionBean auction) throws RemoteException {
        // Generate a new UUID for the incoming auction, then put it in the queue
        auction.setUuid(UUID.randomUUID());
        auctionsList.put(auction.getUuid(), auction);
        winnersList.put(auction.getUuid(), null);
        bidByClientList.put(auction.getUuid(), new HashMap<IClient, Integer>());
        bidThisRoundList.put(auction.getUuid(), 0);
        nbParticipantsList.put(auction.getUuid(), 0);
        nameClientsBidList.put(auction.getUuid(), new ArrayList<String>());

        LOGGER.info("Auction " + auction.getName() + " from " + auction.getSeller() + " placed in queue \n");
        if (clients.size() >= MIN_NUMBER_CLIENTS) {
            launchAuction(auction.getUuid(), auction);
        }
    }

    /**
     * Use when there is no auction actually running due to a lack of clients and when a client just connected
     *
     */
    public synchronized
    void checkForAuction() throws RemoteException {
        if (!auctionInProgress && (auctionsList.size() > 0) && (clients.size() >= MIN_NUMBER_CLIENTS)) {
            for(Map.Entry<UUID, AuctionBean> pair : auctionsList.entrySet())
                launchAuction(pair.getKey(), pair.getValue());
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
            // TODO find a new way to add the client without letting him be in the currents auctions but without letting him
            // TODO wait for all auctions to finish
            while (auctionInProgress) {
                wait();
            }

            clients.add(client);
            LOGGER.info("Client " +  client.getIdentifier() + " connected \n" + client.toString() + "\n");
            checkForAuction();

        } catch (InterruptedException e) {
            LOGGER.warning(e.getMessage()  + "\n");
        }
    }

    /**
     * Disconnect a client from the server
     * @param client The client who will be disconnected from the server
     * @throws RemoteException
     * @throws InterruptedException
     */
    @Override
    public void disconnect(IClient client) throws RemoteException, InterruptedException {
        try {
			LOGGER.info("Disconnect : Client " + client.toString() + "\n");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        this.clientDisconnection(client);
    }

    /**
     * Validate and flush all waiting registrations
     */
    private synchronized void validateRegistrations(UUID winnerId) {

        if(auctionsList.size() == 0)
            auctionInProgress = false;

        winnersList.remove(winnerId);

        notifyAll();
    }

    /**
     * Raise the bid on an item
     * @param client The client who raise the bid
     * @param newBid The value of the new bid
     * @throws RemoteException
     */
    @Override
    public synchronized void raiseBid(UUID auctionId, IClient client, int newBid) throws RemoteException {
        if((client.getState(auctionId) == ClientState.WAITING) && (newBid > auctionsList.get(auctionId).getPrice())) {
            bidByClientList.get(auctionId).put(client, newBid);
            bidThisRoundList.put(auctionId, bidThisRoundList.get(auctionId) + 1);

            client.setState(auctionId, ClientState.RAISING);
            LOGGER.info("New bid " + newBid + " placed by client " + client.toString()
                    + " on auction : " + auctionsList.get(auctionId).getName() + "\n");
        }
    }

    /**
     * Notifies the server that a client's timer has reach zero.
     * @param client
     * @throws RemoteException
     * @throws InterruptedException
     */
    @Override
    public synchronized void timeElapsed(UUID auctionId, IClient client) throws RemoteException, InterruptedException {

        nbParticipantsList.put(auctionId, nbParticipantsList.get(auctionId) - 1);
        LOGGER.info("A client time elapsed : " + client.getIdentifier() + "for auction : "
                + auctionsList.get(auctionId).getName() + "\n");

        if (nbParticipantsList.get(auctionId) == 0) {
            // Case of a blank round : the auction is completed
            if(bidThisRoundList.get(auctionId) == 0) {

                // Notify all the clients to show the winner
                for (IClient c : clients) {
                    c.bidSold(auctionId, winnersList.get(auctionId));
                }

                bidByClientList.get(auctionId).clear();

                if(winnersList.get(auctionId) != null) {
                    winnersList.get(auctionId).addAuctionWin(auctionsList.get(auctionId));
                }

                // Validate the registrations of clients in the monitor's queue
                validateRegistrations(auctionId);

            } else {
                roundWinner(auctionId);
            }
        }
    }

    /**
     * Notifies the server that a client's has disconnected
     * @param client
     * @throws RemoteException
     * @throws InterruptedException
     */
    @Override
    public synchronized void clientDisconnection(IClient client) throws RemoteException, InterruptedException {

        clients.remove(client);

        for(Map.Entry<UUID, AuctionBean> auctionsPair : auctionsList.entrySet()) {

            UUID auctionId = auctionsPair.getKey();
            AuctionBean auctionValue = auctionsPair.getValue();

            if (!(client.getState(auctionsPair.getKey()) == ClientState.RAISING)) {
                nbParticipantsList.put(auctionId, nbParticipantsList.get(auctionId) - 1);
            }

            bidByClientList.get(auctionId).remove(client);

            LOGGER.info("A client has disconnected : " + client.getIdentifier() + "\n");

            // If everybody bid or leave
            if (nbParticipantsList.get(auctionId) == 0) {
                // Case of a blank round : the auction is completed
                if (bidThisRoundList.get(auctionId) == 0) {

                    // If the actual client who's quitting the client is suppose to be the winner of one of the auctions, then we change the winner
                    if(winnersList.get(auctionId) != null && winnersList.get(auctionId).getIdentifier().equals(client.getIdentifier())) {
                        defineWinner(auctionId);
                    }

                    // Notify all the clients to show the winner
                    for (IClient c : clients) {
                        c.bidSold(auctionId, winnersList.get(auctionId));
                    }

                    if (winnersList.get(auctionId) != null) {
                        winnersList.get(auctionId).addAuctionWin(auctionValue);
                    }

                    bidByClientList.get(auctionId).clear();

                    // Validate the registrations of clients in the monitor's queue
                    validateRegistrations(auctionId);
                } else {
                    roundWinner(auctionId);
                }
            } else if (winnersList.get(auctionId) != null && winnersList.get(auctionId).getIdentifier().equals(client.getIdentifier())) {
                defineWinner(auctionId);

                for (IClient c : clients) {
                    c.updatePrice(auctionId, auctionValue.getPrice());
                }
            }
        }
    }

    /**
     * Notifies the server that a clients have crashed
     * @param clientsCrashed
     * @throws RemoteException
     */
    @Override
    public synchronized void clientsCrashed(List<IClient> clientsCrashed) throws RemoteException{

        clients.removeAll(clientsCrashed);

        for(Map.Entry<UUID, AuctionBean> auctionsPair : auctionsList.entrySet()) {

            UUID auctionId = auctionsPair.getKey();
            AuctionBean auctionValue = auctionsPair.getValue();

            int counter = 0;

            for (IClient client : clients){
                for (String nameClientBid : nameClientsBidList.get(auctionId)) {
                    if (nameClientBid.equals(client.getIdentifier())) {
                        break;
                    }
                    counter++;
                }
            }

            nbParticipantsList.put(auctionId, nbParticipantsList.get(auctionId) - (clientsCrashed.size() - counter));


            for (IClient client : clientsCrashed){
                bidByClientList.get(auctionId).remove(client);
            }

            LOGGER.info(clientsCrashed.size() + " clients have crashed \n");

            // If everybody bid or leave
            if (nbParticipantsList.get(auctionId) == 0) {
                // Case of a blank round : the auction is completed
                if (bidThisRoundList.get(auctionId) == 0) {

                    // If one of the clients who crashed is the winner we change it with someone else
                    if(winnersList.get(auctionId) != null){
                        try{
                            winnersList.get(auctionId).getName();
                        }catch(Exception e){
                            defineWinner(auctionId);
                        }
                    }

                    // Notify all the clients to show the winner
                    for (IClient c : clients) {
                        c.bidSold(auctionId, winnersList.get(auctionId));
                    }

                    if (winnersList.get(auctionId) != null) {
                        winnersList.get(auctionId).addAuctionWin(auctionValue);
                    }

                    bidByClientList.get(auctionId).clear();

                    // Validate the registrations of clients in the monitor's queue
                    validateRegistrations(auctionId);
                } else {
                    roundWinner(auctionId);
                }
            } else {
                if(winnersList.get(auctionId) != null){
                    try{
                        winnersList.get(auctionId).getName();
                    }catch(Exception e){
                        defineWinner(auctionId);
                    }
                }
                for (IClient c : clients) {
                    c.updatePrice(auctionId, auctionValue.getPrice());
                }

            }
        }
    }

    public IClient getWinner(UUID auctionId) throws RemoteException{
      return winnersList.get(auctionId);
    }

    public AuctionBean getAuction(UUID auctionId) throws RemoteException{
        return auctionsList.get(auctionId);
    }

    public static TreeMap<IClient, Integer> sortMapByValue(HashMap<IClient, Integer> map){
        Comparator<IClient> comparator = new ValueComparator(map);
        //TreeMap is a map sorted by its keys.
        //The comparator is used to sort the TreeMap by keys.
        TreeMap<IClient, Integer> result = new TreeMap<IClient, Integer>(comparator);
        result.putAll(map);
        return result;
    }


    public void roundWinner(UUID auctionId)  throws RemoteException{
        // Compute the winner of the current round

        defineWinner(auctionId);
        bidThisRoundList.put(auctionId, 0);

        if(winnersList.get(auctionId) != null) {
            LOGGER.info("End of a round. Bid = " + auctionsList.get(auctionId).getPrice()
                    + " - The current winner is " + winnersList.get(auctionId).getIdentifier() + "\n");

            // clean the data structures before the next round
            nbParticipantsList.put(auctionId, clients.size() - 1);

            // notify the clients of the new price & start a new round
            for (IClient c : clients) {
                c.newPrice(auctionId, auctionsList.get(auctionId).getPrice());
            }
        } else {
            LOGGER.info("There is no winners in this round \n");
        }

    }

    public void defineWinner(UUID auctionId){

        int maxBid = Integer.MIN_VALUE;
        ArrayList<IClient> highestBidsClients = new ArrayList<IClient>();

        TreeMap<IClient, Integer> sortedMap = sortMapByValue(bidByClientList.get(auctionId));

        // We get all the clients who bid the same amount
        if(sortedMap.size() != 0){
            maxBid = sortedMap.firstEntry().getValue();
            for(Map.Entry<IClient, Integer> pair : sortedMap.entrySet()) {

                if(pair.getValue() == maxBid) {
                    highestBidsClients.add(pair.getKey());
                } else {
                    break;
                }
            }
        }

        if(highestBidsClients.size() != 0) {
            // And we chose a random one between them
            Random r = new Random();
            int index = r.nextInt(highestBidsClients.size());

            winnersList.put(auctionId, highestBidsClients.get(index));

            auctionsList.get(auctionId).setPrice(maxBid);

            try {
                LOGGER.info("The actual winner is : " + winnersList.get(auctionId).getIdentifier()
                        + " with a bid of : " + auctionsList.get(auctionId).getPrice()
                        + " for the auction : " + auctionsList.get(auctionId).getName() + "\n");
            }catch(RemoteException e){
                e.printStackTrace();
            }
        } else{
            winnersList.put(auctionId, null);
            LOGGER.info("There is no winner found \n");
        }
    }
}
