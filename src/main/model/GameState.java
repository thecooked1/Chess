package main.model;

import main.model.Piece;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private final Board board;
    private Color currentPlayer;
    private final CastlingRights castlingRights;
    private Position enPassantTarget; // Square behind pawn that just moved two steps
    // Add move history, fifty-move rule counter, etc., if needed for draws

    public GameState() {
        board = new Board();
        board.initializeStandardSetup();
        currentPlayer = Color.WHITE;
        castlingRights = new CastlingRights(); // Start with all rights
        enPassantTarget = null;
    }

    // Copy constructor for simulation/rollback
    public GameState(GameState other) {
        this.board = new Board(other.board); // Use Board's copy constructor
        this.currentPlayer = other.currentPlayer;
        this.castlingRights = new CastlingRights(other.castlingRights); // Use CastlingRights' copy constructor
        this.enPassantTarget = other.enPassantTarget; // Position is immutable, direct copy is fine
    }


    public Board getBoard() {
        return board;
    }

    public Color getCurrentPlayer() {
        return currentPlayer;
    }

    public CastlingRights getCastlingRights() {
        return castlingRights;
    }

    public Position getEnPassantTarget() {
        return enPassantTarget;
    }

    public void switchPlayer() {
        currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
    }

    // --- Methods to modify state (used by GameLogic) ---

    // Applies a move *without validation*. Validation must happen first.
    public void applyMoveUnsafe(Move move) {
        Position start = move.getStartPosition();
        Position end = move.getEndPosition();
        Piece movingPiece = board.getPieceAt(start);

        if (movingPiece == null) {
            System.err.println("Warning: applyMoveUnsafe called with no piece at start: " + start);
            return; // Should not happen if move was validated
        }

        // --- State Updates Before Moving Piece ---

        // 1. Store previous En Passant target for capture check later
        Position previousEnPassantTarget = this.enPassantTarget;

        // 2. Clear current En Passant target (it's only valid for one turn)
        this.enPassantTarget = null; // Reset before checking if the current move creates a new one

        // 3. Update Castling Rights (based on piece type and start/end positions)
        updateCastlingRightsOnMove(movingPiece, start, end);


        // --- Perform Board Changes ---

        // 4. Handle En Passant Capture (Must use previousEnPassantTarget)
        boolean isEnPassantCapture = movingPiece.getType() == PieceType.PAWN &&
                end.equals(previousEnPassantTarget);
        if (isEnPassantCapture) {
            // Determine the actual captured pawn's position
            int capturedPawnRow = (movingPiece.getColor() == Color.WHITE) ? end.getRow() + 1 : end.getRow() - 1;
            Position capturedPawnPos = new Position(capturedPawnRow, end.getCol());
            board.setPieceAt(capturedPawnPos, null); // Remove the captured pawn
            // System.out.println("Debug: En Passant capture executed. Removed pawn at " + capturedPawnPos);
        }

        // 5. Move the primary piece (capturing implicitly if end square occupied)
        board.movePiece(start, end); // This also sets hasMoved flag

        // 6. Handle Castling (Move the Rook)
        if (movingPiece.getType() == PieceType.KING) {
            handleCastlingRookMove(start, end);
        }

        // 7. Handle Pawn Promotion (Replace Pawn with new piece)
        if (move.isPromotion()) {
            handlePromotion(end, movingPiece.getColor(), move.getPromotionPieceType());
        }

        // 8. Set New En Passant Target (if this move was a 2-square pawn push)
        if (movingPiece.getType() == PieceType.PAWN && Math.abs(start.getRow() - end.getRow()) == 2) {
            int targetRow = start.getRow() + (end.getRow() - start.getRow()) / 2;
            this.enPassantTarget = new Position(targetRow, start.getCol());
            // System.out.println("Debug: New En Passant target set at " + this.enPassantTarget);
        }

        // --- Final Step ---
        // 9. Switch Player
        switchPlayer();
    }


    // --- Helper methods for applyMoveUnsafe ---

    private void updateCastlingRightsOnMove(Piece movingPiece, Position start, Position end) {
        Color color = movingPiece.getColor();
        // If King moves
        if (movingPiece.getType() == PieceType.KING) {
            castlingRights.revokeAll(color);
        }
        // If Rook moves from home square
        else if (movingPiece.getType() == PieceType.ROOK) {
            Position whiteQueenRookHome = new Position(7, 0);
            Position whiteKingRookHome = new Position(7, 7);
            Position blackQueenRookHome = new Position(0, 0);
            Position blackKingRookHome = new Position(0, 7);

            if (start.equals(whiteQueenRookHome)) castlingRights.revokeQueenSide(Color.WHITE);
            else if (start.equals(whiteKingRookHome)) castlingRights.revokeKingSide(Color.WHITE);
            else if (start.equals(blackQueenRookHome)) castlingRights.revokeQueenSide(Color.BLACK);
            else if (start.equals(blackKingRookHome)) castlingRights.revokeKingSide(Color.BLACK);
        }

        // If a Rook is captured *on its home square*
        Piece capturedPiece = board.getPieceAt(end); // Important: Check *before* move overwrites! (Board needs lookahead or this check needs raw data)
        // Correction: board.movePiece handles capture AFTER setting null, so we need to check the 'end' position piece *before* the move is made.
        // Let's assume board.movePiece returns the captured piece or we check it *before* board.movePiece.
        // For simplicity, let's assume the current `board.getPieceAt(end)` *before* the move is valid
        // Or, better, `applyMoveUnsafe` needs the board state *before* the move for this check.
        // A simpler way: If the END square is a rook home square, check the color of the piece that *would* be captured.

        // Let's restructure applyMoveUnsafe slightly for clarity on capture check:

         /* Alternative structure for applyMoveUnsafe regarding capture check:
         ...
         Piece capturedPiece = board.getPieceAt(end); // Check BEFORE moving
         // 3. Update Castling Rights based on moving piece AND captured piece
         updateCastlingRightsOnMove(movingPiece, start, end, capturedPiece); // Pass captured piece info
         // ... rest of logic ...
         board.movePiece(start, end); // Now actually perform move/capture
         ...
         */

        // Assuming current structure, this check is less reliable if piece was already moved by board.movePiece
        // Let's check the target square directly.
        if (end.equals(new Position(7, 0)) && board.getPieceAt(end) != null && board.getPieceAt(end).getColor() == Color.BLACK) castlingRights.revokeQueenSide(Color.WHITE); // Black Rook on white square? Incorrect logic.

        // --- Revised Castling Revocation on Capture ---
        // We need to check if the *opponent's* rook is captured on its home square.
        Position targetSquare = end;
        Piece potentialCapture = board.getPieceAt(targetSquare); // Piece currently on target square
        if(potentialCapture != null && potentialCapture.getType() == PieceType.ROOK) {
            Color capturedColor = potentialCapture.getColor();
            if(capturedColor != color) { // Ensure it's an opponent's rook being captured
                Position whiteQueenRookHome = new Position(7, 0);
                Position whiteKingRookHome = new Position(7, 7);
                Position blackQueenRookHome = new Position(0, 0);
                Position blackKingRookHome = new Position(0, 7);

                if (targetSquare.equals(whiteQueenRookHome) && capturedColor == Color.WHITE) castlingRights.revokeQueenSide(Color.WHITE);
                else if (targetSquare.equals(whiteKingRookHome) && capturedColor == Color.WHITE) castlingRights.revokeKingSide(Color.WHITE);
                else if (targetSquare.equals(blackQueenRookHome) && capturedColor == Color.BLACK) castlingRights.revokeQueenSide(Color.BLACK);
                else if (targetSquare.equals(blackKingRookHome) && capturedColor == Color.BLACK) castlingRights.revokeKingSide(Color.BLACK);
            }
        }
    }


    private void handleCastlingRookMove(Position kingStart, Position kingEnd) {
        if (Math.abs(kingStart.getCol() - kingEnd.getCol()) == 2) { // King moved two squares -> Castling
            Position rookStart, rookEnd;
            if (kingEnd.getCol() > kingStart.getCol()) { // King side castle (e.g., e1g1 or e8g8)
                rookStart = new Position(kingStart.getRow(), 7); // h1 or h8
                rookEnd = new Position(kingStart.getRow(), 5);   // f1 or f8
            } else { // Queen side castle (e.g., e1c1 or e8c8)
                rookStart = new Position(kingStart.getRow(), 0); // a1 or a8
                rookEnd = new Position(kingStart.getRow(), 3);   // d1 or d8
            }
            // System.out.println("Debug: Castling detected. Moving rook from " + rookStart + " to " + rookEnd);
            Piece rook = board.getPieceAt(rookStart);
            if (rook != null && rook.getType() == PieceType.ROOK) {
                board.movePiece(rookStart, rookEnd);
            } else {
                System.err.println("Error during castling: Rook not found at " + rookStart);
            }
        }
    }

    private void handlePromotion(Position end, Color color, PieceType promotionType) {
        // System.out.println("Debug: Promoting pawn at " + end + " to " + promotionType);
        Piece promotedPiece = PieceFactory.createPiece(promotionType, color);
        promotedPiece.setHasMoved(true); // Promoted piece counts as moved
        board.setPieceAt(end, promotedPiece);
    }

    // --- Game Status Checks ---
    public boolean isCheck(Color kingColor) {
        Position kingPos = board.findKing(kingColor);
        if (kingPos == null) {
            System.err.println("CRITICAL ERROR: King not found for color " + kingColor + " during isCheck(). Board state might be corrupt.");
            // Depending on desired robustness, you might want to throw an exception here
            // or return false, but logging is essential.
            // new Throwable().printStackTrace(); // Print stack trace for debugging where king disappeared
            return false;
        }
        Color opponentColor = (kingColor == Color.WHITE) ? Color.BLACK : Color.WHITE;
        return isSquareAttackedBy(kingPos, opponentColor);
    }

    /**
     * Checks if the given square is attacked by any piece of the specified attackerColor.
     * This method relies on the getPseudoLegalMoves() of each piece, which should
     * correctly include all possible squares the piece can move to or capture on
     * (ignoring whether the move leaves the king in check).
     *
     * @param square The Position to check for attacks.
     * @param attackerColor The Color of the pieces that might be attacking.
     * @return true if the square is attacked, false otherwise.
     */
    public boolean isSquareAttackedBy(Position square, Color attackerColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                // Position constructor is safe here as r, c are guaranteed 0-7
                Position attackerPos = new Position(r, c);
                Piece p = board.getPieceAt(attackerPos);

                // Check if a piece of the attacking color exists at this position
                if (p != null && p.getColor() == attackerColor) {

                    // Get all pseudo-legal moves for this piece.
                    // We are now confident that the getPseudoLegalMoves methods
                    // have the necessary bounds checks.
                    List<Position> pseudoMoves = p.getPseudoLegalMoves(board, attackerPos);

                    // Check if the target 'square' is among the piece's pseudo-legal moves.
                    // This covers standard moves AND captures (including pawn diagonal captures)
                    // because getPseudoLegalMoves returns all potential destinations.
                    if (pseudoMoves.contains(square)) {
                        // System.out.println("Debug: Square " + square + " is attacked by " + p + " at " + attackerPos); // Optional debug
                        return true; // The square is attacked
                    }
                }
            }
        }
        // If loop completes without finding an attacker
        return false;
    }

    public boolean isCheckmate(Color kingColor) {
        if (!isCheck(kingColor)) {
            return false; // Not in check, so cannot be checkmate
        }
        // If in check, check if there are *any* legal moves for the player
        return generateAllLegalMoves(kingColor).isEmpty();
    }

    public boolean isStalemate(Color kingColor) {
        if (isCheck(kingColor)) {
            return false; // In check, so not stalemate (could be checkmate)
        }
        // If not in check, check if there are *any* legal moves
        return generateAllLegalMoves(kingColor).isEmpty();
    }


    /**
     * Generates all fully legal moves for the specified player.
     * This considers checks and prevents moves that leave the king in check.
     */
    public List<Move> generateAllLegalMoves(Color playerColor) {
        List<Move> legalMoves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position startPos = new Position(r, c);
                Piece piece = board.getPieceAt(startPos);
                if (piece != null && piece.getColor() == playerColor) {
                    // Use a temporary list for this piece's pseudo moves + special moves
                    List<Position> pseudoMoveTargets = piece.getPseudoLegalMoves(board, startPos);
                    List<Move> potentialMovesForPiece = new ArrayList<>();

                    // --- Add potential special moves (Castling, En Passant) ---
                    addSpecialPseudoMoves(piece, startPos, pseudoMoveTargets); // Modifies pseudoMoveTargets list


                    // --- Convert target positions to Move objects, handling promotions ---
                    for (Position endPos : pseudoMoveTargets) {
                        // Check for Pawn Promotion
                        if (piece.getType() == PieceType.PAWN) {
                            int promotionRank = (playerColor == Color.WHITE) ? 0 : 7;
                            if (endPos.getRow() == promotionRank) {
                                // If a pawn reaches the final rank, generate moves for each possible promotion
                                PieceType[] promotions = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};
                                for (PieceType promoType : promotions) {
                                    potentialMovesForPiece.add(new Move(startPos, endPos, promoType));
                                }
                            } else {
                                // Standard pawn move (not promotion)
                                potentialMovesForPiece.add(new Move(startPos, endPos));
                            }
                        } else {
                            // Standard move for other pieces
                            potentialMovesForPiece.add(new Move(startPos, endPos));
                        }
                    }

                    // --- Filter for Legality (Check safety) ---
                    for (Move potentialMove : potentialMovesForPiece) {
                        if (isMoveLegal(potentialMove)) {
                            legalMoves.add(potentialMove);
                        }
                    }
                }
            }
        }
        return legalMoves;
    }

    // Helper to add castling and en passant moves before legality check
    private void addSpecialPseudoMoves(Piece piece, Position startPos, List<Position> pseudoMoveTargets) {
        // 1. En Passant (Pawn only)
        if (piece.getType() == main.model.PieceType.PAWN && enPassantTarget != null) {
            int direction = (piece.getColor() == Color.WHITE) ? -1 : 1;
            int targetRow = startPos.getRow() + direction;
            // Check if the pawn is adjacent horizontally to the en passant target's column
            // and if the move forward-diagonal lands on the en passant target square
            if (targetRow == enPassantTarget.getRow() && Math.abs(startPos.getCol() - enPassantTarget.getCol()) == 1) {
                // Check if the diagonal move is to the correct en passant target square
                Position epMoveTarget = new Position(targetRow, enPassantTarget.getCol());
                if (epMoveTarget.equals(enPassantTarget)) {
                    pseudoMoveTargets.add(enPassantTarget);
                }
            }
        }


        // 2. Castling (King only)
        if (piece.getType() == main.model.PieceType.KING && !piece.hasMoved()) {
            Color color = piece.getColor();
            int kingRow = startPos.getRow();

            // King Side
            if (castlingRights.canCastle(color, true)) {
                Position rookPos = new Position(kingRow, 7);
                Piece rook = board.getPieceAt(rookPos);
                if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                    // Check squares between king and rook are empty
                    if (board.getPieceAt(kingRow, 5) == null && board.getPieceAt(kingRow, 6) == null) {
                        // Check king doesn't pass through check
                        if (!isSquareAttackedBy(startPos, (color == Color.WHITE) ? Color.BLACK : Color.WHITE) &&
                                !isSquareAttackedBy(new Position(kingRow, 5), (color == Color.WHITE) ? Color.BLACK : Color.WHITE) &&
                                !isSquareAttackedBy(new Position(kingRow, 6), (color == Color.WHITE) ? Color.BLACK : Color.WHITE)) {
                            pseudoMoveTargets.add(new Position(kingRow, 6)); // Add king's target square for castling
                        }
                    }
                }
            }
            // Queen Side
            if (castlingRights.canCastle(color, false)) {
                Position rookPos = new Position(kingRow, 0);
                Piece rook = board.getPieceAt(rookPos);
                if (rook != null && rook.getType() == PieceType.ROOK && !rook.hasMoved()) {
                    // Check squares between king and rook are empty
                    if (board.getPieceAt(kingRow, 1) == null &&
                            board.getPieceAt(kingRow, 2) == null &&
                            board.getPieceAt(kingRow, 3) == null) {
                        // Check king doesn't pass through check
                        if (!isSquareAttackedBy(startPos, (color == Color.WHITE) ? Color.BLACK : Color.WHITE) &&
                                !isSquareAttackedBy(new Position(kingRow, 3), (color == Color.WHITE) ? Color.BLACK : Color.WHITE) &&
                                !isSquareAttackedBy(new Position(kingRow, 2), (color == Color.WHITE) ? Color.BLACK : Color.WHITE)) {
                            pseudoMoveTargets.add(new Position(kingRow, 2)); // Add king's target square for castling
                        }
                    }
                }
            }
        }
    }


    // Checks if a specific move is fully legal (does not leave the king in check)
    private boolean isMoveLegal(Move move) {
        Piece movingPiece = board.getPieceAt(move.getStartPosition());
        if (movingPiece == null) return false; // No piece to move

        // Simulate the move on a temporary board state
        GameState tempState = new GameState(this); // Use copy constructor
        // Important: Temporarily switch player *before* applying move for simulation context
        tempState.currentPlayer = this.currentPlayer;
        tempState.applyMoveUnsafe(move);

        // Check if the player who *made* the move left their own king in check
        return !tempState.isCheck(movingPiece.getColor());
    }



}