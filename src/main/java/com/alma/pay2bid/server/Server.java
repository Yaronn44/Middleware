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

        currentAuction = auctions.poll();

        auctionInProgress = true;
        nbParticipants = clients.size() - 1; // We do not count the seller as an active participant

        // Notify the clients that a new auction has begun
        for (IClient client : clients) {
            client.newAuction(currentAuction);
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
        auctions.add(auction);

        LOGGER.info("Auction " + auction.getName() + " from " + auction.getSeller() + " placed in queue \n");
        if (!auctionInProgress && (auctions.size() == 1) && (clients.size() >= MIN_NUMBER_CLIENTS)) {
            launchAuction();
        }
    }

    /**
     * Use when there is no auction actually running due to a lack of clients and when a client just connected
     *
     */
    public synchronized
    void checkForAuction() throws RemoteException {
        if (!auctionInProgress && (auctions.size() > 0) && (clients.size() >= MIN_NUMBER_CLIENTS)) {
            LOGGER.info(auctions.peek().toString());
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
            LOGGER.info("Client " +  client.getIdentifier() + " connected \n" + client.toString() + "\n");
            LOGGER.info("NB Client " + clients.size() + "\n");
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
    private synchronized void validateRegistrations() {
        auctionInProgress = false;
        notifyAll();
    }

    /**
     * Raise the bid on an item
     * @param client The client who raise the bid
     * @param newBid The value of the new bid
     * @throws RemoteException
     */
    @Override
    public synchronized void raiseBid(IClient client, int newBid) throws RemoteException {
        if((client.getState() == ClientState.WAITING) && (newBid > currentAuction.getPrice())) {
            bidByClient.put(client, newBid);
            client.setState(ClientState.RAISING);
            LOGGER.info("New bid " + newBid + " placed by client " + client.toString() + "\n");
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
        LOGGER.info("A client time elapsed : " + client.getIdentifier() + "\n");

        if (nbParticipants == 0) {
            // Case of a blank round : the auction is completed
            if(bidByClient.size() == 0) {

                // Notify all the clients to show the winner
                for (IClient c : clients) {
                    c.bidSold(winner);
                }

                // Validate the registrations of clients in the monitor's queue
                validateRegistrations();

                // Launch the next auction if there is one available and enough clients
                if (!auctions.isEmpty() && (clients.size() >= MIN_NUMBER_CLIENTS)) {
                    launchAuction();
                }

            } else {
                roundWinner();
            }
        }
    }

    public synchronized void clientDisconnection(IClient client) throws RemoteException, InterruptedException {

        clients.remove(client);

        if(!bidByClient.containsKey(client)){
            nbParticipants--;
        }else{
            bidByClient.remove(client);
        }

        LOGGER.info("A client has disconnected : " + client.getIdentifier() + "\n");

        // If everybody bid or leave
        if (nbParticipants == 0) {
            // Case of a blank round : the auction is completed
            if (bidByClient.size() == 0) {

                // If the actual client who's quitting the client is suppose to be the winner, then we change the winner
                if(winner != null && winner.getIdentifier().equals(client.getIdentifier())){
                    defineWinner();
                }

                // Notify all the clients to show the winner
                for (IClient c : clients) {
                    c.bidSold(winner);
                }

                // Validate the registrations of clients in the monitor's queue
                validateRegistrations();
            } else{
                roundWinner();
            }
        }else if(winner != null && winner.getIdentifier().equals(client.getIdentifier())){
            defineWinner();
        }
    }

    public synchronized void clientsCrashed(List<IClient> clientsCrashed) throws RemoteException{

        clients.removeAll(clientsCrashed);

        for (IClient client : clientsCrashed){
            if(!bidByClient.containsKey(client)){
                nbParticipants--;
            }else{
                bidByClient.remove(client);
            }
        }

        LOGGER.info(clientsCrashed.size() + " clients have crashed \n");

        if (nbParticipants == 0) {
            // Case of a blank round : the auction is completed
            if (bidByClient.size() == 0) {

                // If one of the clients who crashed is the winner we change it with someone else
                for(IClient client : clientsCrashed){
                    if(winner != null && winner.getIdentifier().equals(client.getIdentifier())){
                        defineWinner();
                    }
                }

                for (IClient c : clients) {
                    c.bidSold(winner);
                }

                // Validate the registrations of clients in the monitor's queue
                validateRegistrations();
            }else{
                roundWinner();
            }
        } else{
            for(IClient client : clientsCrashed){
                if(winner != null && winner.getIdentifier().equals(client.getIdentifier())){
                    defineWinner();
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

    public static TreeMap<IClient, Integer> sortMapByValue(HashMap<IClient, Integer> map){
        Comparator<IClient> comparator = new ValueComparator(map);
        //TreeMap is a map sorted by its keys.
        //The comparator is used to sort the TreeMap by keys.
        TreeMap<IClient, Integer> result = new TreeMap<IClient, Integer>(comparator);
        result.putAll(map);
        return result;
    }


    public void roundWinner()  throws RemoteException{
        // Compute the winner of the current round

        defineWinner();

        if(winner != null) {
            LOGGER.info("End of a round. Bid = " + currentAuction.getPrice() + " - The current winner is " + winner.getIdentifier() + "\n");

            // clean the data structures before the next round
            nbParticipants = clients.size() - 1;
            bidByClient.clear();

            // notify the clients of the new price & start a new round
            for (IClient c : clients) {
                c.newPrice(currentAuction.getUUID(), currentAuction.getPrice());
            }
        } else {
            LOGGER.info("There is no winners in this round \n");
        }
    }

    public void defineWinner(){

        int maxBid = Integer.MIN_VALUE;
        ArrayList<IClient> highestBidsClients = new ArrayList<IClient>();

        TreeMap<IClient, Integer> sortedMap = sortMapByValue(bidByClient);

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

            winner = highestBidsClients.get(index);

            currentAuction.setPrice(maxBid);
            try {
                LOGGER.info("The actual winner is : " + winner.getIdentifier() + "\n");
            }catch(RemoteException e){
                e.printStackTrace();
            }
        } else{
            winner = null;
            LOGGER.info("There is no winner found \n");
        }
    }
}
