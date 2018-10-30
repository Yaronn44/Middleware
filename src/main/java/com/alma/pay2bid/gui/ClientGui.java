package com.alma.pay2bid.gui;

import com.alma.pay2bid.bean.AuctionBean;
import com.alma.pay2bid.client.Client;
import com.alma.pay2bid.client.IClient;
import com.alma.pay2bid.client.observer.IBidSoldObserver;
import com.alma.pay2bid.client.observer.INewAuctionObserver;
import com.alma.pay2bid.client.observer.INewPriceObserver;
import com.alma.pay2bid.client.observer.ITimerObserver;
import com.alma.pay2bid.gui.listeners.AuctionInputListener;
import com.alma.pay2bid.gui.listeners.AuctionWinListener;
import com.alma.pay2bid.gui.listeners.RaiseBidButtonListener;
import com.alma.pay2bid.server.IServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

import static java.lang.System.exit;

/**
 * The main GUI of the application client-side
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public class ClientGui {

    private static final Logger LOGGER = Logger.getLogger(ClientGui.class.getCanonicalName());
    private Client client;
    private IServer server;
    private HashMap<UUID, AuctionView> auctionsList;

    /**
     * Main frame & elements
     */
    private JFrame mainFrame;
    private JLabel statusLabel;
    private JPanel mainPanel;
    private JPanel auctionPanel;

    /**
     * Constructor
     * @param client
     */
    public ClientGui(Client client, IServer server) throws RemoteException, InterruptedException {

        this.client = client;
        this.server = server;
        auctionsList = new HashMap<UUID, AuctionView>();

        client.addNewAuctionObserver(new INewAuctionObserver() {
            @Override
            public void updateNewAuction(AuctionBean auction) {
                LOGGER.info("A new auction needs to be added to the GUI");
                addAuctionPanel(auction);
            }
        });
        // paint the GUI
        createGui();

        server.register(this.client);
    }

    /**
     * Initialize the GUI & populate it with the base elements
     */
    private void createGui() {

        // Create the Main JFrame
        try{
            mainFrame = new JFrame("Pay2Bid - User : " + client.getIdentifier());
        }catch(Exception e){
            System.out.println(e);
        }
        Dimension dimension = new Dimension(500, 300);
        mainFrame.setSize(500, 300);
        mainFrame.setMaximumSize(dimension);
        mainFrame.setLayout(new BorderLayout());

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent){
                try {
                    server.disconnect(client);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                exit(0);
            }
        });

        // Create the Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenuItem newAuction = new JMenuItem("New Auction");
        newAuction.setActionCommand("newAuction");
        newAuction.addActionListener(new AuctionInputListener(this));
        menuBar.add(newAuction);

        JMenuItem winAuctions = new JMenuItem("Auctions win");
        winAuctions.setActionCommand("winAuctions");
        winAuctions.addActionListener(new AuctionWinListener(this));
        menuBar.add(winAuctions);

        mainFrame.setJMenuBar(menuBar);

        // Create the Frame Header
        JLabel headerLabel = new JLabel("", JLabel.CENTER);
        headerLabel.setText("Current Auctions");

        statusLabel = new JLabel("", JLabel.CENTER);
        statusLabel.setBackground(Color.red);
        statusLabel.setSize(400,0);

        mainFrame.add(headerLabel, BorderLayout.PAGE_START);

        // Create the Main panel which will contains the GUI's elements
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        auctionPanel = new JPanel();
        auctionPanel.setLayout(new BoxLayout(auctionPanel, BoxLayout.Y_AXIS));
        mainPanel.add(auctionPanel);
        mainPanel.setMaximumSize(dimension);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        mainFrame.add(scrollPane, BorderLayout.CENTER);

        mainFrame.add(statusLabel, BorderLayout.PAGE_END);
    }

    /**
     * Show the client GUI
     */
    public void show(){
        mainFrame.setVisible(true);
    }

    /**
     * Add a new panel to the main panel which display a new Auction
     * @param auctionBean
     */
    private void addAuctionPanel(AuctionBean auctionBean){
        if(!auctionsList.containsKey(auctionBean.getUuid())) {
            LOGGER.info("Add new auction to auctionPanel \n");

            //auctionPanel.removeAll();
            final AuctionView auction = new AuctionView(auctionBean);
            
            JButton raiseBidButton = new JButton("Raise the bid");
            raiseBidButton.setActionCommand("raiseBid");

            raiseBidButton.addActionListener(new RaiseBidButtonListener(auctionBean.getUuid(), client, client.getServer(), auction, statusLabel));
            if (this.client.getIsSeller(auctionBean.getUuid())) {
              raiseBidButton.setEnabled(false);
              raiseBidButton.setVisible(false);
              auction.setEnableBidTextField(false);
              auction.setEnableBidTextFieldVisible(false);
            }
            auction.setRaiseButton(raiseBidButton);


            //Now add the observer to receive all price updates
            client.addNewPriceObserver(new INewPriceObserver() {
                @Override
                public void updateNewPrice(UUID auctionId, Integer price) {
                    setAuctionPrice(auctionId, price);
                    if (!client.getIsSeller(auctionId)) {
                    		auction.enable();
                    }		
                }
            });

            // Add a observer to receive the notification when the bid is sold
            client.addBidSoldObserver(new IBidSoldObserver() {
                @Override
                public void updateBidSold(IClient client) {
                    try {
                        auction.setWinner(client.getIdentifier());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void updateBidSold() {
                    auction.setWinner(null);
                }
            });

            client.addTimerObserver(auctionBean.getUuid(), new ITimerObserver() {
                @Override
                public void updateTimer(String time) {
                    auction.setAuctionTimer(time);
                }
            });

            auctionPanel.add(auction.getAuctionPanel());
            auctionsList.put(auctionBean.getUuid(), auction);

            mainPanel.revalidate();
            mainPanel.repaint();

        } else {
            LOGGER.warning("Trying to add a duplicated auction to the list - Auction : " + auctionBean.toString() + "\n");
        }
    }

    /**
     * Set an new price for a given AuctionBean
     */
    private void setAuctionPrice(UUID auctionId, int newPrice){
        LOGGER.info("auctionPrice set ! \n");
        AuctionView auction = auctionsList.get(auctionId);

        // Update auction in our list
        auction.setPrice(newPrice);

          //update the current winner
        try {
          auction.setCurrentWinner(server.getWinner(auctionId).getIdentifier());
        } catch (RemoteException e){
            e.printStackTrace();
        }

        //Reload the main panel
        auction.getAuctionPanel().revalidate();
        auction.getAuctionPanel().repaint();

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    /**
     * Create the menu to add a new Auction
     */
    public void newAuctionView() {
        AuctionInput input = new AuctionInput(client);
        input.showFrame();
    }

    public void winAuctionsView(){
        AuctionWinView auctionsWin = new AuctionWinView();
        auctionsWin.showAuctionWin(client.getAuctionsWin());
    }
}
