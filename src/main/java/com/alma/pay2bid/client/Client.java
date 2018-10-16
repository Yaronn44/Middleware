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
        private String timeString;
        private long time = TIME_TO_RAISE_BID;

        public TimerManager(String timeMessage){
            this.timeString = timeMessage;
        }

        @Override
        public void run() {
            try {
                time -=TIME_TO_REFRESH;
                timeString = Long.toString(time/1000);
                if(time == 0) {
                		if (!Client.this.estVendeur) {
                			server.timeElapsed(Client.this);
                		}
                } else {
                    for(ITimerObserver o : newTimerObservers){
                        o.updateTimer(timeString);
                    }
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

    private ClientBean identity;
    private IServer server;
    private transient Timer timer;
    private AuctionBean currentAuction;
    private String name;
    private String timeElapsed;
    private ClientState state;
    private boolean estVendeur;

    // collections of observers used to connect the client to the GUI
    private transient Collection<ITimerObserver> newTimerObservers = new ArrayList<ITimerObserver>();
    private transient Collection<IBidSoldObserver> bidSoldObservers = new ArrayList<IBidSoldObserver>();
    private transient Collection<INewAuctionObserver> newAuctionObservers = new ArrayList<INewAuctionObserver>();
    private transient Collection<INewPriceObserver> newPriceObservers = new ArrayList<INewPriceObserver>();

    /**
     * To get a server reference:
     * <pre>
     *     IServer server = (IServer) LocateRegistry.getRegistry(host, port).lookup("Server");
     * </pre>
     * @param server
     * @param name
     * @throws RemoteException
     */
    public Client(IServer server) throws RemoteException {
        super();

        identity = new ClientBean(UUID.randomUUID(), name, "default password", name);
        this.server = server;
        this.name = "inconnu";
        this.estVendeur = false;
        state = ClientState.WAITING;
    }

    /**
     * Register a new auction
     * @param auction
     * @throws RemoteException
     */
    @Override
    public void newAuction(AuctionBean auction) throws RemoteException {
        LOGGER.info("New auction received from the server");
        if (this.getName().equals(auction.getVendeur())) {
    			this.setEstVendeur(true);
        } else { 
    			this.setEstVendeur(false);
        }
        currentAuction = auction;

        timer = new Timer();
        timer.schedule(new TimerManager(timeElapsed),0, TIME_TO_REFRESH);

        state = ClientState.WAITING;

        // notify the observers of the new auction
        for (INewAuctionObserver observer : newAuctionObservers) {
            observer.updateNewAuction(auction);
        }
    }

    /**
     * Submit a new auction to the server
     * @param auction
     * @throws RemoteException
     */
    @Override
    public void submit(AuctionBean auction) throws RemoteException {
        LOGGER.info("New auction submitted to the server");
        server.placeAuction(auction);
    }

    /**
     * An item has been sold to a client
     * @param buyer
     * @throws RemoteException
     */
    @Override
    public void bidSold(IClient buyer) throws RemoteException {
        LOGGER.info((buyer == null ? "nobody" : buyer.getName()) + " won " + currentAuction.getName());

        currentAuction = null;

        this.estVendeur = false;

        timer.cancel();
        timer = null;

        state = ClientState.ENDING;

        // notify the observers of the new bid
        for (IBidSoldObserver observer : bidSoldObservers) {
        	if(buyer != null)
        		observer.updateBidSold(buyer);
        }
    }

    /**
     * Update the price of an item
     * @param auctionID
     * @param price
     * @throws RemoteException
     */
    @Override
    public void newPrice(UUID auctionID, int price) throws RemoteException {
        LOGGER.info("New price received for the current auction");

        currentAuction.setPrice(price);

        if(timer != null) {
            timer.cancel();
            timer = null;
        }

        timer = new Timer();
        timer.schedule(new TimerManager(timeElapsed),0, TIME_TO_REFRESH);

        state = ClientState.WAITING;

        // notify the observers of the new price for the current auction
        for (INewPriceObserver observer : newPriceObservers) {
            observer.updateNewPrice(auctionID, price);
        }
    }

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
    public ClientState getState() throws RemoteException {
        return state;
    }

    @Override
    public void setState(ClientState state) {
        this.state = state;
    }

    @Override
    public boolean addNewPriceObserver(INewPriceObserver observer) {
        return newPriceObservers.add(observer);
    }

    @Override
    public boolean removeNewPriceObserver(INewPriceObserver observer) {
        return newPriceObservers.remove(observer);
    }

    @Override
    public boolean addBidSoldObserver(IBidSoldObserver observer) {
        return bidSoldObservers.add(observer);
    }

    @Override
    public boolean removeBidSoldObserver(IBidSoldObserver observer) {
        return bidSoldObservers.remove(observer);
    }

    @Override
    public boolean addNewAuctionObserver(INewAuctionObserver observer) {
        return newAuctionObservers.add(observer);
    }

    @Override
    public boolean removeNewAuctionObserver(INewAuctionObserver observer) {
        return newAuctionObservers.remove(observer);
    }

    @Override
    public boolean addTimerObserver(ITimerObserver observer) {
        return newTimerObservers.add(observer);
    }

    @Override
    public boolean removeTimerObserver(ITimerObserver observer) {
        return newTimerObservers.remove(observer);
    }

    public boolean getEstVendeur(){
      return this.estVendeur;
    }

    public void setEstVendeur(boolean v) throws RemoteException{
      this.estVendeur = v ;
    }
}
