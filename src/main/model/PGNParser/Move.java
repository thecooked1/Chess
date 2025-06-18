package main.model.PGNParser;

public class Move {
    private String piece = "P"; // Defaults to Pawn
    private char targetFile;
    private int targetRank;
    private boolean isCapture = false;
    private boolean isCheck = false;
    private boolean isCheckmate = false;
    private String disambiguation = "";
    private String promotion = null;
    private String raw;

    private boolean isKingsideCastle = false;
    private boolean isQueensideCastle = false;

    public Move(String raw) {
        this.raw = raw;
    }

    public String getPiece() { return piece; }
    public char getTargetFile() { return targetFile; }
    public int getTargetRank() { return targetRank; }
    public boolean isCapture() { return isCapture; }
    public boolean isCheck() { return isCheck; }
    public boolean isCheckmate() { return isCheckmate; }
    public String getDisambiguation() { return disambiguation; }
    public String getPromotion() { return promotion; }
    public String getRaw() { return raw; }
    public boolean isKingsideCastle() { return isKingsideCastle; }
    public boolean isQueensideCastle() { return isQueensideCastle; }

    public void setPiece(String piece) { this.piece = piece; }
    public void setTargetFile(char targetFile) { this.targetFile = targetFile; }
    public void setTargetRank(int targetRank) { this.targetRank = targetRank; }
    public void setCapture(boolean capture) { isCapture = capture; }
    public void setCheck(boolean check) { isCheck = check; }
    public void setCheckmate(boolean checkmate) { isCheckmate = checkmate; }
    public void setDisambiguation(String disambiguation) { this.disambiguation = disambiguation; }
    public void setPromotion(String promotion) { this.promotion = promotion; }
    public void setKingsideCastle(boolean kingsideCastle) { isKingsideCastle = kingsideCastle; }
    public void setQueensideCastle(boolean queensideCastle) { isQueensideCastle = queensideCastle; }

    public String getTargetSquare() {
        return "" + targetFile + targetRank;
    }

    @Override
    public String toString() {
        return raw;
    }
}