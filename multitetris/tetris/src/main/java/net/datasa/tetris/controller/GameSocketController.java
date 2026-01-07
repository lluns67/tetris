package net.datasa.tetris.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datasa.tetris.model.Game;
import net.datasa.tetris.service.GameService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Controller
@Slf4j
@RequiredArgsConstructor
public class GameSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate template;
    private final Map<String, ScheduledExecutorService> gameLoops = new HashMap<>();

    @MessageMapping("/start")
    public void startGame(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        log.debug("Game start request from: {}", sessionId);

        gameService.createGame(sessionId);
        
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        gameLoops.put(sessionId, executor);
        
        executor.scheduleAtFixedRate(() -> {
            Game currentGame = gameService.lower(sessionId);
            if (currentGame.isGameOver()) {
                log.debug("Game over for session: {}", sessionId);
                template.convertAndSend("/topic/game/" + sessionId, currentGame);
                executor.shutdown();
                gameLoops.remove(sessionId);
            } else {
                template.convertAndSend("/topic/game/" + sessionId, currentGame);
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS); // 1초마다 블록을 내림

    }

    @MessageMapping("/move")
    public void move(String direction, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Game game;
        switch (direction) {
            case "left":
                game = gameService.moveLeft(sessionId);
                break;
            case "right":
                game = gameService.moveRight(sessionId);
                break;

            case "rotate":
                game = gameService.rotate(sessionId);
                break;
            case "drop":
            	game = gameService.drop(sessionId);
            	break;
            default:
                return;
        }
        template.convertAndSend("/topic/game/" + sessionId, game);
    }
}
