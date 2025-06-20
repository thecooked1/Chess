package main.model.Board;

import main.model.Square;
import main.model.pieces.*;
import main.model.PGNParser.Move;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Board {
    private final Piece[][] grid;
    private Colour turn;
    private Square enPassantTargetSquare;
    private Square kingInCheckSquare; // To easily highlight a king in check

    // castling rights tracking
    private boolean whiteKingsideCastleRight = true;
    private boolean whiteQueensideCastleRight = true;
    private boolean blackKingsideCastleRight = true;
    private boolean blackQueensideCastleRight = true;

    public Board() {
        grid = new Piece[8][8];
        setupInitialPosition();
    }

    public void printBoard() {
        System.out.println("  a b c d e f g h   Turn: " + turn);
        System.out.println(" +-----------------+");
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + "|");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                char symbol = (p == null) ? '.' : p.getSymbol();
                // Use lowercase for black pieces for better readability
                if (p != null && p.getColor() == Colour.BLACK) {
                    symbol = Character.toLowerCase(symbol);
                }
                System.out.print(symbol + " ");
            }
            System.out.println("|" + (8 - r));
        }
        System.out.println(" +-----------------+");
        System.out.println("  a b c d e f g h   EP Target: " + (enPassantTargetSquare == null ? "None" : enPassantTargetSquare));
    }


    public Piece applyMove(Square start, Square end, Optional<String> promotionPiece) {
        Piece movingPiece = getPiece(start);
        if (movingPiece == null) {
            throw new IllegalArgumentException("No piece at start square " + start);
        }

        Piece capturedPiece = getPiece(end);

        // Handle en passant capture, where the captured piece is not on the 'end' square
        if (movingPiece instanceof Pawn && end.equals(enPassantTargetSquare)) {
            int capturedPawnRank = turn == Colour.WHITE ? end.rank() + 1 : end.rank() - 1;
            Square capturedPawnSquare = new Square(capturedPawnRank, end.file());
            capturedPiece = getPiece(capturedPawnSquare);
            setPiece(capturedPawnSquare, null);
        }

        // Handle castling by moving the rook as well
        if (movingPiece instanceof King && Math.abs(start.file() - end.file()) == 2) {
            handleRookCastleMove(start, end);
        }

        // Move the piece
        setPiece(end, movingPiece);
        setPiece(start, null);

        // Handle promotion
        if (movingPiece instanceof Pawn && (end.rank() == 0 || end.rank() == 7)) {
            String promo = promotionPiece.orElse("Q").toUpperCase();
            Piece promoted = switch (promo) {
                case "R" -> new Rook(turn);
                case "B" -> new Bishop(turn);
                case "N" -> new Knight(turn);
                default -> new Queen(turn);
            };
            setPiece(end, promoted);
        }

        // --- Update Board State AFTER the move ---
        updateCastlingRights(movingPiece, start, end);
        updateEnPassantTarget(movingPiece, start, end);
        advanceTurn();

        // After turn advances, check if the NEW player is in check
        this.kingInCheckSquare = isInCheck(this.turn) ? findKing(this.turn) : null;

        return capturedPiece;
    }


    public boolean applyMove(Move move) {
        // Handle castling as a special case first
        if (move.isKingsideCastle() || move.isQueensideCastle()) {
            return applyCastlingFromPGN(move);
        }

        Square targetSquare = new Square(8 - move.getTargetRank(), move.getTargetFile() - 'a');

        // Find the one piece that can legally make this move
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square startSquare = new Square(r, c);
                Piece piece = getPiece(startSquare);
                if (piece != null && piece.getColor() == turn &&
                        Character.toString(piece.getSymbol()).equalsIgnoreCase(move.getPiece())) {

                    // Filter out pieces that don't match the PGN's disambiguation rule
                    if (!isCorrectDisambiguation(move.getDisambiguation(), startSquare)) {
                        continue;
                    }
                    // Check if the move is fully legal (not leaving king in check)
                    if (isLegalMove(startSquare, targetSquare)) {
                        applyMove(startSquare, targetSquare, Optional.ofNullable(move.getPromotion()));
                        // After the move, update the move object with check/checkmate status
                        if (kingInCheckSquare != null) {
                            move.setCheck(true);
                            if (!hasAnyLegalMoves(turn)) {
                                move.setCheckmate(true);
                            }
                        }
                        return true;
                    }
                }
            }
        }
        System.err.println("ILLEGAL PGN MOVE: No piece found that can legally perform " + move.getRaw() + " for " + turn);
        return false;
    }

    public List<Square> getLegalMovesForPiece(Square start) {
        List<Square> legalMoves = new ArrayList<>();
        Piece piece = getPiece(start);
        if (piece == null || piece.getColor() != this.turn) {
            return legalMoves;
        }

        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square target = new Square(r, c);
                if (isLegalMove(start, target)) {
                    legalMoves.add(target);
                }
            }
        }
        return legalMoves;
    }

    public boolean isLegalMove(Square start, Square end) {
        Piece piece = getPiece(start);
        if (piece == null || piece.getColor() != turn) return false;
        if (start.equals(end)) return false;

        if (piece instanceof King && Math.abs(start.file() - end.file()) == 2) {
            return isCastleLegal(start, end);
        }

        // Check if it's a valid move pattern for the piece (pseudo-legal)
        if (!piece.isValidMove(start, end, this)) return false;

        // Prevent capturing your own piece
        Piece targetPiece = getPiece(end);
        if (targetPiece != null && targetPiece.getColor() == turn) return false;

        // Simulate the move to see if it leaves the king in check
        boolean leavesKingInCheck;
        setPiece(end, piece);
        setPiece(start, null);
        // Special case for en-passant undo
        Square enPassantCaptureSquare = null;
        Piece enPassantPawn = null;
        if (piece instanceof Pawn && end.equals(enPassantTargetSquare)) {
            int capturedPawnRank = turn == Colour.WHITE ? end.rank() + 1 : end.rank() - 1;
            enPassantCaptureSquare = new Square(capturedPawnRank, end.file());
            enPassantPawn = getPiece(enPassantCaptureSquare);
            setPiece(enPassantCaptureSquare, null);
        }

        leavesKingInCheck = isInCheck(turn);

        // Undo the move
        setPiece(start, piece);
        setPiece(end, targetPiece);
        if (enPassantPawn != null) {
            setPiece(enPassantCaptureSquare, enPassantPawn);
        }

        return !leavesKingInCheck;
    }


    private boolean isCastleLegal(Square kingStart, Square kingEnd) {
        if (isInCheck(turn)) return false; // Rule 1: Cannot castle while in check.

        Colour enemyColor = (turn == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
        int rank = kingStart.rank();

        // Kingside Castle
        if (kingEnd.file() > kingStart.file()) {
            boolean canKingside = (turn == Colour.WHITE) ? whiteKingsideCastleRight : blackKingsideCastleRight;
            if (!canKingside) return false;

            Square path1 = new Square(rank, 5); // f1/f8
            Square path2 = new Square(rank, 6); // g1/g8

            // Rule 2: Path must be clear
            if (getPiece(path1) != null || getPiece(path2) != null) return false;

            // Rule 3: Cannot pass through check
            return !isSquareAttackedBy(kingStart, enemyColor) &&
                    !isSquareAttackedBy(path1, enemyColor) &&
                    !isSquareAttackedBy(path2, enemyColor);
        }
        // Queenside Castle
        else {
            boolean canQueenside = (turn == Colour.WHITE) ? whiteQueensideCastleRight : blackQueensideCastleRight;
            if (!canQueenside) return false;

            Square path1 = new Square(rank, 3); // d1/d8
            Square path2 = new Square(rank, 2); // c1/c8
            Square path3 = new Square(rank, 1); // b1/b8

            // Rule 2: Path must be clear
            if (getPiece(path1) != null || getPiece(path2) != null || getPiece(path3) != null) return false;

            // Rule 3: Cannot pass through check
            return !isSquareAttackedBy(kingStart, enemyColor) &&
                    !isSquareAttackedBy(path1, enemyColor) &&
                    !isSquareAttackedBy(path2, enemyColor);
        }
    }

    // Checks if the given color has any legal moves available.
    public boolean hasAnyLegalMoves(Colour color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p != null && p.getColor() == color) {
                    // Temporarily set the turn to the color we are checking
                    Colour originalTurn = this.turn;
                    this.turn = color;
                    if (!getLegalMovesForPiece(new Square(r,c)).isEmpty()) {
                        this.turn = originalTurn; // Restore turn before returning
                        return true;
                    }
                    this.turn = originalTurn; // Restore turn
                }
            }
        }
        return false;
    }

    // Checks if the king of the specified color is currently under attack.
    public boolean isInCheck(Colour color) {
        Square kingSquare = findKing(color);
        if (kingSquare == null) {
            return true; // Should not happen in a valid game, but is a "checked" state.
        }
        return isSquareAttackedBy(kingSquare, (color == Colour.WHITE) ? Colour.BLACK : Colour.WHITE);
    }


    public Piece getPiece(Square sq) {
        if (sq == null || !sq.isValid()) return null;
        return grid[sq.rank()][sq.file()];
    }

    public void setPiece(Square sq, Piece piece) {
        if (sq != null && sq.isValid()) {
            grid[sq.rank()][sq.file()] = piece;
        }
    }

    public Colour getTurn() { return turn; }
    public Square getEnPassantTargetSquare() { return enPassantTargetSquare; }
    public Square getKingInCheckSquare() { return kingInCheckSquare; }

    public void setupInitialPosition() {
        for(int r=0; r<8; r++) for (int c = 0; c < 8; c++) grid[r][c] = null;

        this.turn = Colour.WHITE;
        this.enPassantTargetSquare = null;
        this.kingInCheckSquare = null;
        this.whiteKingsideCastleRight = true;
        this.whiteQueensideCastleRight = true;
        this.blackKingsideCastleRight = true;
        this.blackQueensideCastleRight = true;

        // Setup White pieces
        setPiece(Square.fromAlgebraic("a1"), new Rook(Colour.WHITE));
        setPiece(Square.fromAlgebraic("b1"), new Knight(Colour.WHITE));
        setPiece(Square.fromAlgebraic("c1"), new Bishop(Colour.WHITE));
        setPiece(Square.fromAlgebraic("d1"), new Queen(Colour.WHITE));
        setPiece(Square.fromAlgebraic("e1"), new King(Colour.WHITE));
        setPiece(Square.fromAlgebraic("f1"), new Bishop(Colour.WHITE));
        setPiece(Square.fromAlgebraic("g1"), new Knight(Colour.WHITE));
        setPiece(Square.fromAlgebraic("h1"), new Rook(Colour.WHITE));
        for (char file = 'a'; file <= 'h'; file++) setPiece(Square.fromAlgebraic(file + "2"), new Pawn(Colour.WHITE));

        // Setup Black pieces
        setPiece(Square.fromAlgebraic("a8"), new Rook(Colour.BLACK));
        setPiece(Square.fromAlgebraic("b8"), new Knight(Colour.BLACK));
        setPiece(Square.fromAlgebraic("c8"), new Bishop(Colour.BLACK));
        setPiece(Square.fromAlgebraic("d8"), new Queen(Colour.BLACK));
        setPiece(Square.fromAlgebraic("e8"), new King(Colour.BLACK));
        setPiece(Square.fromAlgebraic("f8"), new Bishop(Colour.BLACK));
        setPiece(Square.fromAlgebraic("g8"), new Knight(Colour.BLACK));
        setPiece(Square.fromAlgebraic("h8"), new Rook(Colour.BLACK));
        for (char file = 'a'; file <= 'h'; file++) setPiece(Square.fromAlgebraic(file + "7"), new Pawn(Colour.BLACK));
    }

    private void advanceTurn() {
        this.turn = (this.turn == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
    }

    private void handleRookCastleMove(Square kingStart, Square kingEnd) {
        // Kingside castle
        if (kingEnd.file() > kingStart.file()) {
            Square rookStart = new Square(kingStart.rank(), 7);
            Square rookEnd = new Square(kingStart.rank(), 5);
            setPiece(rookEnd, getPiece(rookStart));
            setPiece(rookStart, null);
        }
        // Queenside castle
        else {
            Square rookStart = new Square(kingStart.rank(), 0);
            Square rookEnd = new Square(kingStart.rank(), 3);
            setPiece(rookEnd, getPiece(rookStart));
            setPiece(rookStart, null);
        }
    }

    private boolean applyCastlingFromPGN(Move move) {
        int rank = (turn == Colour.WHITE) ? 7 : 0;
        Square kingStart = new Square(rank, 4);
        Square kingEnd = new Square(rank, move.isKingsideCastle() ? 6 : 2);

        // Use the same validation logic as the GUI
        List<Square> legalMoves = new ArrayList<>();
        addLegalCastlingMoves(legalMoves, kingStart);

        if (legalMoves.contains(kingEnd)) {
            applyMove(kingStart, kingEnd, Optional.empty());
            return true;
        }
        return false;
    }

    private void updateEnPassantTarget(Piece movingPiece, Square start, Square end) {
        if (movingPiece instanceof Pawn && Math.abs(start.rank() - end.rank()) == 2) {
            enPassantTargetSquare = new Square((start.rank() + end.rank()) / 2, start.file());
        } else {
            enPassantTargetSquare = null;
        }
    }

    private void updateCastlingRights(Piece movingPiece, Square start, Square end) {
        if (movingPiece instanceof King) {
            if (movingPiece.getColor() == Colour.WHITE) {
                whiteKingsideCastleRight = false;
                whiteQueensideCastleRight = false;
            } else {
                blackKingsideCastleRight = false;
                blackQueensideCastleRight = false;
            }
        }
        // If a rook moves from its starting square
        if (start.equals(Square.fromAlgebraic("h1"))) whiteKingsideCastleRight = false;
        if (start.equals(Square.fromAlgebraic("a1"))) whiteQueensideCastleRight = false;
        if (start.equals(Square.fromAlgebraic("h8"))) blackKingsideCastleRight = false;
        if (start.equals(Square.fromAlgebraic("a8"))) blackQueensideCastleRight = false;

        // If a rook is captured on its starting square
        if (end.equals(Square.fromAlgebraic("h1"))) whiteKingsideCastleRight = false;
        if (end.equals(Square.fromAlgebraic("a1"))) whiteQueensideCastleRight = false;
        if (end.equals(Square.fromAlgebraic("h8"))) blackKingsideCastleRight = false;
        if (end.equals(Square.fromAlgebraic("a8"))) blackQueensideCastleRight = false;
    }

    private Square findKing(Colour color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p instanceof King && p.getColor() == color) {
                    return new Square(r, c);
                }
            }
        }
        return null;
    }

    private boolean isSquareAttackedBy(Square square, Colour attackerColor) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square attackerSquare = new Square(r, c);
                Piece attacker = getPiece(attackerSquare);
                if (attacker != null && attacker.getColor() == attackerColor) {
                    // For pawns, a normal move is different from an attack. We must check specifically for attacks.
                    if (attacker instanceof Pawn) {
                        int dir = attacker.getColor() == Colour.WHITE ? -1 : 1;
                        if (square.rank() == r + dir && Math.abs(square.file() - c) == 1) {
                            return true;
                        }
                    } else if (attacker.isValidMove(attackerSquare, square, this)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addLegalCastlingMoves(List<Square> legalMoves, Square kingSquare) {
        Colour color = getPiece(kingSquare).getColor();
        Colour enemyColor = (color == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
        int rank = kingSquare.rank();

        // Rule 1: Cannot castle while in check.
        if (isInCheck(color)) {
            return;
        }

        // --- Check Kingside Castling (O-O) ---
        boolean canKingside = (color == Colour.WHITE) ? whiteKingsideCastleRight : blackKingsideCastleRight;
        if (canKingside) {
            Square rookSquare = new Square(rank, 7);
            // Check if a Rook is actually there and hasn't moved (covered by the boolean right)
            if (getPiece(rookSquare) instanceof Rook) {
                // Rule 2: Path must be clear.
                Square path1 = new Square(rank, 5); // f1/f8
                Square path2 = new Square(rank, 6); // g1/g8
                if (getPiece(path1) == null && getPiece(path2) == null) {
                    // Rule 3: King cannot pass through or land on an attacked square.
                    if (!isSquareAttackedBy(path1, enemyColor) && !isSquareAttackedBy(path2, enemyColor)) {
                        legalMoves.add(path2); // Add g1/g8 as a legal move destination
                    }
                }
            }
        }

        // --- Check Queenside Castling (O-O-O) ---
        boolean canQueenside = (color == Colour.WHITE) ? whiteQueensideCastleRight : blackQueensideCastleRight;
        if (canQueenside) {
            Square rookSquare = new Square(rank, 0);
            if (getPiece(rookSquare) instanceof Rook) {
                // Rule 2: Path must be clear.
                Square path1 = new Square(rank, 3); // d1/d8
                Square path2 = new Square(rank, 2); // c1/c8
                Square path3 = new Square(rank, 1); // b1/b8 (for queenside, path is longer)
                if (getPiece(path1) == null && getPiece(path2) == null && getPiece(path3) == null) {
                    // Rule 3: King cannot pass through or land on an attacked square.
                    if (!isSquareAttackedBy(path1, enemyColor) && !isSquareAttackedBy(path2, enemyColor)) {
                        // Note: b1/b8 is not checked as the king does not pass over it.
                        legalMoves.add(path2); // Add c1/c8 as a legal move destination
                    }
                }
            }
        }
    }

    private boolean isCorrectDisambiguation(String dis, Square pieceSquare) {
        if (dis == null || dis.isEmpty()) return true;

        if (dis.length() == 1) {
            char d = dis.charAt(0);
            if (Character.isDigit(d)) { // Disambiguation by rank
                return (8 - Character.getNumericValue(d)) == pieceSquare.rank();
            } else { // Disambiguation by file
                return (d - 'a') == pieceSquare.file();
            }
        } else if (dis.length() == 2) { // Disambiguation by full coordinate
            return Square.fromAlgebraic(dis).equals(pieceSquare);
        }
        return false;
    }

    /**
    * Updates the board's state from a FEN string.
    * This is primarily used by the client to synchronize its display board
    * with the state sent by the server.
    * @param fen The FEN string for piece placement.
    */
    public void updateFromFen(String fen) {
        // 1. Clear the current grid
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                this.grid[r][c] = null;
            }
        }

        // 2. Populate the grid from the FEN string
        String[] ranks = fen.split(" ")[0].split("/");
        for (int r = 0; r < ranks.length; r++) {
            String rankStr = ranks[r];
            int c = 0;
            for (char ch : rankStr.toCharArray()) {
                if (c >= 8) break;
                if (Character.isDigit(ch)) {
                    c += Character.getNumericValue(ch);
                } else {
                    Colour color = Character.isUpperCase(ch) ? Colour.WHITE : Colour.BLACK;
                    Piece piece = switch (Character.toLowerCase(ch)) {
                        case 'r' -> new Rook(color);
                        case 'n' -> new Knight(color);
                        case 'b' -> new Bishop(color);
                        case 'q' -> new Queen(color);
                        case 'k' -> new King(color);
                        case 'p' -> new Pawn(color);
                        default -> null;
                    };
                    this.grid[r][c] = piece;
                    c++;
                }
            }
        }

        String[] parts = fen.split(" ");
        if (parts.length > 1) {
            // 'w' for white's turn, 'b' for black's
            this.turn = parts[1].equals("w") ? Colour.WHITE : Colour.BLACK;
        }

        // Note: A full implementation would also parse and set the turn, castling rights, etc.
        // from the FEN string for perfect local validation. For now, this is sufficient for display.
        
    }

    public void setTurn(Colour turn) {
        this.turn = turn;
    }

}