package main.model;

// Helper class to manage castling availability
public class CastlingRights {
    private boolean whiteKingSide;
    private boolean whiteQueenSide;
    private boolean blackKingSide;
    private boolean blackQueenSide;

    public CastlingRights(boolean wk, boolean wq, boolean bk, boolean bq) {
        this.whiteKingSide = wk;
        this.whiteQueenSide = wq;
        this.blackKingSide = bk;
        this.blackQueenSide = bq;
    }

    // Default constructor assumes all rights available initially
    public CastlingRights() {
        this(true, true, true, true);
    }

    // Copy constructor
    public CastlingRights(CastlingRights other) {
        this.whiteKingSide = other.whiteKingSide;
        this.whiteQueenSide = other.whiteQueenSide;
        this.blackKingSide = other.blackKingSide;
        this.blackQueenSide = other.blackQueenSide;
    }

    public boolean canCastle(Color color, boolean kingSide) {
        if (color == Color.WHITE) {
            return kingSide ? whiteKingSide : whiteQueenSide;
        } else {
            return kingSide ? blackKingSide : blackQueenSide;
        }
    }

    public void revokeKingSide(Color color) {
        if (color == Color.WHITE) whiteKingSide = false;
        else blackKingSide = false;
    }

    public void revokeQueenSide(Color color) {
        if (color == Color.WHITE) whiteQueenSide = false;
        else blackQueenSide = false;
    }

    public void revokeAll(Color color) {
        revokeKingSide(color);
        revokeQueenSide(color);
    }

    // Getters for easier state inspection if needed
    public boolean isWhiteKingSide() { return whiteKingSide; }
    public boolean isWhiteQueenSide() { return whiteQueenSide; }
    public boolean isBlackKingSide() { return blackKingSide; }
    public boolean isBlackQueenSide() { return blackQueenSide; }
}