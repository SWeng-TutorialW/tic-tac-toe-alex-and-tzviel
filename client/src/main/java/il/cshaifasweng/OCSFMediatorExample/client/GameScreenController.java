package il.cshaifasweng.OCSFMediatorExample.client;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import il.cshaifasweng.OCSFMediatorExample.entities.GameBoard;
import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class GameScreenController implements Initializable {

    @FXML
    private GridPane gameBoard;

    @FXML
    private Label statusLabel;

    @FXML
    private Button restartButton;

    private SimpleClient client;
    private char playerSymbol; // 'X' or 'O'
    private boolean myTurn = false;
    private Button[][] boardButtons = new Button[3][3];

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("GameScreenController initialized");
        EventBus.getDefault().register(this);
        client = SimpleClient.getClient();
        
        // Initialize the game board with buttons
        initializeGameBoard();
        
        // Initially disable restart button
        restartButton.setDisable(true);
        
        System.out.println("Game board initialized with " + (3*3) + " buttons");
    }

    private void initializeGameBoard() {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Button button = createGameButton(row, col);
                boardButtons[row][col] = button;
                gameBoard.add(button, col, row);
                System.out.println("Added button at position [" + row + "," + col + "]");
            }
        }
    }

    private Button createGameButton(final int row, final int col) {
        Button button = new Button();
        button.setMinSize(100, 100);
        button.setPrefSize(100, 100);
        button.setFont(Font.font(36));
        button.setText(""); // Ensure button starts empty
        
        button.setOnAction(event -> handleButtonClick(row, col));
        
        return button;
    }

    /**
     * Set the player's symbol directly (called from ConnectionController)
     */
    public void setPlayerSymbol(char symbol) {
        this.playerSymbol = symbol;
        System.out.println("Player symbol set to: " + playerSymbol);
        updateStatus("You are playing as " + playerSymbol);
        
        // Check if it's this player's turn to start
        if (playerSymbol == 'X') {
            // X typically goes first
            myTurn = true;
            updateStatus("Your turn");
            System.out.println("X goes first, setting myTurn to true");
        }
    }

    public void setGameMessage(GameMessage message) {
        System.out.println("setGameMessage called with type: " + message.getType());
        
        // Handle different message types
        switch (message.getType()) {
            case PLAYER_ASSIGNED:
                // This should have been handled by ConnectionController already
                // but we'll handle it here too just in case
                if (playerSymbol == 0) { // Only set if not already set
                    playerSymbol = message.getPlayerSymbol();
                    System.out.println("Player assigned symbol: " + playerSymbol);
                    updateStatus("You are playing as " + playerSymbol);
                    
                    // Check if it's this player's turn to start
                    if (playerSymbol == 'X') {
                        // X typically goes first
                        myTurn = true;
                        updateStatus("Your turn");
                        System.out.println("X goes first, setting myTurn to true");
                    }
                }
                break;
                
            case BOARD_UPDATE:
                if (message.getGameBoard() != null) {
                    updateBoard(message.getGameBoard());
                    System.out.println("Board updated from setGameMessage");
                    
                    // Update turn state based on the current turn in the message
                    char currentTurn = message.getPlayerSymbol();
                    myTurn = (currentTurn == playerSymbol);
                    
                    if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                        updateStatus(message.getMessage());
                    } else {
                        updateStatus(myTurn ? "Your turn" : "Opponent's turn");
                    }
                    
                    System.out.println("Turn updated - currentTurn: " + currentTurn + 
                                   ", playerSymbol: " + playerSymbol + 
                                   ", myTurn: " + myTurn);
                }
                break;
                
            case PLAYER_TURN:
                handlePlayerTurn(message);
                break;
                
            default:
                System.out.println("Unhandled message type in setGameMessage: " + message.getType());
                break;
        }
    }

    @Subscribe
    public void onGameEvent(GameEvent event) {
        GameMessage message = event.getGameMessage();
        System.out.println("Received game event: " + message.getType());
        
        Platform.runLater(() -> {
            switch (message.getType()) {
                case PLAYER_ASSIGNED:
                    handlePlayerAssigned(message);
                    break;
                    
                case BOARD_UPDATE:
                    handleBoardUpdate(message);
                    break;
                    
                case PLAYER_TURN:
                    handlePlayerTurn(message);
                    break;
                    
                case GAME_OVER:
                    handleGameOver(message);
                    break;
                    
                case WAIT_FOR_PLAYER:
                    updateStatus(message.getMessage());
                    System.out.println("Waiting for player: " + message.getMessage());
                    break;
                    
                default:
                    System.out.println("Unhandled message type: " + message.getType());
                    break;
            }
        });
    }
    
    private void handlePlayerAssigned(GameMessage message) {
        if (playerSymbol == 0) { // Only set if not already set
            playerSymbol = message.getPlayerSymbol();
            System.out.println("Player assigned symbol: " + playerSymbol);
            updateStatus("You are playing as " + playerSymbol);
            
            // Check if it's this player's turn to start
            if (playerSymbol == 'X') {
                myTurn = true;
                updateStatus("Your turn");
                System.out.println("X goes first, setting myTurn to true");
            }
        }
    }
    
    private void handleBoardUpdate(GameMessage message) {
        // Update the board state
        if (message.getGameBoard() != null) {
            updateBoard(message.getGameBoard());
            System.out.println("Board updated");
            
            // Update turn state based on the current turn in the message
            if (message.getPlayerSymbol() != 0) {
                char currentTurn = message.getPlayerSymbol();
                myTurn = (currentTurn == playerSymbol);
                
                // Update status message if provided
                if (message.getMessage() != null && !message.getMessage().isEmpty()) {
                    updateStatus(message.getMessage());
                } else {
                    updateStatus(myTurn ? "Your turn" : "Opponent's turn");
                }
                
                System.out.println("Turn updated - currentTurn: " + currentTurn + 
                                ", playerSymbol: " + playerSymbol + 
                                ", myTurn: " + myTurn);
            }
        }
    }
    
    private void handleGameOver(GameMessage message) {
        if (message.getGameBoard() != null) {
            updateBoard(message.getGameBoard());
        }
        updateStatus(message.getMessage());
        myTurn = false;
        restartButton.setDisable(false);
        System.out.println("Game over: " + message.getMessage());
    }

    private void handlePlayerTurn(GameMessage message) {
        char turnSymbol = message.getPlayerSymbol();
        System.out.println("Player turn message received for: " + turnSymbol + ", my symbol is: " + playerSymbol);
        
        if (turnSymbol == playerSymbol) {
            myTurn = true;
            updateStatus("Your turn");
            System.out.println("It's now my turn as player " + playerSymbol);
        } else {
            myTurn = false;
            updateStatus("Opponent's turn");
            System.out.println("It's opponent's turn");
        }
    }

    private void updateBoard(GameBoard board) {
        if (board == null) {
            System.out.println("Warning: updateBoard called with null board");
            return;
        }
        
        char[][] boardState = board.getBoard();
        System.out.println("Updating board UI with current state:");
        
        // Print current board state for debugging
        for (int i = 0; i < 3; i++) {
            StringBuilder row = new StringBuilder();
            for (int j = 0; j < 3; j++) {
                row.append(boardState[i][j] == ' ' ? '_' : boardState[i][j]).append(" ");
            }
            System.out.println(row.toString());
        }
        
        // Update UI
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final char cell = boardState[row][col];
                final Button button = boardButtons[row][col];
                
                Platform.runLater(() -> {
                    if (cell == 'X' || cell == 'O') {
                        button.setText(String.valueOf(cell));
                        System.out.println("Set button to '" + cell + "'");
                    } else {
                        button.setText("");
                        System.out.println("Cleared button");
                    }
                });
            }
        }
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        System.out.println("Status updated: " + message);
    }

    private void handleButtonClick(int row, int col) {
        System.out.println("Button clicked at [" + row + "," + col + "]");
        System.out.println("myTurn: " + myTurn + ", isEmpty: " + boardButtons[row][col].getText().isEmpty() + ", playerSymbol: " + playerSymbol);
        
        if (myTurn && boardButtons[row][col].getText().isEmpty()) {
            try {
                // Send move to server
                GameMessage moveMessage = new GameMessage(GameMessage.MessageType.MOVE);
                moveMessage.setRow(row);
                moveMessage.setCol(col);
                moveMessage.setPlayerSymbol(playerSymbol);
                client.sendToServer(moveMessage);
                System.out.println("Sent move to server: [" + row + "," + col + "] as player " + playerSymbol);
                
                // Update UI immediately to show the move
                boardButtons[row][col].setText(String.valueOf(playerSymbol));
                
                // Disable further moves until server confirms
                myTurn = false;
                updateStatus("Waiting for opponent...");
                
            } catch (IOException e) {
                e.printStackTrace();
                updateStatus("Error sending move: " + e.getMessage());
            }
        } else {
            if (!myTurn) {
                updateStatus("Not your turn!");
                System.out.println("Not player's turn, cannot make move");
            }
        }
    }

    @FXML
    void restartGame(ActionEvent event) {
        try {
            GameMessage restartMessage = new GameMessage(GameMessage.MessageType.RESTART_GAME);
            client.sendToServer(restartMessage);
            restartButton.setDisable(true);
            updateStatus("Requesting new game...");
            System.out.println("Restart game requested");
        } catch (IOException e) {
            e.printStackTrace();
            updateStatus("Error restarting game: " + e.getMessage());
        }
    }
}
