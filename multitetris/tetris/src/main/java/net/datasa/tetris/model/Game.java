package net.datasa.tetris.model;

import lombok.Getter;

@Getter
public class Game {
    private final int WIDTH = 10;
    private final int HEIGHT = 20;
    private int[][] board = new int[HEIGHT][WIDTH];
    private Piece currentPiece;
    private Piece nextPiece;
    private boolean isGameOver = false;
    private int score = 0;
    private int linesCleared = 0;

    public Game() {
        this.currentPiece = Piece.newPiece();
        this.nextPiece = Piece.newPiece();
    }

    public void movePiece(int dx, int dy) {
        currentPiece.move(dx, dy);
        if (checkCollision()) {
            currentPiece.move(-dx, -dy); // move back
        }
    }

    public void rotatePiece() {
        currentPiece.rotate();
        if (checkCollision()) {
            currentPiece.unRotate(); // rotate back
        }
    }
    
    public void dropPiece() {
    	while(!checkCollision()) {
    		currentPiece.move(0, 1);
    	}
    	currentPiece.move(0, -1);
    	lockPiece();
    }

    public boolean lowerPiece() {
        currentPiece.move(0, 1);
        if (checkCollision()) {
            currentPiece.move(0, -1);
            lockPiece();
            return false; // Piece locked
        }
        return true; // Piece moved down
    }

    private boolean checkCollision() {
        int[][] shape = currentPiece.getShape();
        Point position = currentPiece.getPosition();
        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != 0) {
                    int boardX = position.getX() + x;
                    int boardY = position.getY() + y;
                    if (boardY >= HEIGHT || boardX < 0 || boardX >= WIDTH || (boardY >= 0 && board[boardY][boardX] != 0)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void lockPiece() {
        int[][] shape = currentPiece.getShape();
        Point position = currentPiece.getPosition();
        int pieceTypeIndex = currentPiece.getShapeIndex() + 1;

        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] != 0) {
                    int boardX = position.getX() + x;
                    int boardY = position.getY() + y;
                    if(boardY >= 0) {
                    	board[boardY][boardX] = pieceTypeIndex;
                    }
                }
            }
        }
        clearLines();
        currentPiece = nextPiece;
        nextPiece = Piece.newPiece();
        if (checkCollision()) {
            isGameOver = true;
        }
    }

    private void clearLines() {
        int lines = 0;
        for (int y = HEIGHT - 1; y >= 0; y--) {
            boolean lineIsFull = true;
            for (int x = 0; x < WIDTH; x++) {
                if (board[y][x] == 0) {
                    lineIsFull = false;
                    break;
                }
            }
            if (lineIsFull) {
                lines++;
                for (int row = y; row > 0; row--) {
                    board[row] = board[row - 1];
                }
                board[0] = new int[WIDTH];
                y++; // check the same line again
            }
        }
        // Update score
        if (lines > 0) {
            linesCleared += lines;
            switch (lines) {
                case 1: score += 100; break;
                case 2: score += 300; break;
                case 3: score += 500; break;
                case 4: score += 800; break;
            }
        }
    }
}
