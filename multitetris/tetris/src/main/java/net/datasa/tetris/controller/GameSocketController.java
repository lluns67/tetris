package net.datasa.tetris.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datasa.tetris.model.Game;
import net.datasa.tetris.model.Room;
import net.datasa.tetris.service.GameService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GameSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate template;
    private final Map<String, ScheduledExecutorService> gameLoops = new ConcurrentHashMap<>();

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        log.debug("Session disconnected: {}", sessionId);
        
        Room room = gameService.getRoom(sessionId);
        if (room != null) {
            String roomId = room.getRoomId();
            
            // Stop game loop
            ScheduledExecutorService executor = gameLoops.remove(roomId);
            if (executor != null) {
                executor.shutdown();
            }
            
            gameService.removePlayer(sessionId);
            
            // Notify remaining players (if any)
            if (!room.isEmpty()) {
                template.convertAndSend("/topic/room/" + roomId, "PLAYER_LEFT");
            }
        }
    }

    @MessageMapping("/join")
    public void join(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.debug("Join request from: {}", sessionId);

        Room room = gameService.joinRoom(sessionId);
        
        // Notify the user about the room info
        template.convertAndSend("/topic/game/" + sessionId, room);

        // Notify everyone in the room about the new player
        template.convertAndSend("/topic/room/" + room.getRoomId(), room);
    }
    
    @MessageMapping("/ready")
    public void ready(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Room room = gameService.ready(sessionId);
        
        if (room == null) return;
        
        // Broadcast ready status
        template.convertAndSend("/topic/room/" + room.getRoomId(), room);
        
        // Check if all are ready and start if so
        if (!room.isPlaying() && room.isAllReady()) {
            startGameForRoom(room);
        }
    }

    private void startGameForRoom(Room room) {
        room.setPlaying(true);
        log.debug("Starting game for room: {}", room.getRoomId());
        
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        gameLoops.put(room.getRoomId(), executor);

        executor.scheduleAtFixedRate(() -> {
            boolean anyGameOver = false;
            for (String playerId : room.getGames().keySet()) {
                Game game = gameService.lower(playerId);
                if (game.isGameOver()) {
                    anyGameOver = true;
                }
            }
            
            // Broadcast room state
            template.convertAndSend("/topic/room/" + room.getRoomId(), room);

            if (anyGameOver) {
                log.debug("Game over for room: {}", room.getRoomId());
                executor.shutdown();
                gameLoops.remove(room.getRoomId());
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    @MessageMapping("/move")
    public void move(String direction, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Room room = gameService.getRoom(sessionId);
        
        if (room == null || !room.isPlaying()) return;
        
        switch (direction) {
            case "left":
                gameService.moveLeft(sessionId);
                break;
            case "right":
                gameService.moveRight(sessionId);
                break;
            case "rotate":
                gameService.rotate(sessionId);
                break;
            case "rotateCCW":
                gameService.rotateCCW(sessionId);
                break;
            case "down":
                gameService.moveDown(sessionId);
                break;
            case "drop":
            	gameService.drop(sessionId);
            	break;
            default:
                return;
        }
        template.convertAndSend("/topic/room/" + room.getRoomId(), room);
    }
}
