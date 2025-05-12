package dev.abozhik.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.abozhik.model.GamePlayer;
import dev.abozhik.model.GameStatus;
import dev.abozhik.model.MoveRequest;
import dev.abozhik.model.constants.GameConstants;
import dev.abozhik.model.entity.Game;
import dev.abozhik.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Game savedGame;

    @BeforeEach
    void setUp() {
        Game game = new Game(GameStatus.IN_PROGRESS, GameConstants.EMPTY_BOARD, GamePlayer.X);
        savedGame = gameRepository.save(game);
    }

    @Test
    void createGameTest() throws Exception {
        mockMvc.perform(post("/api/game/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.board", is(GameConstants.EMPTY_BOARD)));
    }

    @Test
    void listGamesTest() throws Exception {
        gameRepository.save(new Game(GameStatus.IN_PROGRESS, GameConstants.EMPTY_BOARD, GamePlayer.X));
        mockMvc.perform(get("/api/game"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getGameTest() throws Exception {
        mockMvc.perform(get("/api/game/{id}", savedGame.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(savedGame.getId().intValue())));
    }

    @Test
    void makeMoveTest() throws Exception {
        MoveRequest move = new MoveRequest();
        move.setPosition(0);
        move.setPlayer(GamePlayer.X);
        move.setRequestId(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/game/{id}/move", savedGame.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(move)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.board", is("X        ")))
                .andExpect(jsonPath("$.currentPlayer", is("O")));
    }

    @Test
    void makeMoveWhenInvalidPositionTest() throws Exception {
        MoveRequest move = new MoveRequest();
        move.setPosition(999);
        move.setPlayer(GamePlayer.X);
        move.setRequestId(UUID.randomUUID().toString());

        mockMvc.perform(post("/api/game/{id}/move", savedGame.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(move)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Game Error"))
                .andExpect(jsonPath("$.message").value("Invalid position. Position must be between 0 and 8"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getGameWhenNotFoundTest() throws Exception {
        mockMvc.perform(get("/api/game/999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Game Error"))
                .andExpect(jsonPath("$.message").value("Game not found"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
