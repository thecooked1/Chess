package com.chess.model.Board;

import com.chess.common.Colour;
import com.chess.common.Square;
import com.chess.model.pieces.Piece;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        // This method runs before each test, providing a fresh board.
        board = new Board();
    }

    // ===================================
    // TESTS FOR BUGS YOU ARE EXPERIENCING
    // ===================================

    @Test
    @DisplayName("FAILURE 1: Should correctly identify stalemate and parse FEN turn")
    void testStalemate() {
        // FEN for a stalemate position. 'b' means it is BLACK's turn.
        board.updateFromFen("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");

        // This assertion will fail with your current code.
        assertEquals(Colour.BLACK, board.getTurn(), "FEN parsing should set the turn to Black.");

        assertFalse(board.isInCheck(Colour.BLACK), "King should not be in check in this stalemate position.");
        assertFalse(board.hasAnyLegalMoves(Colour.BLACK), "Black should have no legal moves in this stalemate position.");
    }

    @Test
    @DisplayName("FAILURE 2: Board should initialize with correct black piece symbols")
    void testInitialSetupBlackPieces() {
        // Your Pawn class sets symbol to 'P' regardless of color. This will fail.
        Piece blackPawn = board.getPiece(new Square(1, 4)); // e7
        assertNotNull(blackPawn);
        // This fails because your Pawn constructor hardcodes the symbol to 'P'.
        assertEquals('p', blackPawn.getSymbol(), "Black pawn symbol should be lowercase 'p'.");
    }

    @Test
    @DisplayName("FAILURE 3: White pawn should promote when capturing a piece on the 8th rank")
    void testPawnPromotionByCapture() {
        // Setup: White pawn on g7, Black rook on h8. White to move.
        // The pawn can legally capture the rook and promote.
        board.updateFromFen("7r/6P1/8/k7/8/8/8/K7 w - - 0 1");

        Square start = new Square(1, 6); // g7
        Square end = new Square(0, 7);   // h8

        // This assertion should pass. The move is a valid diagonal capture.
        assertTrue(board.isLegalMove(start, end), "Pawn on g7 should be able to capture the rook on h8.");

        // We can also test the application of the move to be thorough.
        board.applyMove(start, end, Optional.of("N")); // Promote to a Knight

        Piece promotedPiece = board.getPiece(end);
        assertNotNull(promotedPiece, "There should be a piece on h8 after promotion.");
        assertEquals('N', promotedPiece.getSymbol(), "The promoted piece should be a Knight.");
        assertEquals(Colour.WHITE, promotedPiece.getColor(), "The promoted piece should be white.");
    }

    @Test
    @DisplayName("FAILURE 4: A pinned piece should not be able to move and expose the king")
    void testPinnedPieceCannotMove() {
        // Setup: A white rook on e4 is pinned to the king on e1 by a black rook on e8.
        board.updateFromFen("4r3/8/8/8/4R3/8/8/4K3 w - - 0 1");
        Square pinnedRookSquare = new Square(4, 4); // The Rook at e4

        // This assertion will fail. Your current `isLegalMove` allows the rook to move horizontally.
        // It simulates the move, sees the king is NOT in check (because the rook is still on the file),
        // and incorrectly allows it.
        List<Square> legalMoves = board.getLegalMovesForPiece(pinnedRookSquare);
        assertFalse(legalMoves.contains(new Square(4, 3)), "Pinned rook should NOT be able to move to d4.");
        assertFalse(legalMoves.contains(new Square(4, 5)), "Pinned rook should NOT be able to move to f4.");

        // It SHOULD be able to move along the pin axis.
        assertTrue(legalMoves.contains(new Square(5, 4)), "Pinned rook SHOULD be able to move to e3.");
    }

    // ===================================
    // OTHER IMPORTANT TESTS
    // ===================================

    @Test
    @DisplayName("Should correctly identify checkmate (Fool's Mate)")
    void testFoolsmateCheckmate() {
        // 1. f3 e5 2. g4 Qh4#
        board.applyMove(new Square(6, 5), new Square(5, 5), Optional.empty()); // f3
        board.applyMove(new Square(1, 4), new Square(3, 4), Optional.empty()); // e5
        board.applyMove(new Square(6, 6), new Square(4, 6), Optional.empty()); // g4
        board.applyMove(new Square(0, 3), new Square(4, 7), Optional.empty()); // Qh4#

        assertTrue(board.isInCheck(Colour.WHITE), "White king should be in check.");
        assertFalse(board.hasAnyLegalMoves(Colour.WHITE), "White should have no legal moves after being checkmated.");
    }

    @Test
    @DisplayName("White should be able to capture en passant")
    void testEnPassant() {
        // 1. e4 (dummy move) 2. e5 d5. After this, white pawn on e5 can capture d5 en passant.
        board.applyMove(new Square(6, 4), new Square(4, 4), Optional.empty());
        board.applyMove(new Square(1, 0), new Square(2, 0), Optional.empty());
        board.applyMove(new Square(4, 4), new Square(3, 4), Optional.empty());

        // This move by black sets the en passant target square to d6 (rank 2, file 3)
        board.applyMove(new Square(1, 3), new Square(3, 3), Optional.empty());

        Square enPassantTarget = board.getEnPassantTargetSquare();
        assertNotNull(enPassantTarget, "En passant target square should be set.");
        assertEquals(new Square(2, 3), enPassantTarget, "En passant target should be d6.");

        Square whitePawn = new Square(3, 4); // e5
        assertTrue(board.isLegalMove(whitePawn, enPassantTarget), "Pawn should be able to capture en passant.");
    }

    // Add this method inside your BoardTest class
    @Test
    @DisplayName("Rook should be blocked by its own pieces")
    void testRookBlockedByFriendlyPiece() {
        // Standard starting position
        Board board = new Board();
        Square rookSquare = new Square(7, 0); // a1

        // The path to a4 is blocked by the pawn on a2
        Square targetSquare = new Square(4, 0); // a4

        assertFalse(board.isLegalMove(rookSquare, targetSquare), "Rook on a1 should not be able to jump over the pawn on a2.");
    }

    // Add this method inside your BoardTest class
    @Test
    @DisplayName("King should not be able to move into a square attacked by an opponent")
    void testKingCannotMoveIntoCheck() {
        // Setup: White king on e1, Black rook on a2. White to move.
        board.updateFromFen("8/8/8/8/8/8/r7/4K3 w - - 0 1");

        Square kingStart = new Square(7, 4); // e1

        // The d1 square (7, 3) is attacked by the black rook on a2 (6,0)
        // Wait, the rook on a2 attacks the second rank, not the first. Let's fix the FEN.
        // Correct FEN: Black rook on a1 attacks the entire first rank.
        board.updateFromFen("8/8/8/8/8/8/8/r3K3 w - - 0 1");

        Square illegalTarget = new Square(7, 3); // d1
        Square legalTarget = new Square(6, 4); // e2

        assertFalse(board.isLegalMove(kingStart, illegalTarget), "King should not be able to move into check (to d1).");
        assertTrue(board.isLegalMove(kingStart, legalTarget), "King should be able to move to a safe square (e2).");
    }

    // Add this method inside your BoardTest class
    @Test
    @DisplayName("En passant capture should only be available for one move")
    void testEnPassantExpiresAfterOneMove() {
        // Setup: 1. e4 (dummy) 2. e5 d5
        board.applyMove(new Square(6, 4), new Square(4, 4), Optional.empty()); // 1. e4
        board.applyMove(new Square(1, 0), new Square(2, 0), Optional.empty()); // 1. ... a6
        board.applyMove(new Square(4, 4), new Square(3, 4), Optional.empty()); // 2. e5

        // Black's two-square pawn advance sets the en passant target.
        board.applyMove(new Square(1, 3), new Square(3, 3), Optional.empty()); // 2. ... d5
        assertNotNull(board.getEnPassantTargetSquare(), "En passant should be available immediately after the pawn jump.");

        // Now, instead of capturing, White makes a "wasted" move.
        board.applyMove(new Square(6, 0), new Square(5, 0), Optional.empty()); // 3. a3

        // Black makes a "wasted" move.
        board.applyMove(new Square(1, 1), new Square(2, 1), Optional.empty()); // 3. ... b6

        // It is now White's turn again. The en passant opportunity should be gone.
        assertNull(board.getEnPassantTargetSquare(), "En passant target should be null after one full move cycle.");

        Square whitePawn = new Square(3, 4); // e5
        Square enPassantSquare = new Square(2, 3); // d6
        assertFalse(board.isLegalMove(whitePawn, enPassantSquare), "Pawn should no longer be able to capture en passant.");
    }

}