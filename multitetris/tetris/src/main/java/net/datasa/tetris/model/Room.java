package net.datasa.tetris.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class Room {
    private String roomId;
    private Map<String, Game> games = new ConcurrentHashMap<>();
    private Map<String, Boolean> readyStatus = new ConcurrentHashMap<>();
    private boolean isPlaying = false;

    public Room(String roomId) {
        this.roomId = roomId;
    }

    public void addPlayer(String sessionId) {
        games.put(sessionId, new Game());
        readyStatus.put(sessionId, false);
    }

    public void removePlayer(String sessionId) {
        games.remove(sessionId);
        readyStatus.remove(sessionId);
    }
    
    public void setReady(String sessionId, boolean ready) {
        readyStatus.put(sessionId, ready);
    }
    
    public boolean isAllReady() {
        if (games.size() < 2) return false;
        for (String sessionId : games.keySet()) {
            if (!readyStatus.getOrDefault(sessionId, false)) {
                return false;
            }
        }
        return true;
    }
    
    public Game getGame(String sessionId) {
    	return games.get(sessionId);
    }

    public boolean isFull() {
        return games.size() >= 6;
    }
    
    public boolean isEmpty() {
    	return games.isEmpty();
    }
}
