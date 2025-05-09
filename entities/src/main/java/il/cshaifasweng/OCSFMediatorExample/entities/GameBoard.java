package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class GameBoard implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private char[][] board;
    private char currentPlayer;
    private boolean gameOver;
    private char winner; // 'X', 'O', or 'T' for tie, ' ' for no winner yet
    
    public GameBoard() {
        board = new char[3][3];
        // Initialize the board with empty spaces
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
        gameOver = false;
        winner = ' ';
    }
    
    public boolean makeMove(int row, int col, char player) {
        if (row < 0 || row > 2 || col < 0 || col > 2 || board[row][col] != ' ' || gameOver) {
            return false; // Invalid move
        }
        
        board[row][col] = player;
        checkGameStatus();
        return true;
    }
    
    private void checkGameStatus() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != ' ' && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                gameOver = true;
                winner = board[i][0];
                return;
            }
        }
        
        // Check columns
        for (int i = 0; i < 3; i++) {
            if (board[0][i] != ' ' && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                gameOver = true;
                winner = board[0][i];
                return;
            }
        }
        
        // Check diagonals
        if (board[0][0] != ' ' && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            gameOver = true;
            winner = board[0][0];
            return;
        }
        
        if (board[0][2] != ' ' && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            gameOver = true;
            winner = board[0][2];
            return;
        }
        
        // Check for tie
        boolean isFull = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    isFull = false;
                    break;
                }
            }
        }
        
        if (isFull) {
            gameOver = true;
            winner = 'T'; // Tie
        }
    }
    
    public char[][] getBoard() {
        return board;
    }
    
    public char getCurrentPlayer() {
        return currentPlayer;
    }
    
    public void setCurrentPlayer(char currentPlayer) {
        this.currentPlayer = currentPlayer;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
    
    public char getWinner() {
        return winner;
    }
    
    public void reset() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
        gameOver = false;
        winner = ' ';
    }
}
