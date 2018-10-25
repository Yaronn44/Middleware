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
    private JFrame auctionWinFrame;
    private JPanel auctionWinPanel;

    public AuctionWinView(){

        auctionWinFrame = new JFrame("List of auctions win");
        Dimension dimension = new Dimension(500, 300);
        auctionWinFrame.setSize(500, 300);
        auctionWinFrame.setMaximumSize(dimension);

        auctionWinPanel = new JPanel();
        auctionWinPanel.setMaximumSize(new Dimension(600, 200));
        auctionWinPanel.setLayout(new GridLayout(0,4, 5, 5));

        auctionWinFrame.add(auctionWinPanel);

        auctionWinFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent){
                hideFrame();
            }
        });
    }


    public void showFrame(){
        auctionWinFrame.setVisible(true);
    }

    public void hideFrame(){ auctionWinFrame.setVisible(false); }


    public void showAuctionWin(java.util.List<AuctionBean> auctionsWin){

        for(AuctionBean auction : auctionsWin){
            // Create the name label
            JLabel auctionNameLabel = new JLabel(" Name : ");
            JLabel auctionNameValue = new JLabel("");
            auctionNameValue.setText(auction.getName());
            auctionNameValue.setLabelFor(auctionNameLabel);
            auctionWinPanel.add(auctionNameLabel);
            auctionWinPanel.add(auctionNameValue);

            // Create the price label
            JLabel auctionPriceLabel = new JLabel(" Price : ");
            JLabel auctionPriceValue = new JLabel("");
            auctionPriceValue.setText(Integer.toString(auction.getPrice()));
            auctionPriceValue.setLabelFor(auctionPriceLabel);
            auctionWinPanel.add(auctionPriceLabel);
            auctionWinPanel.add(auctionPriceValue);
        }

        showFrame();
    }

    public JPanel getAuctionPanel() {
        return auctionWinPanel;
    }

}
