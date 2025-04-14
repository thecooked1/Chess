package view;

import model.*; // Need model classes for updates
import java.util.List;

// Interface defining how the Controller updates the View
public interface IGameView {
    void initializeBoard(Piece[][] boardState); // Initial setup
    void updateBoard(Piece[][] boardState);     // Refresh board display
    void updateClock(PlayerColor color, String timeString);
    void showGameOver(GameStatus status, PlayerColor winner); // Winner can be null for stalemate
    void highlightSquare(Position pos, java.awt.Color color); // For selection feedback
    void highlightLegalMoves(List<Position> legalTargets);
    void clearHighlights();
    void setStatusMessage(String message); // General status like whose turn or "Check!"
    void closeGameWindow();
    // Add methods for pawn promotion dialog if implementing
}