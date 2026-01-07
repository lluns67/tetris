package net.datasa.tetris.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Piece {
    // 7가지 테트로미노(I, O, T, S, Z, J, L)
    private static final List<int[][][]> SHAPES = Arrays.asList(
            // I
            new int[][][] {
                { { 0, 0, 0, 0 }, { 1, 1, 1, 1 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } },
                { { 0, 1, 0, 0 }, { 0, 1, 0, 0 }, { 0, 1, 0, 0 }, { 0, 1, 0, 0 } }
            },
            // O
            new int[][][] {
                { { 1, 1 }, { 1, 1 } }
            },
            // T
            new int[][][] {
                { { 0, 1, 0 }, { 1, 1, 1 }, { 0, 0, 0 } },
                { { 0, 1, 0 }, { 0, 1, 1 }, { 0, 1, 0 } },
                { { 0, 0, 0 }, { 1, 1, 1 }, { 0, 1, 0 } },
                { { 0, 1, 0 }, { 1, 1, 0 }, { 0, 1, 0 } }
            },
            // S
            new int[][][] {
                { { 0, 1, 1 }, { 1, 1, 0 }, { 0, 0, 0 } },
                { { 0, 1, 0 }, { 0, 1, 1 }, { 0, 0, 1 } }
            },
            // Z
            new int[][][] {
                { { 1, 1, 0 }, { 0, 1, 1 }, { 0, 0, 0 } },
                { { 0, 0, 1 }, { 0, 1, 1 }, { 0, 1, 0 } }
            },
            // J
            new int[][][] {
                { { 0, 1, 0 }, { 0, 1, 0 }, { 1, 1, 0 } },
                { { 1, 0, 0 }, { 1, 1, 1 }, { 0, 0, 0 } },
                { { 0, 1, 1 }, { 0, 1, 0 }, { 0, 1, 0 } },
                { { 0, 0, 0 }, { 1, 1, 1 }, { 0, 0, 1 } }
            },
            // L
            new int[][][] {
                { { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 1 } },
                { { 0, 0, 0 }, { 1, 1, 1 }, { 1, 0, 0 } },
                { { 1, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 } },
                { { 0, 0, 1 }, { 1, 1, 1 }, { 0, 0, 0 } }
            }
    );
    private static final List<String> TYPES = Arrays.asList("I", "O", "T", "S", "Z", "J", "L");
    private final int shapeIndex;
    private int rotation;
    private Point position;
    private final int[][][] rotations;

    private Piece(int shapeIndex) {
        this.shapeIndex = shapeIndex;
        this.rotations = SHAPES.get(this.shapeIndex);
        this.rotation = 0;
        // 블록 시작위치
        this.position = new Point(3, 0); 
    }
    
    public static Piece newPiece() {
        Random rnd = new Random();
        int shapeIndex = rnd.nextInt(SHAPES.size());
        return new Piece(shapeIndex);
    }
    
    public String getType() {
        return TYPES.get(this.shapeIndex);
    }

    public int[][] getShape() {
        return rotations[rotation];
    }

    public void rotate() {
        rotation = (rotation + 1) % rotations.length;
    }
    
    public void unRotate() {
    	rotation = (rotation + rotations.length - 1) % rotations.length;
    }

    public int getShapeIndex() {
        return shapeIndex;
    }

    public void move(int dx, int dy) {
        position.setX(position.getX() + dx);
        position.setY(position.getY() + dy);
    }
}
