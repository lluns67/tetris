package net.datasa.tetris.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import net.datasa.tetris.model.Game;

@Service
public class GameService {

    private final Map<String, Game> games = new ConcurrentHashMap<>();

    public Game createGame(String gameId) {
        Game game = new Game();
        games.put(gameId, game);
        return game;
    }

    public Game getGame(String gameId) {
        return games.get(gameId);
    }

    public Game moveLeft(String gameId) {
        Game game = games.get(gameId);
        if (game != null && !game.isGameOver()) {
            game.movePiece(-1, 0);
        }
        return game;
    }

    public Game moveRight(String gameId) {
        Game game = games.get(gameId);
        if (game != null && !game.isGameOver()) {
            game.movePiece(1, 0);
        }
        return game;
    }

    public Game rotate(String gameId) {
        Game game = games.get(gameId);
        if (game != null && !game.isGameOver()) {
            game.rotatePiece();
        }
        return game;
    }

    public Game drop(String gameId) {
        Game game = games.get(gameId);
        if (game != null && !game.isGameOver()) {
        	game.dropPiece();
        }
        return game;
    }
    
    public Game lower(String gameId) {
        Game game = games.get(gameId);
        if (game != null && !game.isGameOver()) {
            game.lowerPiece();
        }
        return game;
    }
}
