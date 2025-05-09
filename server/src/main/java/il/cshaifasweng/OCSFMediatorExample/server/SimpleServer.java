package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.ArrayList;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

public class SimpleServer extends AbstractServer {
	private static ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();
	private GameManager gameManager;

	public SimpleServer(int port) {
		super(port);
		gameManager = GameManager.getInstance();
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		if (msg instanceof GameMessage) {
			GameMessage gameMsg = (GameMessage) msg;
			
			switch (gameMsg.getType()) {
				case JOIN_GAME:
					gameManager.handleJoinGame(client);
					break;
				case MOVE:
					gameManager.handleMove(gameMsg, client);
					break;
				case RESTART_GAME:
					gameManager.handleRestartGame(client);
					break;
				default:
					System.out.println("Unknown game message type: " + gameMsg.getType());
			}
			return;
		}
		
		String msgString = msg.toString();
		if (msgString.startsWith("#warning")) {
			Warning warning = new Warning("Warning from server!");
			try {
				client.sendToClient(warning);
				System.out.format("Sent warning to client %s\n", client.getInetAddress().getHostAddress());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else if(msgString.startsWith("add client")){
			SubscribedClient connection = new SubscribedClient(client);
			SubscribersList.add(connection);
			try {
				client.sendToClient("client added successfully");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		else if(msgString.startsWith("remove client")){
			if(!SubscribersList.isEmpty()){
				for(SubscribedClient subscribedClient: SubscribersList){
					if(subscribedClient.getClient().equals(client)){
						SubscribersList.remove(subscribedClient);
						break;
					}
				}
			}
		}
	}
	
	@Override
	protected void clientDisconnected(ConnectionToClient client) {
		super.clientDisconnected(client);
		gameManager.handleClientDisconnected(client);
		
		// Remove from subscribers list if present
		if (!SubscribersList.isEmpty()) {
			SubscribersList.removeIf(subscribedClient -> subscribedClient.getClient().equals(client));
		}
	}
	
	public void sendToAllClients(String message) {
		try {
			for (SubscribedClient subscribedClient : SubscribersList) {
				subscribedClient.getClient().sendToClient(message);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}
