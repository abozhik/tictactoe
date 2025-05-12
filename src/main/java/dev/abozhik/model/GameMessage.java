package dev.abozhik.model;

import lombok.Data;

@Data
public class GameMessage {
    private Long id;
    private int position;
    private String board;
    private GamePlayer currentPlayer;
    private GameStatus status;
}