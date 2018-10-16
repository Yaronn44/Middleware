package com.alma.pay2bid.gui.listeners;

import com.alma.pay2bid.client.Client;
import com.alma.pay2bid.client.IClient;
import com.alma.pay2bid.gui.AuctionView;
import com.alma.pay2bid.server.IServer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

/**
 * An ActionListener called to raise the bid of an item
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public class RaiseBidButtonListener implements ActionListener {
    private IClient client;
    private IServer server;
    private AuctionView auctionView;
    private JTextField bidField;
    private JLabel statusLabel;

    public RaiseBidButtonListener(IClient client, IServer server, AuctionView gui, JLabel statusLabel) {
        this.client = client;
        this.server = server;
        auctionView = gui;
        this.bidField = gui.getAuctionBid();
        this.statusLabel = statusLabel;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String command = actionEvent.getActionCommand();
        if("raiseBid".equals(command))  {
            try {
            	int bidFieldValue = Integer.valueOf(bidField.getText());

	        	if(bidFieldValue > server.getCurrentAuction().getPrice()) {
	        		statusLabel.setText("New bid sent.");
	        		
	                server.raiseBid(client, bidFieldValue);
	                auctionView.disable();
	                try {
						server.timeElapsed(this.client);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	        	}
	        	else
	        		statusLabel.setText("Bid must higher than the current auction");

	            
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
