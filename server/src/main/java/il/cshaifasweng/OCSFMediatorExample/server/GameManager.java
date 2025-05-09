package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.GameBoard;
import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GameManager {
    private static GameManager instance = null;
    private GameBoard gameBoard;
    private ConnectionToClient playerX;
    private ConnectionToClient playerO;
    private char currentTurn; // 'X' or 'O'
    private boolean gameInProgress;
    private final Random random = new Random();
    
    private GameManager() {
        gameBoard = new GameBoard();
        gameInProgress = false;
    }
    
    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }
    
    public synchronized void handleJoinGame(ConnectionToClient client) {
        System.out.println("Client joined game: " + client.getInetAddress());
        
        // If game is already in progress with two players
        if (gameInProgress && playerX != null && playerO != null) {
            try {
                GameMessage message = new GameMessage(GameMessage.MessageType.GAME_JOINED);
                message.setMessage("Game is already in progress. Please try again later.");
                client.sendToClient(message);
                System.out.println("Game is full, rejected player");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        // If there's no game in progress and no players waiting
        if (!gameInProgress && playerX == null && playerO == null) {
            // Assign first player to X
            playerX = client;
            sendPlayerAssigned(client, 'X');
            System.out.println("Assigned player X");
            
            // Tell player to wait for another player
            sendWaitForPlayer(client);
        }
        // If there's one player waiting
        else if (!gameInProgress && (playerX == null || playerO == null)) {
            // Assign second player to O
            if (playerX == null) {
                playerX = client;
                sendPlayerAssigned(client, 'X');
                System.out.println("Assigned player X");
            } else {
                playerO = client;
                sendPlayerAssigned(client, 'O');
                System.out.println("Assigned player O");
            }
            
            // Start the game
            startGame();
        }
        // If client is reconnecting or requesting current state
        else if (gameInProgress && (client == playerX || client == playerO)) {
            // Send current game state to the reconnecting player
            char symbol = (client == playerX) ? 'X' : 'O';
            sendPlayerAssigned(client, symbol);
            
            // Send current board state
            GameMessage boardMessage = new GameMessage(GameMessage.MessageType.BOARD_UPDATE);
            boardMessage.setGameBoard(gameBoard);
            boardMessage.setPlayerSymbol(currentTurn);
            boardMessage.setMessage(currentTurn == symbol ? "Your turn" : "Opponent's turn");
            
            try {
                client.sendToClient(boardMessage);
                System.out.println("Sent current board state to reconnecting player");
                
                // Also send a PLAYER_TURN message to ensure proper turn state
                GameMessage turnMessage = new GameMessage(GameMessage.MessageType.PLAYER_TURN);
                turnMessage.setPlayerSymbol(currentTurn);
                turnMessage.setMessage(currentTurn == symbol ? "Your turn" : "Opponent's turn");
                client.sendToClient(turnMessage);
                System.out.println("Sent turn notification to reconnecting player");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void sendPlayerAssigned(ConnectionToClient client, char symbol) {
        try {
            GameMessage message = new GameMessage(GameMessage.MessageType.PLAYER_ASSIGNED);
            message.setPlayerSymbol(symbol);
            message.setMessage("You have been assigned as player " + symbol);
            client.sendToClient(message);
            System.out.println("Sent PLAYER_ASSIGNED message to client: " + symbol);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendWaitForPlayer(ConnectionToClient client) {
        try {
            GameMessage message = new GameMessage(GameMessage.MessageType.WAIT_FOR_PLAYER);
            message.setMessage("Waiting for another player to join...");
            client.sendToClient(message);
            System.out.println("Sent WAIT_FOR_PLAYER message to client");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void startGame() {
        gameBoard = new GameBoard();
        gameInProgress = true;
        
        // X always goes first
        currentTurn = 'X';
        gameBoard.setCurrentPlayer(currentTurn);
        System.out.println("Game started. X goes first");
        
        // Send initial board state to both players
        updateBoardForBothPlayers();
        
        // Notify whose turn it is
        notifyPlayerTurn();
    }
    
    public synchronized void handleMove(GameMessage message, ConnectionToClient client) {
        System.out.println("Received move from client: [" + message.getRow() + "," + message.getCol() + "] as " + message.getPlayerSymbol());
        
        if (!gameInProgress) {
            System.out.println("Game not in progress, ignoring move");
            return;
        }
        
        char playerSymbol = message.getPlayerSymbol();
        
        // Verify it's the correct client for this symbol
        if ((playerSymbol == 'X' && client != playerX) || (playerSymbol == 'O' && client != playerO)) {
            System.out.println("Not the correct client for this symbol, ignoring move");
            sendErrorMessage(client, "Not your turn!");
            return;
        }
        
        // Verify it's this player's turn
        if (playerSymbol != currentTurn) {
            System.out.println("Not player's turn, sending error");
            sendErrorMessage(client, "Not your turn!");
            return;
        }
        
        // Try to make the move
        int row = message.getRow();
        int col = message.getCol();
        
        if (gameBoard.makeMove(row, col, playerSymbol)) {
            System.out.println("Valid move made: [" + row + "," + col + "] by " + playerSymbol);
            
            // Switch turns
            currentTurn = (currentTurn == 'X') ? 'O' : 'X';
            gameBoard.setCurrentPlayer(currentTurn);
            
            // Check if game is over
            if (gameBoard.isGameOver()) {
                gameInProgress = false;
                sendGameOverMessage();
                System.out.println("Game over. Winner: " + gameBoard.getWinner());
            } else {
                // Send board update to both players
                updateBoardForBothPlayers();
                
                // Notify whose turn it is now
                notifyPlayerTurn();
            }
        } else {
            // Invalid move
            System.out.println("Invalid move, sending error");
            sendErrorMessage(client, "Invalid move!");
        }
    }
    
    private void sendErrorMessage(ConnectionToClient client, String message) {
        try {
            GameMessage response = new GameMessage(GameMessage.MessageType.BOARD_UPDATE);
            response.setMessage(message);
            response.setGameBoard(gameBoard);
            response.setPlayerSymbol(currentTurn);
            client.sendToClient(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void updateBoardForBothPlayers() {
        GameMessage message = new GameMessage(GameMessage.MessageType.BOARD_UPDATE);
        message.setGameBoard(gameBoard);
        message.setPlayerSymbol(currentTurn);  // Include current turn in board update
        System.out.println("Sending updated board to both players. Current turn: " + currentTurn);
        
        try {
            if (playerX != null) {
                message.setMessage(currentTurn == 'X' ? "Your turn" : "Opponent's turn");
                playerX.sendToClient(message);
                System.out.println("Sent board update to X: " + message.getMessage());
            }
            if (playerO != null) {
                message.setMessage(currentTurn == 'O' ? "Your turn" : "Opponent's turn");
                playerO.sendToClient(message);
                System.out.println("Sent board update to O: " + message.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void notifyPlayerTurn() {
        try {
            GameMessage xMessage = new GameMessage(GameMessage.MessageType.PLAYER_TURN);
            xMessage.setPlayerSymbol(currentTurn);
            xMessage.setMessage(currentTurn == 'X' ? "Your turn" : "Opponent's turn");
            
            GameMessage oMessage = new GameMessage(GameMessage.MessageType.PLAYER_TURN);
            oMessage.setPlayerSymbol(currentTurn);
            oMessage.setMessage(currentTurn == 'O' ? "Your turn" : "Opponent's turn");
            
            if (playerX != null) {
                playerX.sendToClient(xMessage);
                System.out.println("Notified player X: " + xMessage.getMessage());
            }
            if (playerO != null) {
                playerO.sendToClient(oMessage);
                System.out.println("Notified player O: " + oMessage.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendGameOverMessage() {
        GameMessage message = new GameMessage(GameMessage.MessageType.GAME_OVER);
        message.setGameBoard(gameBoard);
        
        char winner = gameBoard.getWinner();
        if (winner == 'T') {
            message.setMessage("Game over! It's a tie!");
        } else {
            message.setMessage("Game over! Player " + winner + " wins!");
        }
        
        try {
            if (playerX != null) {
                playerX.sendToClient(message);
            }
            if (playerO != null) {
                playerO.sendToClient(message);
            }
            System.out.println("Sent game over message to both players");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public synchronized void handleRestartGame(ConnectionToClient client) {
        // Only allow restart if the game is over
        if (!gameInProgress && playerX != null && playerO != null) {
            startGame();
            System.out.println("Game restarted");
        }
    }
    
    public synchronized void handleClientDisconnected(ConnectionToClient client) {
        if (client == playerX) {
            playerX = null;
            notifyOpponentDisconnected(playerO);
            System.out.println("Player X disconnected");
        } else if (client == playerO) {
            playerO = null;
            notifyOpponentDisconnected(playerX);
            System.out.println("Player O disconnected");
        }
        
        // Reset game if a player disconnects
        if (gameInProgress) {
            gameInProgress = false;
            gameBoard = new GameBoard();
            System.out.println("Game reset due to player disconnection");
        }
    }
    
    private void notifyOpponentDisconnected(ConnectionToClient client) {
        if (client != null) {
            try {
                GameMessage message = new GameMessage(GameMessage.MessageType.GAME_OVER);
                message.setMessage("Your opponent has disconnected. Game over.");
                client.sendToClient(message);
                System.out.println("Notified remaining player about disconnection");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
