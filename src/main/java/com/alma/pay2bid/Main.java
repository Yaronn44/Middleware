package com.alma.pay2bid;

import com.alma.pay2bid.client.Client;
import com.alma.pay2bid.gui.ClientGui;
import com.alma.pay2bid.gui.GetClientName;
import com.alma.pay2bid.server.IServer;
import com.alma.pay2bid.server.Server;
import org.apache.commons.cli.*;

import java.rmi.ConnectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.logging.Logger;

/**
 * The Main application
 * @author Alexis Giraudet
 * @author Arnaud Grall
 * @author Thomas Minier
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());

    private static void startClient(String host, int port) {

        try {
            GetClientName getClientName = new GetClientName();
            String clientName = getClientName.getName();

            if(clientName == null){throw new Exception("Client exit without choosing a name...");}

            IServer server = (IServer) LocateRegistry.getRegistry(host, port).lookup("com.alma.pay2bid.server.Server");
            Client client = new Client(server, clientName);

            ClientGui c = new ClientGui(client, server);

            c.show();
        } catch (Exception  e) {
        	if(e instanceof ConnectException)
        		System.out.println("Server has not been launch !");
        	else
        		e.printStackTrace();
        }
    }

    private static void startServer(int port, boolean daemon) {
        try {
            String name = "com.alma.pay2bid.server.Server";
            IServer server = new Server();
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind(name, server);
            LOGGER.info("Server up and running at localhost on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = new Options();

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "pay2bid", options );

        options.addOption("l", "listen", false, "server port to listen");
        options.addOption("d", "daemon", false, "run the server as a daemon");
        options.addOption("h", "host", false, "host");
        options.addOption("p", "daemon", false, "port");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String host = "localhost";
        int port = 1099;
        boolean daemon = false;

        if(cmd.hasOption("d")) {
            daemon = true;
        }

        if(cmd.hasOption("h") && cmd.getOptionValue("h") != null) {
            host = cmd.getOptionValue("h");
        }

        if(cmd.hasOption("p") && cmd.getOptionValue("p") != null) {
            port = Integer.parseInt(cmd.getOptionValue("p"));
        }

        if(cmd.hasOption("l")) {
            // start the server
            startServer(port, daemon);
        } else {
        	// start the client
        	startClient(host, port);
        }
    }
}
