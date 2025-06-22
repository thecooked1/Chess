// main/model/Board/Board.java
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
    private Square kingInCheckSquare;

    private boolean whiteKingsideCastleRight = true;
    private boolean whiteQueensideCastleRight = true;
    private boolean blackKingsideCastleRight = true;
    private boolean blackQueensideCastleRight = true;

    public Board() {
        grid = new Piece[8][8];
        setupInitialPosition();
    }

    public String applyMove(Square start, Square end, Optional<String> promotionPiece) {
        Piece movingPiece = getPiece(start);
        if (movingPiece == null) {
            throw new IllegalArgumentException("No piece at start square " + start);
        }

        StringBuilder sanBuilder = new StringBuilder();

        if (movingPiece instanceof King && Math.abs(start.file() - end.file()) == 2) {
            sanBuilder.append(end.file() > start.file() ? "O-O" : "O-O-O");
        } else {
            if (!(movingPiece instanceof Pawn)) {
                sanBuilder.append(movingPiece.getSymbol());
            }
            sanBuilder.append(getDisambiguation(movingPiece, start, end));

            Piece capturedPiece = getPiece(end);
            boolean isEnPassant = movingPiece instanceof Pawn && end.equals(enPassantTargetSquare);
            if (capturedPiece != null || isEnPassant) {
                if (movingPiece instanceof Pawn) {
                    sanBuilder.append(start.fileAsChar());
                }
                sanBuilder.append('x');
            }

            sanBuilder.append(end.toAlgebraic());

            if (movingPiece instanceof Pawn && (end.rank() == 0 || end.rank() == 7)) {
                String promo = promotionPiece.orElse("Q").toUpperCase();
                sanBuilder.append("=").append(promo);
            }
        }

        internalApplyMove(start, end, promotionPiece);

        if (isInCheck(this.turn)) {
            if (hasAnyLegalMoves(this.turn)) {
                sanBuilder.append('+');
            } else {
                sanBuilder.append('#');
            }
        }

        return sanBuilder.toString();
    }

    private Piece internalApplyMove(Square start, Square end, Optional<String> promotionPiece) {
        Piece movingPiece = getPiece(start);
        Piece capturedPiece = getPiece(end);
        if (movingPiece instanceof Pawn && end.equals(enPassantTargetSquare)) {
            int capturedPawnRank = turn == Colour.WHITE ? end.rank() + 1 : end.rank() - 1;
            Square capturedPawnSquare = new Square(capturedPawnRank, end.file());
            capturedPiece = getPiece(capturedPawnSquare);
            setPiece(capturedPawnSquare, null);
        }
        if (movingPiece instanceof King && Math.abs(start.file() - end.file()) == 2) {
            handleRookCastleMove(start, end);
        }
        setPiece(end, movingPiece);
        setPiece(start, null);
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
        updateCastlingRights(movingPiece, start, end);
        updateEnPassantTarget(movingPiece, start, end);
        advanceTurn();
        this.kingInCheckSquare = isInCheck(this.turn) ? findKing(this.turn) : null;
        return capturedPiece;
    }

    private String getDisambiguation(Piece movingPiece, Square start, Square end) {
        if (movingPiece instanceof Pawn || movingPiece instanceof King) {
            return "";
        }
        List<Square> ambiguousSquares = new ArrayList<>();
        Colour originalTurn = this.turn;
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square otherSquare = new Square(r, c);
                Piece otherPiece = getPiece(otherSquare);
                if (!otherSquare.equals(start) && otherPiece != null && otherPiece.getClass().equals(movingPiece.getClass()) && otherPiece.getColor() == movingPiece.getColor()) {
                    this.turn = movingPiece.getColor();
                    boolean isLegal = isLegalMove(otherSquare, end);
                    this.turn = originalTurn;
                    if (isLegal) {
                        ambiguousSquares.add(otherSquare);
                    }
                }
            }
        }
        if (ambiguousSquares.isEmpty()) {
            return "";
        }
        boolean fileIsUnique = true;
        for (Square sq : ambiguousSquares) {
            if (sq.file() == start.file()) {
                fileIsUnique = false;
                break;
            }
        }
        if (fileIsUnique) {
            return String.valueOf(start.fileAsChar());
        }
        boolean rankIsUnique = true;
        for (Square sq : ambiguousSquares) {
            if (sq.file() == start.file()) { // Check only among pieces on the same file
                if (sq.rank() == start.rank()) {
                    rankIsUnique = false;
                    break;
                }
            }
        }
        if (rankIsUnique) {
            return String.valueOf(start.rankAsChar());
        }
        return start.toAlgebraic();
    }

    // ... all other methods are unchanged and correct, so I am omitting them for brevity ...
    public boolean applyMove(Move move) {
        if (move.isKingsideCastle() || move.isQueensideCastle()) {
            return applyCastlingFromPGN(move);
        }
        Square targetSquare = new Square(8 - move.getTargetRank(), move.getTargetFile() - 'a');
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square startSquare = new Square(r, c);
                Piece piece = getPiece(startSquare);
                if (piece != null && piece.getColor() == turn && Character.toString(piece.getSymbol()).equalsIgnoreCase(move.getPiece())) {
                    if (!isCorrectDisambiguation(move.getDisambiguation(), startSquare)) {
                        continue;
                    }
                    if (isLegalMove(startSquare, targetSquare)) {
                        internalApplyMove(startSquare, targetSquare, Optional.ofNullable(move.getPromotion()));
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

    public void printBoard() {
        System.out.println("  a b c d e f g h   Turn: " + turn);
        System.out.println(" +-----------------+");
        for (int r = 0; r < 8; r++) {
            System.out.print((8 - r) + "|");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                char symbol = (p == null) ? '.' : p.getSymbol();
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
        if (!piece.isValidMove(start, end, this)) return false;
        Piece targetPiece = getPiece(end);
        if (targetPiece != null && targetPiece.getColor() == turn) return false;
        boolean leavesKingInCheck;
        setPiece(end, piece);
        setPiece(start, null);
        Square enPassantCaptureSquare = null;
        Piece enPassantPawn = null;
        if (piece instanceof Pawn && end.equals(enPassantTargetSquare)) {
            int capturedPawnRank = turn == Colour.WHITE ? end.rank() + 1 : end.rank() - 1;
            enPassantCaptureSquare = new Square(capturedPawnRank, end.file());
            enPassantPawn = getPiece(enPassantCaptureSquare);
            setPiece(enPassantCaptureSquare, null);
        }
        leavesKingInCheck = isInCheck(turn);
        setPiece(start, piece);
        setPiece(end, targetPiece);
        if (enPassantPawn != null) {
            setPiece(enPassantCaptureSquare, enPassantPawn);
        }
        return !leavesKingInCheck;
    }

    private boolean isCastleLegal(Square kingStart, Square kingEnd) {
        if (isInCheck(turn)) return false;
        Colour enemyColor = (turn == Colour.WHITE) ? Colour.BLACK : Colour.WHITE;
        int rank = kingStart.rank();
        if (kingEnd.file() > kingStart.file()) {
            boolean canKingside = (turn == Colour.WHITE) ? whiteKingsideCastleRight : blackKingsideCastleRight;
            if (!canKingside) return false;
            Square path1 = new Square(rank, 5);
            Square path2 = new Square(rank, 6);
            if (getPiece(path1) != null || getPiece(path2) != null) return false;
            return !isSquareAttackedBy(kingStart, enemyColor) && !isSquareAttackedBy(path1, enemyColor) && !isSquareAttackedBy(path2, enemyColor);
        } else {
            boolean canQueenside = (turn == Colour.WHITE) ? whiteQueensideCastleRight : blackQueensideCastleRight;
            if (!canQueenside) return false;
            Square path1 = new Square(rank, 3);
            Square path2 = new Square(rank, 2);
            Square path3 = new Square(rank, 1);
            if (getPiece(path1) != null || getPiece(path2) != null || getPiece(path3) != null) return false;
            return !isSquareAttackedBy(kingStart, enemyColor) && !isSquareAttackedBy(path1, enemyColor) && !isSquareAttackedBy(path2, enemyColor);
        }
    }

    public boolean hasAnyLegalMoves(Colour color) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p != null && p.getColor() == color) {
                    Colour originalTurn = this.turn;
                    this.turn = color;
                    if (!getLegalMovesForPiece(new Square(r, c)).isEmpty()) {
                        this.turn = originalTurn;
                        return true;
                    }
                    this.turn = originalTurn;
                }
            }
        }
        return false;
    }

    public boolean isInCheck(Colour color) {
        Square kingSquare = findKing(color);
        if (kingSquare == null) {
            return true;
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

    public Colour getTurn() {
        return turn;
    }

    public Square getEnPassantTargetSquare() {
        return enPassantTargetSquare;
    }

    public Square getKingInCheckSquare() {
        return kingInCheckSquare;
    }

    public void setupInitialPosition() {
        for (int r = 0; r < 8; r++) for (int c = 0; c < 8; c++) grid[r][c] = null;
        this.turn = Colour.WHITE;
        this.enPassantTargetSquare = null;
        this.kingInCheckSquare = null;
        this.whiteKingsideCastleRight = true;
        this.whiteQueensideCastleRight = true;
        this.blackKingsideCastleRight = true;
        this.blackQueensideCastleRight = true;
        setPiece(Square.fromAlgebraic("a1"), new Rook(Colour.WHITE));
        setPiece(Square.fromAlgebraic("b1"), new Knight(Colour.WHITE));
        setPiece(Square.fromAlgebraic("c1"), new Bishop(Colour.WHITE));
        setPiece(Square.fromAlgebraic("d1"), new Queen(Colour.WHITE));
        setPiece(Square.fromAlgebraic("e1"), new King(Colour.WHITE));
        setPiece(Square.fromAlgebraic("f1"), new Bishop(Colour.WHITE));
        setPiece(Square.fromAlgebraic("g1"), new Knight(Colour.WHITE));
        setPiece(Square.fromAlgebraic("h1"), new Rook(Colour.WHITE));
        for (char file = 'a'; file <= 'h'; file++) setPiece(Square.fromAlgebraic(file + "2"), new Pawn(Colour.WHITE));
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
        if (kingEnd.file() > kingStart.file()) {
            Square rookStart = new Square(kingStart.rank(), 7);
            Square rookEnd = new Square(kingStart.rank(), 5);
            setPiece(rookEnd, getPiece(rookStart));
            setPiece(rookStart, null);
        } else {
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
        List<Square> legalMoves = new ArrayList<>();
        addLegalCastlingMoves(legalMoves, kingStart);
        if (legalMoves.contains(kingEnd)) {
            internalApplyMove(kingStart, kingEnd, Optional.empty());
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
        if (start.equals(Square.fromAlgebraic("h1"))) whiteKingsideCastleRight = false;
        if (start.equals(Square.fromAlgebraic("a1"))) whiteQueensideCastleRight = false;
        if (start.equals(Square.fromAlgebraic("h8"))) blackKingsideCastleRight = false;
        if (start.equals(Square.fromAlgebraic("a8"))) blackQueensideCastleRight = false;
        if (end.equals(Square.fromAlgebraic("h1"))) whiteKingsideCastleRight = false;
        if (end.equals(Square.fromAlgebraic("a1"))) whiteQueensideCastleRight = false;
        if (end.equals(Square.fromAlgebraic("h8"))) blackQueensideCastleRight = false;
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
        if (isInCheck(color)) {
            return;
        }
        boolean canKingside = (color == Colour.WHITE) ? whiteKingsideCastleRight : blackKingsideCastleRight;
        if (canKingside) {
            Square rookSquare = new Square(rank, 7);
            if (getPiece(rookSquare) instanceof Rook) {
                Square path1 = new Square(rank, 5);
                Square path2 = new Square(rank, 6);
                if (getPiece(path1) == null && getPiece(path2) == null) {
                    if (!isSquareAttackedBy(path1, enemyColor) && !isSquareAttackedBy(path2, enemyColor)) {
                        legalMoves.add(path2);
                    }
                }
            }
        }
        boolean canQueenside = (color == Colour.WHITE) ? whiteQueensideCastleRight : blackQueensideCastleRight;
        if (canQueenside) {
            Square rookSquare = new Square(rank, 0);
            if (getPiece(rookSquare) instanceof Rook) {
                Square path1 = new Square(rank, 3);
                Square path2 = new Square(rank, 2);
                Square path3 = new Square(rank, 1);
                if (getPiece(path1) == null && getPiece(path2) == null && getPiece(path3) == null) {
                    if (!isSquareAttackedBy(path1, enemyColor) && !isSquareAttackedBy(path2, enemyColor)) {
                        legalMoves.add(path2);
                    }
                }
            }
        }
    }

    private boolean isCorrectDisambiguation(String dis, Square pieceSquare) {
        if (dis == null || dis.isEmpty()) return true;
        if (dis.length() == 1) {
            char d = dis.charAt(0);
            if (Character.isDigit(d)) {
                return (8 - Character.getNumericValue(d)) == pieceSquare.rank();
            } else {
                return (d - 'a') == pieceSquare.file();
            }
        } else if (dis.length() == 2) {
            return Square.fromAlgebraic(dis).equals(pieceSquare);
        }
        return false;
    }

    public void updateFromFen(String fen) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                this.grid[r][c] = null;
            }
        }
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
            this.turn = parts[1].equals("w") ? Colour.WHITE : Colour.BLACK;
        }
    }

    public void setTurn(Colour turn) {
        this.turn = turn;
    }
}