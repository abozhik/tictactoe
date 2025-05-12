package dev.abozhik.model.entity;

import dev.abozhik.model.GamePlayer;
import dev.abozhik.model.GameStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private GamePlayer currentPlayer;

    @Column(nullable = false)
    private String board;

    @Column(nullable = false)
    private GameStatus status;

    @Column(nullable = false)
    private LocalDateTime creationDateTime;

    @Version
    private Long version;

    public Game(GameStatus status, String board, GamePlayer currentPlayer) {
        this.status = status;
        this.board = board;
        this.currentPlayer = currentPlayer;
    }

    @PrePersist
    protected void onCreate() {
        creationDateTime = LocalDateTime.now();
    }
}