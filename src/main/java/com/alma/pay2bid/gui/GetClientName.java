package com.alma.pay2bid.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

import static java.lang.System.exit;

public class GetClientName {

    /**
     * Main frame & elements
     */
    private JFrame mainFrame;
    private JPanel mainPanel;
    private JLabel mainLabel;
    private JTextField txt;
    private String[] options = {"OK", "EXIT"};

    public GetClientName(){

        // Create the Main JFrame
        mainFrame = new JFrame("Client connexion");
        mainPanel = new JPanel();

        mainLabel = new JLabel("Enter your name (should be longer than 1 character) : ");
        txt = new JTextField(10);
        mainPanel.add(mainLabel);
        mainPanel.add(txt);

        Dimension dimension = new Dimension(200, 200);
        mainFrame.setSize(200, 200);
        mainFrame.setMaximumSize(dimension);

        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent){
                exit(0);
            }
        });
    }

    public String getName(){

        // First block use to define the client
        boolean hasName = false;
        String clientName = null;
        while (!hasName) {

            int selectedOption = JOptionPane.showOptionDialog(null, mainPanel, "User connexion", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            if (selectedOption == 0) {
                clientName = txt.getText();
                if (clientName.length() > 1) {
                    hasName = true;
                    JOptionPane.showMessageDialog(mainFrame, "Welcome " + clientName + " !");
                }
            } else if(selectedOption == 1){
                hasName = true;
                clientName = null;
            } else {
                hasName = true;
                clientName = null;
            }
        }
        return clientName;
    }
}
