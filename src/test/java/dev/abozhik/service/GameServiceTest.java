package dev.abozhik.service;

import dev.abozhik.model.GamePlayer;
import dev.abozhik.model.GameStatus;
import dev.abozhik.model.constants.GameConstants;
import dev.abozhik.model.entity.Game;
import dev.abozhik.repository.GameRepository;
import dev.abozhik.repository.ProcessedRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class GameServiceTest {

    @MockitoSpyBean
    private GameService gameService;

    @MockitoSpyBean
    private GameRepository gameRepository;


    @Autowired
    private ProcessedRequestRepository processedRequestRepository;


    @Test
    void createGameTest() {
        //when
        Game game = gameService.createGame();
        //then
        assertNotNull(game.getId());
        assertEquals(GameConstants.EMPTY_BOARD, game.getBoard());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
    }

    @Test
    void makeMoveAndXWinsTest() {
        //given
        Game game = gameService.createGame();
        Long gameId = game.getId();
        //when
        gameService.makeMove(gameId, 0, GamePlayer.X, UUID.randomUUID().toString());
        gameService.makeMove(gameId, 3, GamePlayer.O, UUID.randomUUID().toString());
        gameService.makeMove(gameId, 1, GamePlayer.X, UUID.randomUUID().toString());
        gameService.makeMove(gameId, 4, GamePlayer.O, UUID.randomUUID().toString());
        Game finalGame = gameService.makeMove(gameId, 2, GamePlayer.X, UUID.randomUUID().toString());
        //then
        assertEquals(GameStatus.X_WON, finalGame.getStatus());
    }


    @Test
    void makeMoveAndXOWinsTest() {
        //given
        Game game = gameService.createGame();
        Long gameId = game.getId();
        //when
        gameService.makeMove(gameId, 0, GamePlayer.X, UUID.randomUUID().toString());
        gameService.makeMove(gameId, 3, GamePlayer.O, UUID.randomUUID().toString());
        gameService.makeMove(gameId, 1, GamePlayer.X, UUID.randomUUID().toString());
        gameService.makeMove(gameId, 4, GamePlayer.O, UUID.randomUUID().toString());
        gameService.makeMove(gameId, 6, GamePlayer.X, UUID.randomUUID().toString());
        Game finalGame = gameService.makeMove(gameId, 5, GamePlayer.O, UUID.randomUUID().toString());
        //then
        assertEquals(GameStatus.O_WON, finalGame.getStatus());
    }

    @Test
    void makeMoveAndDrawTest() {
        //given
        Game game = gameService.createGame();
        Long gameId = game.getId();
        int[] moves = {0, 1, 2, 4, 3, 5, 7, 6, 8};
        //when
        for (int i = 0; i < moves.length; i++) {
            int move = moves[i];
            gameService.makeMove(gameId, move, i % 2 == 0 ? GamePlayer.X : GamePlayer.O, UUID.randomUUID().toString());
        }
        //then
        Game finalGame = gameRepository.findById(gameId).orElseThrow();
        assertEquals(GameStatus.DRAW, finalGame.getStatus());
    }

    @Test
    void idempotencyTest() {
        //given
        Game game = gameService.createGame();
        String requestId = UUID.randomUUID().toString();
        //when
        Game firstCall = gameService.makeMove(game.getId(), 0, GamePlayer.X, requestId);
        Game secondCall = gameService.makeMove(game.getId(), 0, GamePlayer.O, requestId);
        //then
        assertEquals(firstCall.getId(), secondCall.getId());
        assertEquals(firstCall.getBoard(), secondCall.getBoard());
        assertEquals("X        ", gameRepository.findById(game.getId()).orElseThrow().getBoard());
    }

    @Test
    void concurrencyAccessWithOptimisticLockingTest() throws Exception {
        //given
        Game game = gameRepository.save(new Game(GameStatus.IN_PROGRESS, GameConstants.EMPTY_BOARD, GamePlayer.X));
        Long gameId = game.getId();

        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger count = new AtomicInteger(0);

        Answer<?> defaultAnswer = Mockito.mockingDetails(gameRepository)
                .getMockCreationSettings()
                .getDefaultAnswer();

        Mockito.doAnswer(invocation -> {
            latch.countDown();
            latch.await();
            if (count.incrementAndGet() >= 2) {
                return defaultAnswer.answer(invocation);
            }
            return Optional.of(game);
        }).when(gameRepository).findById(any());

        //when
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<?> thread1 = executor.submit(() -> {
            gameService.makeMove(gameId, 0, null, UUID.randomUUID().toString());
        });
        Future<?> thread2 = executor.submit(() -> {
            gameService.makeMove(gameId, 1, null, UUID.randomUUID().toString());
        });

        thread1.get();
        thread2.get();

        //then
        verify(gameService, atLeast(3)).makeMove(eq(gameId), anyInt(), any(), anyString());
    }


    @Test
    void makeMoveWhenGameNotInProgressTest() {
        //given
        Game game = new Game(GameStatus.X_WON, GameConstants.EMPTY_BOARD, GamePlayer.O);
        game = gameRepository.save(game);
        Long gameId = game.getId();
        //when then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            gameService.makeMove(gameId, 0, GamePlayer.O, UUID.randomUUID().toString());
        });
        assertEquals("Game is already finished", ex.getMessage());
    }

    @Test
    void makeMoveWhenNotCurrentPlayerTest() {
        //given
        Game game = new Game(GameStatus.IN_PROGRESS, GameConstants.EMPTY_BOARD, GamePlayer.X);
        game = gameRepository.save(game);
        Long gameId = game.getId();
        //when then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameService.makeMove(gameId, 0, GamePlayer.O, UUID.randomUUID().toString()));
        assertEquals("It's not your turn", ex.getMessage());
    }

    @Test
    void makeMoveWhenPositionAlreadyTakenTest() {
        //given
        Game game = new Game(GameStatus.IN_PROGRESS, "X        ", GamePlayer.X);
        game = gameRepository.save(game);
        Long gameId = game.getId();
        //when then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> gameService.makeMove(gameId, 0, GamePlayer.X, UUID.randomUUID().toString()));
        assertEquals("Position already taken", ex.getMessage());
    }

    @Test
    void makeMoveSuccessTest() {
        //given
        Game game = new Game(GameStatus.IN_PROGRESS, GameConstants.EMPTY_BOARD, GamePlayer.X);
        game = gameRepository.save(game);
        //when
        Game updated = gameService.makeMove(game.getId(), 0, GamePlayer.X, UUID.randomUUID().toString());
        //then
        assertEquals(GamePlayer.O, updated.getCurrentPlayer());
        assertEquals('X', updated.getBoard().charAt(0));
    }

    @ParameterizedTest
    @MethodSource("winningPatterns")
    void makeMoveAllWinningPatternTest(List<Integer> winningMoves) {
        // given
        Game game = new Game(GameStatus.IN_PROGRESS, "         ", GamePlayer.X);
        game = gameRepository.save(game);

        List<Integer> availablePositions = IntStream.range(0, 9)
                .filter(pos -> !winningMoves.contains(pos))
                .boxed()
                .toList();

        Iterator<Integer> oMoves = availablePositions.iterator();
        //when
        for (int i = 0; i < winningMoves.size(); i++) {
            int xMove = winningMoves.get(i);
            game = gameService.makeMove(game.getId(), xMove, GamePlayer.X, UUID.randomUUID().toString());

            if (i < winningMoves.size() - 1 && oMoves.hasNext()) {
                int oMove = oMoves.next();
                game = gameService.makeMove(game.getId(), oMove, GamePlayer.O, UUID.randomUUID().toString());
            }
        }
        //then
        assertEquals(GameStatus.X_WON, game.getStatus());
    }

    private static Stream<Arguments> winningPatterns() {
        return Stream.of(
                Arguments.of(List.of(0, 1, 2)), // row 1
                Arguments.of(List.of(3, 4, 5)), // row 2
                Arguments.of(List.of(6, 7, 8)), // row 3
                Arguments.of(List.of(0, 3, 6)), // column 1
                Arguments.of(List.of(1, 4, 7)), // column 2
                Arguments.of(List.of(2, 5, 8)), // column 3
                Arguments.of(List.of(0, 4, 8)), // diagonal \
                Arguments.of(List.of(2, 4, 6))  // diagonal /
        );
    }
}
