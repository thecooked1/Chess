package model;

import java.util.Objects;

/**
 * Represents a single move in the chess game.
 * Stores the start and end squares, the piece moved, and potentially captured piece or promotion type.
 */
public class Move {
    private final Square startSquare;
    private final Square endSquare;
    private final Piece pieceMoved;
    private final Piece pieceCaptured; // Can be null if no capture
    private final PieceType promotionType; // Can be null if not a promotion
    private final boolean isCastleKingside;
    private final boolean isCastleQueenside;
    private final boolean isEnPassantCapture; // True if this move captures via en passant

    // Constructor for standard moves (including captures)
    public Move(Board board, Square start, Square end) {
        this(start, end, start.getOccupyingPiece(), end.getOccupyingPiece(), null, false, false, false);

        // Detect En Passant capture based on board state *before* move simulation
        if (pieceMoved != null && pieceMoved.getType() == PieceType.PAWN &&
                start.getFile() != end.getFile() && // Diagonal move
                pieceCaptured == null && // But no piece on the destination square
                board.getGameState() != null &&
                end.equals(board.getGameState().getEnPassantTargetSquare())) {
            // Find the actual pawn captured en passant
            int capturedPawnRank = (pieceMoved.getColor() == PieceColor.WHITE) ? end.getRank() - 1 : end.getRank() + 1;
            Piece actualCapturedPawn = board.getPiece(end.getFile(), capturedPawnRank);
            // Re-create the move object with the correct captured piece and flag
            this.reInitializeAsEnPassant(start, end, pieceMoved, actualCapturedPawn);
            return; // Important: exit constructor early after re-initialization
        }

        // Detect Castling
        if (pieceMoved != null && pieceMoved.getType() == PieceType.KING) {
            int fileDiff = end.getFile() - start.getFile();
            if (Math.abs(fileDiff) == 2) {
                if (fileDiff == 2) { // Kingside
                    this.reInitializeAsCastle(start, end, pieceMoved, true, false);
                } else { // Queenside
                    this.reInitializeAsCastle(start, end, pieceMoved, false, true);
                }
            }
        }
    }

    // Constructor for promotion moves
    public Move(Square start, Square end, Piece pawn, Piece captured, PieceType promotion) {
        this(start, end, pawn, captured, promotion, false, false, false);
        if (pawn == null || pawn.getType() != PieceType.PAWN || promotion == null) {
            throw new IllegalArgumentException("Invalid promotion move parameters.");
        }
        if (promotion == PieceType.KING || promotion == PieceType.PAWN) {
            throw new IllegalArgumentException("Cannot promote to King or Pawn.");
        }
    }

    // Private constructor used by others and re-initialization methods
    private Move(Square start, Square end, Piece moved, Piece captured, PieceType promo, boolean kCastle, boolean qCastle, boolean ep) {
        if (start == null || end == null || moved == null) {
            throw new IllegalArgumentException("Start square, end square, and moved piece cannot be null.");
        }
        if (start.equals(end)) {
            throw new IllegalArgumentException("Start and end squares cannot be the same.");
        }
        if(moved.getPosition() != start) {
            // This can happen if the Move is created before the piece is logically on the start square (e.g. during validation)
            // System.err.println("Warning: Piece being moved is not on the start square specified in the Move object.");
        }

        this.startSquare = start;
        this.endSquare = end;
        this.pieceMoved = moved;
        this.pieceCaptured = captured;
        this.promotionType = promo;
        this.isCastleKingside = kCastle;
        this.isCastleQueenside = qCastle;
        this.isEnPassantCapture = ep;

        // Sanity checks
        if ((kCastle || qCastle) && moved.getType() != PieceType.KING) {
            throw new IllegalArgumentException("Only Kings can castle.");
        }
        if (ep && (moved.getType() != PieceType.PAWN || captured == null)) {
            throw new IllegalArgumentException("En passant requires a Pawn capturing a Pawn.");
        }
        if (promo != null && moved.getType() != PieceType.PAWN) {
            throw new IllegalArgumentException("Only Pawns can promote.");
        }
    }

    // Helper to re-initialize when an en passant is detected
    private void reInitializeAsEnPassant(Square start, Square end, Piece moved, Piece captured) {
        // Effectively calls the private constructor again with correct flags/pieces
        new Move(start, end, moved, captured, null, false, false, true);
    }
    // Helper to re-initialize when castling is detected
    private void reInitializeAsCastle(Square start, Square end, Piece moved, boolean kCastle, boolean qCastle) {
        // Effectively calls the private constructor again with correct flags
        new Move(start, end, moved, null, null, kCastle, qCastle, false);
    }


    public Square getStartSquare() {
        return startSquare;
    }

    public Square getEndSquare() {
        return endSquare;
    }

    public Piece getPieceMoved() {
        return pieceMoved;
    }

    public Piece getPieceCaptured() {
        return pieceCaptured;
    }

    public boolean isCapture() {
        return pieceCaptured != null;
    }

    public PieceType getPromotionType() {
        return promotionType;
    }

    public boolean isPromotion() {
        return promotionType != null;
    }

    public boolean isCastleKingside() {
        return isCastleKingside;
    }

    public boolean isCastleQueenside() {
        return isCastleQueenside;
    }

    public boolean isCastle() {
        return isCastleKingside || isCastleQueenside;
    }

    public boolean isEnPassantCapture() {
        return isEnPassantCapture;
    }

    @Override
    public String toString() {
        // Basic algebraic notation (simplistic, doesn't handle disambiguation or checks)
        StringBuilder sb = new StringBuilder();
        if (isCastleKingside) return "O-O";
        if (isCastleQueenside) return "O-O-O";

        if (pieceMoved.getType() != PieceType.PAWN) {
            sb.append(getPieceChar(pieceMoved.getType()));
        }
        // Could add start file/rank for disambiguation if needed
        if (isCapture()) {
            if (pieceMoved.getType() == PieceType.PAWN) {
                sb.append(startSquare.toString().charAt(0)); // Pawn capture includes start file
            }
            sb.append("x");
        }
        sb.append(endSquare.toString());
        if (isPromotion()) {
            sb.append("=").append(getPieceChar(promotionType));
        }
        // Could add + for check or # for checkmate if known
        return sb.toString();
    }

    private char getPieceChar(PieceType type) {
        switch (type) {
            case ROOK: return 'R';
            case KNIGHT: return 'N';
            case BISHOP: return 'B';
            case QUEEN: return 'Q';
            case KING: return 'K';
            case PAWN: return 'P'; // Usually omitted, but useful here
            default: return '?';
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return isCastleKingside == move.isCastleKingside &&
                isCastleQueenside == move.isCastleQueenside &&
                isEnPassantCapture == move.isEnPassantCapture &&
                Objects.equals(startSquare, move.startSquare) &&
                Objects.equals(endSquare, move.endSquare) &&
                Objects.equals(pieceMoved, move.pieceMoved) && // Note: Piece equality includes position
                Objects.equals(pieceCaptured, move.pieceCaptured) &&
                promotionType == move.promotionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startSquare, endSquare, pieceMoved, pieceCaptured, promotionType, isCastleKingside, isCastleQueenside, isEnPassantCapture);
    }
}