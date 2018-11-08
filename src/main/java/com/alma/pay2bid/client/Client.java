package com.alma.pay2bid.client;

import com.alma.pay2bid.bean.AuctionBean;
import com.alma.pay2bid.bean.ClientBean;
import com.alma.pay2bid.client.observable.IBidSoldObservable;
import com.alma.pay2bid.client.observable.INewAuctionObservable;
import com.alma.pay2bid.client.observable.INewPriceObservable;
import com.alma.pay2bid.client.observable.ITimerObservable;
import com.alma.pay2bid.client.observer.IBidSoldObserver;
import com.alma.pay2bid.client.observer.INewAuctionObserver;
import com.alma.pay2bid.client.observer.INewPriceObserver;
import com.alma.pay2bid.client.observer.ITimerObserver;
import com.alma.pay2bid.server.IServer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.logging.Logger;

/**
 * A Client a client that interact with a server running an Auction House
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public class Client extends UnicastRemoteObject implements IClient, IBidSoldObservable, INewAuctionObservable, INewPriceObservable , ITimerObservable{

    /**
     * A timer used to measure time between rounds
     */
    private class TimerManager extends TimerTask {

        private UUID auctionId;
        private String timeString;
        private long time = TIME_TO_RAISE_BID;

        public TimerManager(UUID auctionId, String timeMessage){
            this.auctionId = auctionId;
            this.timeString = timeMessage;
        }

        @Override
        public void run() {
            try {
                time -=TIME_TO_REFRESH;
                timeString = Long.toString(time/1000);
                if(time == 0) {
                    if (!Client.this.isSellerList.get(auctionId)) {
                        server.timeElapsed(auctionId,Client.this);
                    }
                } else {
                    if(newTimerObservers.get(auctionId) != null)
                        newTimerObservers.get(auctionId).updateTimer(timeString);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public String getTimeString() {
            return timeString;
        }

        public void setTimeString(String timeString) {
            this.timeString = timeString;
        }
    }

    private static final Logger LOGGER = Logger.getLogger(Client.class.getCanonicalName());
    private static final long TIME_TO_RAISE_BID = 30000;
    private static final long TIME_TO_REFRESH = 1000;

    private HashMap<UUID, AuctionBean> auctionList;
    private HashMap<UUID, ClientState> stateList;
    private HashMap<UUID, Boolean> isSellerList;
    private transient HashMap<UUID, Timer> timerList;

    private ClientBean identity;
    private IServer server;
    private String name;
    private String timeElapsed = "";
    private List<AuctionBean> wonAuctions;

    // collections of observers used to connect the client to the GUI
    private transient HashMap<UUID, ITimerObserver> newTimerObservers = new HashMap<UUID, ITimerObserver>();
    private transient HashMap<UUID, IBidSoldObserver> bidSoldObservers = new HashMap<UUID, IBidSoldObserver>();
    private transient Collection<INewAuctionObserver> newAuctionObservers = new ArrayList<INewAuctionObserver>();
    private transient HashMap<UUID, INewPriceObserver> newPriceObservers = new HashMap<UUID, INewPriceObserver>();

    /**
     * To get a server reference:
     * <pre>
     *     IServer server = (IServer) LocateRegistry.getRegistry(host, port).lookup("Server");
     * </pre>
     * @param server
     * @param name
     * @throws RemoteException
     */
    public Client(IServer server, String name) throws RemoteException {

        super();

        UUID uuid = UUID.randomUUID();
        String identifier = name + " #" + uuid.toString().substring(0,4);

        this.identity = new ClientBean(uuid, name, "default password", identifier);
        this.server = server;
        this.name = name;
        this.wonAuctions = new ArrayList<AuctionBean>();

        auctionList = new HashMap<UUID, AuctionBean>();
        stateList = new HashMap<UUID, ClientState>();
        isSellerList = new HashMap<UUID, Boolean>();
        timerList = new HashMap<UUID, Timer>();

    }

    /**
     * Register a new auction
     * @param auction
     * @throws RemoteException
     */
    @Override
    public void newAuction(UUID auctionId, AuctionBean auction) throws RemoteException {
        LOGGER.info("New auction received from the server \n");

        if (this.getIdentifier().equals(auction.getSeller())) {
    			this.isSellerList.put(auctionId, true);
        } else {
            this.isSellerList.put(auctionId, false);
        }
        auctionList.put(auctionId, auction);

        Timer newTimer = new Timer();
        newTimer.schedule(new TimerManager(auctionId, timeElapsed),0, TIME_TO_REFRESH);
        timerList.put(auctionId, newTimer);

        stateList.put(auctionId, ClientState.WAITING);

        // notify the observers of the new auction
        for(INewAuctionObserver observer : newAuctionObservers)
            observer.updateNewAuction(auction);
    }

    /**
     * Submit a new auction to the server
     * @param auction The submitted auction
     * @throws RemoteException
     */
    @Override
    public void submit(AuctionBean auction) throws RemoteException {
        LOGGER.info("New auction submitted to the server \n");
        server.placeAuction(auction);
    }

    /**
     * An item has been sold to a client
     * @param buyer The client who bought the current auction
     * @throws RemoteException
     */
    @Override
    public void bidSold(UUID auctionId, IClient buyer) throws RemoteException {

        LOGGER.info((buyer == null ? "nobody" : buyer.getIdentifier())
                + " won " + auctionList.get(auctionId).getName() + "\n");

        auctionList.remove(auctionId);
        isSellerList.remove(auctionId);

        timerList.get(auctionId).cancel();
        timerList.remove(auctionId);

        stateList.put(auctionId, ClientState.ENDING);
        stateList.remove(auctionId);

        // notify the observers of the new bid

        if(buyer != null)
            bidSoldObservers.get(auctionId).updateBidSold(buyer);
        else
            bidSoldObservers.get(auctionId).updateBidSold();
    }

    /**
     * Update the price of an item
     * @param auctionId UUID of the auction
     * @param price Price of the auction
     * @throws RemoteException
     */
    @Override
    public void newPrice(UUID auctionId, int price) throws RemoteException {
        LOGGER.info("New price received for the current auction \n");

        auctionList.get(auctionId).setPrice(price);

        if(timerList.get(auctionId) != null){
            timerList.get(auctionId).cancel();
            timerList.remove(auctionId);
        }

        Timer newTimer = new Timer();
        newTimer.schedule(new TimerManager(auctionId, timeElapsed),0, TIME_TO_REFRESH);
        timerList.put(auctionId, newTimer);

        stateList.put(auctionId, ClientState.WAITING);

        newPriceObservers.get(auctionId).updateNewPrice(auctionId, price);
    }

    /**
     * Update the price of an item
     * @param auctionId UUID of the auction
     * @param price Price of the auction
     * @throws RemoteException
     */
    @Override
    public void updatePrice(UUID auctionId, int price) throws RemoteException {
        LOGGER.info("New price received for the current auction \n");

        auctionList.get(auctionId).setPrice(price);

        // notify the observers of the new price for the current auction
        newPriceObservers.get(auctionId).updateNewPrice(auctionId, price);
    }

    public void addWonAuction(AuctionBean auction) throws RemoteException{
            this.wonAuctions.add(auction);
    }

    public List<AuctionBean> getWonAuctions(){return wonAuctions;}

    public IServer getServer(){ return server;}

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public void setName(String name) throws RemoteException {
        this.name = name;
    }

    @Override
    public ClientState getState(UUID auctionId) throws RemoteException {
        return stateList.get(auctionId);
    }

    @Override
    public void setState(UUID auctionId, ClientState state) {
        this.stateList.put(auctionId, state);
    }

    @Override
    public void addNewPriceObserver(UUID auctionId, INewPriceObserver observer) { newPriceObservers.put(auctionId, observer); }

    @Override
    public void removeNewPriceObserver(UUID auctionId) {
        newPriceObservers.remove(auctionId);
    }

    @Override
    public void addBidSoldObserver(UUID auctionId, IBidSoldObserver observer) { bidSoldObservers.put(auctionId, observer); }

    @Override
    public void removeBidSoldObserver(UUID auctionId) { bidSoldObservers.remove(auctionId); }

    @Override
    public boolean addNewAuctionObserver(INewAuctionObserver observer) { return newAuctionObservers.add(observer); }

    @Override
    public boolean removeNewAuctionObserver(INewAuctionObserver observer) { return newAuctionObservers.remove(observer); }

    @Override
    public void addTimerObserver(UUID auctionId, ITimerObserver observer) {
        newTimerObservers.put(auctionId, observer);
    }

    @Override
    public void removeTimerObserver(UUID auctionId) {
        newTimerObservers.remove(auctionId);
    }

    @Override
    public boolean getIsSeller(UUID auctionId) throws RemoteException{
          return this.isSellerList.get(auctionId);
    }

    @Override
    public void setIsSeller(UUID auctionId, boolean v) throws RemoteException{
      this.isSellerList.put(auctionId, v);
    }

    @Override
    public String getIdentifier(){return identity.getIdentifier(); }
}
