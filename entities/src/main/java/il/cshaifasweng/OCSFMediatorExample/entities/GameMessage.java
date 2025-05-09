package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class GameMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum MessageType {
        JOIN_GAME,        // Client wants to join a game
        GAME_JOINED,     // Server confirms client joined a game
        PLAYER_ASSIGNED, // Server assigns X or O to a player
        MOVE,            // Client sends a move
        BOARD_UPDATE,    // Server sends updated board
        GAME_OVER,       // Game is over
        WAIT_FOR_PLAYER, // Wait for another player to join
        PLAYER_TURN,     // Indicates whose turn it is
        RESTART_GAME     // Request to restart the game
    }
    
    private MessageType type;
    private GameBoard gameBoard;
    private int row;
    private int col;
    private char playerSymbol; // 'X' or 'O'
    private String message;
    
    public GameMessage(MessageType type) {
        this.type = type;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public GameBoard getGameBoard() {
        return gameBoard;
    }
    
    public void setGameBoard(GameBoard gameBoard) {
        this.gameBoard = gameBoard;
    }
    
    public int getRow() {
        return row;
    }
    
    public void setRow(int row) {
        this.row = row;
    }
    
    public int getCol() {
        return col;
    }
    
    public void setCol(int col) {
        this.col = col;
    }
    
    public char getPlayerSymbol() {
        return playerSymbol;
    }
    
    public void setPlayerSymbol(char playerSymbol) {
        this.playerSymbol = playerSymbol;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
