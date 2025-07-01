package com.chess.model.PGNParser;

public class Interpreter {
    public Move parseMove(String token) {
        Move move = new Move(token);

        String t = token;

        if (t.contains("+")) {
            move.setCheck(true);
            t = t.replace("+", "");
        } else if (t.contains("#")) {
            move.setCheckmate(true);
            t = t.replace("#", "");
        }

        if (t.contains("=")) {
            int idx = t.indexOf("=");
            move.setPromotion(t.substring(idx + 1));
            t = t.substring(0, idx);
        }

        if (t.equals("O-O")) { // KingSide
            move.setPiece("K"); // King is the piece moving
            move.setKingsideCastle(true);
            move.setTargetFile('g');
            move.setTargetRank(-1);
            return move;
        } else if (t.equals("O-O-O")) { // QueenSide
            move.setPiece("K");
            move.setQueensideCastle(true);
            move.setTargetFile('c');
            move.setTargetRank(-1);
            return move;
        }

        move.setCapture(t.contains("x"));
        t = t.replace("x", "");

        if ("KQRBN".indexOf(t.charAt(0)) != -1) {
            move.setPiece(String.valueOf(t.charAt(0)));
            t = t.substring(1);
        } else {
            move.setPiece("P");
        }

        // Disambiguation
        int len = t.length();
        if (len == 3) {
            move.setDisambiguation(String.valueOf(t.charAt(0)));
            move.setTargetFile(t.charAt(1));
            move.setTargetRank(Character.getNumericValue(t.charAt(2)));
        } else if (len == 2) {
            move.setTargetFile(t.charAt(0));
            move.setTargetRank(Character.getNumericValue(t.charAt(1)));
        } else if (len == 4) {
            // full disambiguation
            move.setDisambiguation(t.substring(0, 2));
            move.setTargetFile(t.charAt(2));
            move.setTargetRank(Character.getNumericValue(t.charAt(3)));
        } else {
            throw new IllegalArgumentException("Unrecognized move format: " + token);
        }


        return move;
    }
}