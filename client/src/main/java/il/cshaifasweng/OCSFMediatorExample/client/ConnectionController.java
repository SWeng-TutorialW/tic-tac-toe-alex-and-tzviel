package il.cshaifasweng.OCSFMediatorExample.client;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class ConnectionController implements Initializable {

    @FXML
    private TextField hostTextField;

    @FXML
    private TextField portTextField;

    @FXML
    private Button connectButton;

    @FXML
    private Label statusLabel;

    private SimpleClient client;
    private char playerSymbol; // Store the player's symbol
    private boolean waitingForSecondPlayer = false;
    private boolean switchingToGameScreen = false; // Flag to prevent multiple screen switches

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("ConnectionController initialized");
        EventBus.getDefault().register(this);
        hostTextField.setText("localhost");
        portTextField.setText("3000");
    }

    @FXML
    void connect(ActionEvent event) {
        System.out.println("Connect button clicked");
        String host = hostTextField.getText();
        int port;
        
        try {
            port = Integer.parseInt(portTextField.getText());
        } catch (NumberFormatException e) {
            statusLabel.setText("Please enter a valid port number");
            System.out.println("Invalid port number entered");
            return;
        }

        System.out.println("Connecting to " + host + ":" + port);
        
        // Get the client instance
        client = SimpleClient.getClient();
        
        // Update host and port if different from default
        if (!host.equals("localhost") || port != 3000) {
            try {
                if (client.isConnected()) {
                    System.out.println("Closing existing connection");
                    client.closeConnection();
                }
            } catch (IOException e) {
                // Ignore if connection was not open
                System.out.println("Error closing connection: " + e.getMessage());
            }
            
            try {
                System.out.println("Creating client with custom host/port");
                client = SimpleClient.getClient(host, port);
            } catch (Exception e) {
                statusLabel.setText("Error creating client: " + e.getMessage());
                System.out.println("Error creating client: " + e.getMessage());
                return;
            }
        }

        try {
            System.out.println("Opening connection to server");
            client.openConnection();
            statusLabel.setText("Connected to server");
            
            // Send join game message
            GameMessage joinMessage = new GameMessage(GameMessage.MessageType.JOIN_GAME);
            System.out.println("Sending JOIN_GAME message");
            client.sendToServer(joinMessage);
            
            // Disable connect button after successful connection
            connectButton.setDisable(true);
            System.out.println("Connection successful");
            
        } catch (IOException e) {
            statusLabel.setText("Error connecting to server: " + e.getMessage());
            System.out.println("Connection error: " + e.getMessage());
        }
    }

    @Subscribe
    public void onGameEvent(GameEvent event) {
        GameMessage message = event.getGameMessage();
        System.out.println("ConnectionController received game event: " + message.getType());
        
        // If we're already switching to the game screen, ignore further messages
        if (switchingToGameScreen) {
            System.out.println("Already switching to game screen, ignoring message: " + message.getType());
            return;
        }
        
        Platform.runLater(() -> {
            switch (message.getType()) {
                case PLAYER_ASSIGNED:
                    // Store the player's symbol when it's assigned
                    playerSymbol = message.getPlayerSymbol();
                    System.out.println("Player assigned: " + playerSymbol);
                    statusLabel.setText("You are player " + playerSymbol + ". " + 
                        (message.getMessage() != null ? message.getMessage() : ""));
                    break;
                    
                case WAIT_FOR_PLAYER:
                    System.out.println("Waiting for player: " + message.getMessage());
                    statusLabel.setText(message.getMessage());
                    waitingForSecondPlayer = true;
                    break;
                    
                case BOARD_UPDATE:
                    System.out.println("Board update received, switching to game screen");
                    // Time to switch to the game screen
                    switchingToGameScreen = true; // Set flag to prevent multiple switches
                    openGameScreen(message);
                    break;
                    
                case PLAYER_TURN:
                    // If we get a PLAYER_TURN message while still on connection screen,
                    // it means the game is starting
                    if (!switchingToGameScreen) {
                        System.out.println("Game is starting, switching to game screen");
                        switchingToGameScreen = true; // Set flag to prevent multiple switches
                        openGameScreen(message);
                    }
                    break;
                    
                case GAME_JOINED:
                    if (!switchingToGameScreen) {
                        System.out.println("Game joined: " + message.getMessage());
                        statusLabel.setText(message.getMessage());
                    }
                    break;
                    
                default:
                    System.out.println("Unhandled message type in ConnectionController: " + message.getType());
                    break;
            }
        });
    }

    private void openGameScreen(GameMessage message) {
        try {
            System.out.println("Loading GameScreen.fxml");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/il/cshaifasweng/OCSFMediatorExample/client/GameScreen.fxml"));
            Parent root = loader.load();
            
            GameScreenController controller = loader.getController();
            // Pass the player symbol to the game controller
            controller.setPlayerSymbol(playerSymbol);
            System.out.println("Setting player symbol to GameScreenController: " + playerSymbol);
            
            // Also pass the game message
            System.out.println("Setting game message to GameScreenController");
            controller.setGameMessage(message);
            
            Scene scene = new Scene(root);
            Stage stage = (Stage) connectButton.getScene().getWindow();
            if (stage != null) {
                System.out.println("Switching scene to game screen");
                stage.setScene(scene);
                stage.setTitle("Tic-Tac-Toe Game - Player " + playerSymbol);
                stage.show();
                
                // Unregister from EventBus since we're switching controllers
                System.out.println("Unregistering ConnectionController from EventBus");
                EventBus.getDefault().unregister(this);
            } else {
                System.out.println("ERROR: Stage is null, cannot switch scenes");
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Error loading game screen: " + e.getMessage());
            System.out.println("Error loading game screen: " + e.getMessage());
        }
    }
}
