package model;

public enum GameStatus {
    ONGOING,
    CHECK, // Optional refinement
    CHECKMATE_WHITE_WINS,
    CHECKMATE_BLACK_WINS,
    STALEMATE,
    WHITE_WINS_BY_TIMEOUT,
    BLACK_WINS_BY_TIMEOUT
}
