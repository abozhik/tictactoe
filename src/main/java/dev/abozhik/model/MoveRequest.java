package dev.abozhik.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request object for making a move in the game")
public class MoveRequest {

    @Schema(description = "Position on the board (0-8, where 0 is top-left and 8 is bottom-right)", example = "4")
    private int position;

    @Schema(description = "Unique identifier for the move request", example = "move-123")
    private String requestId;

    @Schema(description = "Player making the move (X or O)", example = "X")
    private GamePlayer player;
} 