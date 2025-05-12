package dev.abozhik.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GamePlayer {
    X('X', GameStatus.X_WON),
    O('O', GameStatus.O_WON);

    private final char symbol;
    private final GameStatus winStatus;
}