package com.alma.pay2bid.gui.listeners;

import com.alma.pay2bid.gui.ClientGui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AuctionWinListener implements ActionListener {
    private ClientGui gui;

    public AuctionWinListener(ClientGui gui) {
        this.gui = gui;
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        String command = actionEvent.getActionCommand();
        if("winAuctions".equals(command))  {
            gui.winAuctionsView();
        }
    }
}