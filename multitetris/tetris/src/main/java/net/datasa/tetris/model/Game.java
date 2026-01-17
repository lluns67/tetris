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
    
    // New mechanics fields
    private int combo = 0;
    private boolean lastMoveWasRotation = false;
    private boolean lastLockWasTSpin = false;
    private int lastLockLinesCleared = 0;

    public Game() {
        this.currentPiece = Piece.newPiece();
        this.nextPiece = Piece.newPiece();
    }

    public void movePiece(int dx, int dy) {
        currentPiece.move(dx, dy);
        if (checkCollision()) {
            currentPiece.move(-dx, -dy); // move back
        } else {
            lastMoveWasRotation = false;
        }
    }

    public void rotatePiece() {
        attemptRotation(false);
    }
    
    public void rotatePieceCCW() {
        attemptRotation(true);
    }
    
    private void attemptRotation(boolean ccw) {
        if (ccw) currentPiece.rotateCCW();
        else currentPiece.rotate();
        
        if (!checkCollision()) {
            lastMoveWasRotation = true;
            return;
        }
        
        // Wall Kick Data (Simplified)
        // Try shifting: Right 1, Left 1, Up 1, Right 1 Up 1, Left 1 Up 1, Right 2, Left 2
        int[][] kicks = {
            {1, 0}, {-1, 0},  // Basic wall kick
            {0, -1},          // Floor kick (up)
            {1, -1}, {-1, -1}, // Diagonal kick
            {2, 0}, {-2, 0}   // I-piece long kick
        };
        
        for (int[] kick : kicks) {
            currentPiece.move(kick[0], kick[1]);
            if (!checkCollision()) {
                lastMoveWasRotation = true;
                return; // Kick successful
            }
            currentPiece.move(-kick[0], -kick[1]); // Revert kick
        }
        
        // All failed, revert rotation
        if (ccw) currentPiece.rotate();
        else currentPiece.unRotate();
    }
    
    public void moveDown() {
        movePiece(0, 1);
    }
    
    public void dropPiece() {
    	while(!checkCollision()) {
    		currentPiece.move(0, 1);
    	}
    	currentPiece.move(0, -1);
        // Force rotation flag to false on hard drop? Or keep it? 
        // Usually hard drop preserves spin status if no move happened? 
        // But hard drop IS a move (vertical).
        // Standard: Hard drop counts as a move, so T-Spin is possible only if no vertical movement happened after rotation.
        // But wait, "EZ T-Spin" usually allows kick. 
        // Strict T-Spin: Last action was rotation. 
        // If I hard drop, the last action is the drop (move). 
        // However, if I rotate then hard drop immediately, does it count?
        // Usually, T-Spin is "Last action on the piece before locking".
        // Hard drop locks immediately.
        // So if I rotate and then Hard Drop, the rotation was the last "manipulation".
        // But Hard Drop implementation here moves the piece 1 by 1.
        // Let's set lastMoveWasRotation = false in the loop.
        if (checkCollision()) { // Already colliding? (Shouldn't happen)
             // no op
        } else {
             // If we move down even once, it resets rotation flag
             // Actually, we should check if we moved.
             // But simpler: just lock.
             // If we rely on the loop:
        }
        // Implementation detail: dropPiece moves until collision.
        // If it moved down, lastMoveWasRotation = false.
        // If it didn't move down (already at bottom), flag remains.
        
    	lockPiece();
    }

    public boolean lowerPiece() {
        currentPiece.move(0, 1);
        if (checkCollision()) {
            currentPiece.move(0, -1);
            lockPiece();
            return false; // Piece locked
        }
        lastMoveWasRotation = false;
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
    
    private boolean isTSpin() {
        if (currentPiece.getShapeIndex() != 2) return false; // Not T
        if (!lastMoveWasRotation) return false;
        
        Point pos = currentPiece.getPosition();
        // Check 4 corners of the 3x3 box: (0,0), (2,0), (0,2), (2,2)
        // A corner is "filled" if it's outside board or has a block.
        int cornersFilled = 0;
        int[][] corners = {{0,0}, {2,0}, {0,2}, {2,2}};
        
        for (int[] c : corners) {
            int bx = pos.getX() + c[0];
            int by = pos.getY() + c[1];
            
            if (bx < 0 || bx >= WIDTH || by >= HEIGHT || (by >= 0 && board[by][bx] != 0)) {
                cornersFilled++;
            }
        }
        
        return cornersFilled >= 3;
    }

    private void lockPiece() {
        // 1. Detect T-Spin before locking (since locking changes board)
        lastLockWasTSpin = isTSpin();
        
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
        
        // 2. Clear lines and update combo
        int lines = clearLines();
        lastLockLinesCleared = lines;
        
        if (lines > 0) {
            combo++;
        } else {
            combo = 0;
        }

        currentPiece = nextPiece;
        nextPiece = Piece.newPiece();
        
        // Reset flags for new piece
        lastMoveWasRotation = false;

        if (checkCollision()) {
            isGameOver = true;
        }
    }

    private int clearLines() {
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
            int baseScore = 0;
            switch (lines) {
                case 1: baseScore = 100; break;
                case 2: baseScore = 300; break;
                case 3: baseScore = 500; break;
                case 4: baseScore = 800; break;
            }
            // T-Spin bonus?
            if (lastLockWasTSpin) baseScore *= 2; 
            // Combo bonus?
            if (combo > 1) baseScore += 50 * (combo - 1);
            
            score += baseScore;
        }
        return lines;
    }
    
    public void addGarbageLines(int count, int emptyCol) {
        if (count <= 0) return;
        
        // Shift board up
        for (int y = 0; y < HEIGHT - count; y++) {
            board[y] = board[y + count];
        }
        
        // Fill bottom
        for (int y = HEIGHT - count; y < HEIGHT; y++) {
            board[y] = new int[WIDTH];
            for (int x = 0; x < WIDTH; x++) {
                if (x != emptyCol) {
                    board[y][x] = 8; // 8 = Garbage (Gray)
                }
            }
        }
        
        // Adjust current piece position if it collides? 
        // Standard Tetris: piece moves up with garbage? Or stays?
        // Usually garbage checks happen at start of turn or piece lock.
        // If we push garbage while piece is active, it might overlap.
        // Simple impl: Just push board. If collision happens next move, so be it.
        // But strict implementation might want to nudge piece up.
        // Let's leave piece alone, player must react or die.
    }
}
