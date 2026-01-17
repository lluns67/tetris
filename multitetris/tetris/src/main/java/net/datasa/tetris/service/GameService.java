package net.datasa.tetris.service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import net.datasa.tetris.model.Game;
import net.datasa.tetris.model.Room;

@Service
public class GameService {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final Map<String, String> playerRoomMap = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public Room joinRoom(String sessionId) {
        // Try to find an available room
        for (Room room : rooms.values()) {
            if (!room.isFull() && !room.isPlaying()) {
                room.addPlayer(sessionId);
                playerRoomMap.put(sessionId, room.getRoomId());
                return room;
            }
        }

        // Create new room if none available
        String roomId = UUID.randomUUID().toString();
        Room newRoom = new Room(roomId);
        newRoom.addPlayer(sessionId);
        rooms.put(roomId, newRoom);
        playerRoomMap.put(sessionId, roomId);
        return newRoom;
    }
    
    public Room getRoom(String sessionId) {
    	String roomId = playerRoomMap.get(sessionId);
    	if (roomId == null) return null;
    	return rooms.get(roomId);
    }
    
    public Room ready(String sessionId) {
        Room room = getRoom(sessionId);
        if (room != null) {
            room.setReady(sessionId, true);
        }
        return room;
    }
    
    public void removePlayer(String sessionId) {
    	String roomId = playerRoomMap.remove(sessionId);
    	if (roomId != null) {
    		Room room = rooms.get(roomId);
    		if (room != null) {
    			room.removePlayer(sessionId);
    			if (room.isEmpty()) {
    				rooms.remove(roomId);
    			}
    		}
    	}
    }

    public Game getGame(String sessionId) {
        Room room = getRoom(sessionId);
        if (room == null) return null;
        return room.getGame(sessionId);
    }

    public Game moveLeft(String sessionId) {
        Game game = getGame(sessionId);
        if (game != null && !game.isGameOver()) {
            game.movePiece(-1, 0);
        }
        return game;
    }

    public Game moveRight(String sessionId) {
        Game game = getGame(sessionId);
        if (game != null && !game.isGameOver()) {
            game.movePiece(1, 0);
        }
        return game;
    }
    
    public Game moveDown(String sessionId) {
        Game game = getGame(sessionId);
        if (game != null && !game.isGameOver()) {
            game.moveDown();
        }
        return game;
    }

    public Game rotate(String sessionId) {
        Game game = getGame(sessionId);
        if (game != null && !game.isGameOver()) {
            game.rotatePiece();
        }
        return game;
    }
    
    public Game rotateCCW(String sessionId) {
        Game game = getGame(sessionId);
        if (game != null && !game.isGameOver()) {
            game.rotatePieceCCW();
        }
        return game;
    }

    public Game drop(String sessionId) {
        Game game = getGame(sessionId);
        if (game != null && !game.isGameOver()) {
        	game.dropPiece();
            checkAndSendGarbage(sessionId, game);
        }
        return game;
    }
    
    public Game lower(String sessionId) {
        Game game = getGame(sessionId);
        if (game != null && !game.isGameOver()) {
            boolean moved = game.lowerPiece();
            if (!moved) { // locked
                checkAndSendGarbage(sessionId, game);
            }
        }
        return game;
    }

    private void checkAndSendGarbage(String sessionId, Game game) {
        int lines = game.getLastLockLinesCleared();
        boolean tSpin = game.isLastLockWasTSpin();
        int combo = game.getCombo();
        
        int garbage = 0;
        
        // Rules
        if (lines == 3) garbage += 1;
        if (lines == 4) garbage += 3;
        
        if (tSpin) garbage += 1;
        
        if (combo >= 3) garbage += 1; // "3콤보 이상부터는" -> 3rd clear onwards sends +1
        
        if (garbage > 0) {
            Room room = getRoom(sessionId);
            if (room != null) {
                sendGarbage(room, sessionId, garbage);
            }
        }
    }
    
    private void sendGarbage(Room room, String senderId, int count) {
        int emptyCol = random.nextInt(10); // Common empty column for this batch
        
        for (Map.Entry<String, Game> entry : room.getGames().entrySet()) {
            if (!entry.getKey().equals(senderId)) {
                Game targetGame = entry.getValue();
                if (!targetGame.isGameOver()) {
                    targetGame.addGarbageLines(count, emptyCol);
                }
            }
        }
    }
}
