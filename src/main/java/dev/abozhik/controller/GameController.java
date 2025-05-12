package dev.abozhik.controller;

import dev.abozhik.exception.GameException;
import dev.abozhik.model.MoveRequest;
import dev.abozhik.model.entity.Game;
import dev.abozhik.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@Tag(name = "Game Controller", description = "APIs for managing Tic Tac Toe games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @Operation(summary = "List all games", description = "Returns a list of all games")
    @GetMapping
    public ResponseEntity<List<Game>> listGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @Operation(summary = "Create a new game", description = "Creates a new Tic Tac Toe game and returns the game object")
    @PostMapping("/new")
    public ResponseEntity<Game> createGame() {
        return ResponseEntity.ok(gameService.createGame());
    }

    @Operation(summary = "Get game", description = "Returns the game object for a specific game")
    @GetMapping("/{id}")
    public ResponseEntity<Game> getGame(
            @Parameter(description = "ID of the game to retrieve", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(gameService.getGame(id));
    }

    @Operation(summary = "Make a move", description = "Makes a move in the specified game at the given position")
    @PostMapping("/{id}/move")
    public ResponseEntity<Game> makeMove(
            @Parameter(description = "ID of the game to make a move in", example = "1") @PathVariable Long id,
            @Parameter(description = "Move request containing position, player, and request ID") @RequestBody MoveRequest moveRequest) {
        if (moveRequest == null) {
            throw new GameException("Move request cannot be null");
        }
        if (moveRequest.getPosition() < 0 || moveRequest.getPosition() > 8) {
            throw new GameException("Invalid position. Position must be between 0 and 8");
        }
        Game game = gameService.makeMove(id, moveRequest.getPosition(), moveRequest.getPlayer(), moveRequest.getRequestId());
        return ResponseEntity.ok(game);
    }
} 