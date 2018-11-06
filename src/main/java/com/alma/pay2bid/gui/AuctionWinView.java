package com.alma.pay2bid.gui;

import com.alma.pay2bid.bean.AuctionBean;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

import static java.lang.System.exit;

public class AuctionWinView extends JPanel{

    /**
     * Properties for the main panel
     */
    private JFrame wonAuctionsFrame;
    private JPanel wonAuctionsPanel;

    public AuctionWinView(){

        wonAuctionsFrame = new JFrame("List of won auctions");
        Dimension dimension = new Dimension(500, 300);
        wonAuctionsFrame.setSize(500, 300);
        wonAuctionsFrame.setMaximumSize(dimension);

        wonAuctionsPanel = new JPanel();
        wonAuctionsPanel.setMaximumSize(new Dimension(600, 200));
        wonAuctionsPanel.setLayout(new GridLayout(0,4, 5, 5));

        wonAuctionsFrame.add(wonAuctionsPanel);

        wonAuctionsFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent){
                hideFrame();
            }
        });
    }


    public void showFrame(){
        wonAuctionsFrame.setVisible(true);
    }

    public void hideFrame(){ wonAuctionsFrame.setVisible(false); }


    public void showWonAuctions(java.util.List<AuctionBean> wonAuctions){

        for(AuctionBean auction : wonAuctions){
            // Create the name label
            JLabel auctionNameLabel = new JLabel(" Name : ");
            JLabel auctionNameValue = new JLabel("");
            auctionNameValue.setText(auction.getName());
            auctionNameValue.setLabelFor(auctionNameLabel);
            wonAuctionsPanel.add(auctionNameLabel);
            wonAuctionsPanel.add(auctionNameValue);

            // Create the price label
            JLabel auctionPriceLabel = new JLabel(" Price : ");
            JLabel auctionPriceValue = new JLabel("");
            auctionPriceValue.setText(Integer.toString(auction.getPrice()));
            auctionPriceValue.setLabelFor(auctionPriceLabel);
            wonAuctionsPanel.add(auctionPriceLabel);
            wonAuctionsPanel.add(auctionPriceValue);
        }

        showFrame();
    }

    public JPanel getAuctionPanel() {
        return wonAuctionsPanel;
    }

}
