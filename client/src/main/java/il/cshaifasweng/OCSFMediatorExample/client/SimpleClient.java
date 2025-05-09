package il.cshaifasweng.OCSFMediatorExample.client;

import org.greenrobot.eventbus.EventBus;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;

import java.io.IOException;

public class SimpleClient extends AbstractClient {
	
	private static SimpleClient client = null;
	private static String host = "localhost";
	private static int port = 3000;

	private SimpleClient(String host, int port) {
		super(host, port);
		System.out.println("SimpleClient created with host: " + host + ", port: " + port);
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		if (msg instanceof Warning) {
			System.out.println("Received Warning message from server");
			EventBus.getDefault().post(new WarningEvent((Warning) msg));
		} else if (msg instanceof GameMessage) {
			GameMessage gameMsg = (GameMessage) msg;
			System.out.println("Received GameMessage from server: " + gameMsg.getType());
			EventBus.getDefault().post(new GameEvent(gameMsg));
		} else {
			String message = msg.toString();
			System.out.println("Received string message from server: " + message);
		}
	}
	
	@Override
	public void sendToServer(Object msg) throws IOException {
		if (msg instanceof GameMessage) {
			GameMessage gameMsg = (GameMessage) msg;
			System.out.println("Sending GameMessage to server: " + gameMsg.getType());
		} else {
			System.out.println("Sending message to server: " + msg);
		}
		super.sendToServer(msg);
	}
	
	@Override
	protected void connectionEstablished() {
		super.connectionEstablished();
		System.out.println("Connection established with server: " + getHost() + ":" + getPort());
	}
	
	@Override
	protected void connectionClosed() {
		super.connectionClosed();
		System.out.println("Connection closed with server");
	}
	
	@Override
	protected void connectionException(Exception exception) {
		super.connectionException(exception);
		System.out.println("Connection exception: " + exception.getMessage());
	}
	
	/**
	 * Get the client instance with default host and port
	 * @return SimpleClient instance
	 */
	public static SimpleClient getClient() {
		if (client == null) {
			client = new SimpleClient(host, port);
			System.out.println("Created new SimpleClient with default settings");
		}
		return client;
	}
	
	/**
	 * Get the client instance with specified host and port
	 * @param newHost the host to connect to
	 * @param newPort the port to connect to
	 * @return SimpleClient instance
	 */
	public static SimpleClient getClient(String newHost, int newPort) {
		// If client exists but with different host/port, create a new one
		if (client != null && (!host.equals(newHost) || port != newPort)) {
			try {
				if (client.isConnected()) {
					System.out.println("Closing existing connection before creating new client");
					client.closeConnection();
				}
			} catch (Exception e) {
				// Ignore if already closed
				System.out.println("Error closing connection: " + e.getMessage());
			}
			client = null;
		}
		
		// Update static host and port
		host = newHost;
		port = newPort;
		System.out.println("Updated client settings to host: " + host + ", port: " + port);
		
		// Create new client if needed
		if (client == null) {
			client = new SimpleClient(host, port);
			System.out.println("Created new SimpleClient with custom settings");
		}
		
		return client;
	}
}
