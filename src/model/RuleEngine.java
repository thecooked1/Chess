package model;

import model.piece.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates chess rules: move validation, check, checkmate, stalemate.
 */
public class RuleEngine {

    /**
     * Checks if the king of the specified color is currently under attack.
     */
    public boolean isKingInCheck(BoardModel board, PlayerColor kingColor) {
        Position kingPos = board.findKing(kingColor);
        if (kingPos == null) return false; // Or throw error - king should always be present

        PlayerColor attackerColor = kingColor.opposite();
        List<Piece> opponentPieces = board.getPieces(attackerColor);

        for (Piece attacker : opponentPieces) {
            Position attackerPos = findPiecePosition(board, attacker); // Find where the attacker is
            if (attackerPos == null) continue; // Should not happen in consistent state

            // Check if any pseudo-legal move of the attacker targets the king
            List<Position> pseudoMoves = attacker.getPseudoLegalMoves(board, attackerPos);
            if (pseudoMoves.contains(kingPos)) {
                return true; // King is attacked
            }
        }
        return false; // King is safe
    }

    /**
     * Generates all fully legal moves for a specific piece at a given position.
     */
    public List<Move> generateLegalMovesForPiece(ChessGame game, Position piecePos) {
        BoardModel board = game.getBoardModel();
        Piece piece = board.getPieceAt(piecePos);
        if (piece == null || piece.getColor() != game.getCurrentTurn()) {
            return new ArrayList<>(); // No piece or not its turn
        }

        List<Position> pseudoTargets = piece.getPseudoLegalMoves(board, piecePos);
        List<Move> legalMoves = new ArrayList<>();

        for (Position targetPos : pseudoTargets) {
            Move potentialMove = new Move(piece, piecePos, targetPos);
            // Simulate the move
            BoardModel simulatedBoard = new BoardModel(board);
            simulatedBoard.applyMove(potentialMove); // applyMove updates piece's internal state like hasMoved

            // Check if the move leaves the player's own king in check
            if (!isKingInCheck(simulatedBoard, piece.getColor())) {
                legalMoves.add(potentialMove);
            }
        }

        // Add special moves (Castling) if applicable
        if (piece.getType() == PieceType.KING) {
            // Check and add castling moves (requires game state for rights)
            // This logic needs access to game.canCastleKingSide(), game.canCastleQueenSide() etc.
            // Needs to check path clear and not through check. Complex!
            // Example placeholder:
            // addCastlingMovesIfLegal(game, (King)piece, piecePos, legalMoves);
        }

        // Add special moves (En Passant) if applicable
        if (piece.getType() == PieceType.PAWN) {
            // Check and add en passant moves (requires game state for target square)
            // Needs to check game.getEnPassantTarget(), check if this pawn can capture there.
            // Needs simulation to ensure it doesn't leave king in check.
            // Example placeholder:
            // addEnPassantMovesIfLegal(game, (Pawn)piece, piecePos, legalMoves);
        }

        return legalMoves;
    }

    /**
     * Generates all legal moves for the current player.
     */
    public List<Move> generateAllLegalMoves(ChessGame game) {
        List<Move> allLegalMoves = new ArrayList<>();
        PlayerColor currentPlayer = game.getCurrentTurn();
        List<Piece> playerPieces = game.getBoardModel().getPieces(currentPlayer);

        for (Piece piece : playerPieces) {
            Position piecePos = findPiecePosition(game.getBoardModel(), piece);
            if (piecePos != null) {
                allLegalMoves.addAll(generateLegalMovesForPiece(game, piecePos));
            }
        }
        return allLegalMoves;
    }


    /**
     * Determines the current status of the game (Ongoing, Checkmate, Stalemate).
     */
    public GameStatus getGameStatus(ChessGame game) {
        PlayerColor currentPlayer = game.getCurrentTurn();
        List<Move> legalMoves = generateAllLegalMoves(game);

        if (legalMoves.isEmpty()) {
            if (isKingInCheck(game.getBoardModel(), currentPlayer)) {
                return (currentPlayer == PlayerColor.WHITE) ? GameStatus.CHECKMATE_BLACK_WINS : GameStatus.CHECKMATE_WHITE_WINS;
            } else {
                return GameStatus.STALEMATE;
            }
        } else {
            // Optional: Distinguish check state
            // if (isKingInCheck(game.getBoardModel(), currentPlayer)) {
            //     return GameStatus.CHECK;
            // }
            return GameStatus.ONGOING;
        }
        // Add checks for other draw conditions (50-move rule, repetition, insufficient material) if needed
    }


    /**
     * Helper to find the position of a specific piece object on the board.
     * This is needed because Piece objects don't store their position.
     */
    private Position findPiecePosition(BoardModel board, Piece pieceToFind) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if (board.getPieceAt(new Position(r, c)) == pieceToFind) {
                    return new Position(r, c);
                }
            }
        }
        return null; // Piece not found
    }

    // Placeholder methods for special move logic (to be fully implemented)
    // private void addCastlingMovesIfLegal(ChessGame game, King king, Position kingPos, List<Move> legalMoves) { ... }
    // private void addEnPassantMovesIfLegal(ChessGame game, Pawn pawn, Position pawnPos, List<Move> legalMoves) { ... }

}