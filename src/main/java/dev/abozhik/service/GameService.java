package dev.abozhik.service;

import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.abozhik.exception.GameException;
import dev.abozhik.model.GameMessage;
import dev.abozhik.model.GamePlayer;
import dev.abozhik.model.GameStatus;
import dev.abozhik.model.constants.GameConstants;
import dev.abozhik.model.entity.Game;
import dev.abozhik.model.entity.ProcessedRequest;
import dev.abozhik.repository.GameRepository;
import dev.abozhik.repository.ProcessedRequestRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
public class GameService {

    private final JsonMapper jsonMapper;
    private final GameRepository gameRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProcessedRequestRepository processedRequestRepository;

    public GameService(JsonMapper jsonMapper,
                       GameRepository gameRepository,
                       SimpMessagingTemplate messagingTemplate,
                       ProcessedRequestRepository processedRequestRepository) {
        this.jsonMapper = jsonMapper;
        this.gameRepository = gameRepository;
        this.messagingTemplate = messagingTemplate;
        this.processedRequestRepository = processedRequestRepository;
    }

    @Transactional
    public Game createGame() {
        Game game = gameRepository.save(new Game(GameStatus.IN_PROGRESS, GameConstants.EMPTY_BOARD, GamePlayer.X));
        publishGameUpdate(game);
        return game;
    }

    @SneakyThrows
    @Transactional
    @Retryable(
            retryFor = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
            maxAttemptsExpression = "${game.move.retry.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${game.move.retry.delay:100}")
    )
    public Game makeMove(Long gameId, int position, GamePlayer player, String requestId) {
        if (isDuplicateRequest(requestId)) {
            return getResponseFromProcessedRequest(requestId);
        }

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameException("Game not found with id: " + gameId));
        
        log.info("Move gameId={}, position={}, reqId={}", gameId, position, requestId);
        validateMove(game, position, player != null ? player : game.getCurrentPlayer());
        applyMove(game, position);

        Game savedGame = gameRepository.save(game);

        if (requestId != null) {
            storeProcessedRequest(requestId, savedGame);
        }

        publishGameUpdate(savedGame);
        return savedGame;
    }

    private boolean isDuplicateRequest(String requestId) {
        return requestId != null && processedRequestRepository.existsById(requestId);
    }

    @SneakyThrows
    private Game getResponseFromProcessedRequest(String requestId) {
        ProcessedRequest processedRequest = processedRequestRepository.findById(requestId)
                .orElseThrow(() -> new GameException("Processed request not found"));
        return jsonMapper.readValue(processedRequest.getResponse(), Game.class);
    }

    private void validateMove(Game game, int position, GamePlayer player) {
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameException("Game is already finished");
        }

        if (game.getCurrentPlayer() != player) {
            throw new GameException("It's not your turn");
        }

        char[] board = game.getBoard().toCharArray();
        if (board[position] != ' ') {
            throw new GameException("Position already taken");
        }
    }

    private void applyMove(Game game, int position) {
        char[] board = game.getBoard().toCharArray();
        board[position] = game.getCurrentPlayer().getSymbol();
        game.setBoard(String.valueOf(board));
        if (checkWinner(board)) {
            game.setStatus(game.getCurrentPlayer().getWinStatus());
        } else if (isBoardFull(board)) {
            game.setStatus(GameStatus.DRAW);
        } else {
            game.setCurrentPlayer(getNextPlayer(game));
        }
    }

    private GamePlayer getNextPlayer(Game game) {
        return game.getCurrentPlayer().equals(GamePlayer.X) ? GamePlayer.O : GamePlayer.X;
    }

    @SneakyThrows
    private void storeProcessedRequest(String requestId, Game game) {
        ProcessedRequest processedRequest = new ProcessedRequest();
        processedRequest.setRequestId(requestId);
        processedRequest.setResponse(jsonMapper.writeValueAsString(game));
        processedRequestRepository.save(processedRequest);
    }

    private void publishGameUpdate(Game game) {
        GameMessage message = new GameMessage();
        message.setId(game.getId());
        message.setBoard(game.getBoard());
        message.setCurrentPlayer(game.getCurrentPlayer());
        message.setStatus(game.getStatus());
        messagingTemplate.convertAndSend("/topic/game/" + game.getId(), message);
    }

    private boolean checkWinner(char[] board) {
        int[][] winPatterns = {
                {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
                {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
                {0, 4, 8}, {2, 4, 6}             // diagonals
        };

        for (int[] pattern : winPatterns) {
            char c = board[pattern[0]];
            if (c != ' ' && c == board[pattern[1]] && c == board[pattern[2]]) {
                return true;
            }
        }
        return false;
    }

    private boolean isBoardFull(char[] board) {
        return IntStream.range(0, board.length).noneMatch(i -> board[i] == ' ');
    }

    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    public Game getGame(Long gameId) {
        return gameRepository.findById(gameId).orElseThrow(() -> new GameException("Game not found"));
    }
} 