package main.model.PGNParser;
import java.io.*;
import java.util.*;

public class Parser {

    public static class PGNGame {
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final List<String> moves = new ArrayList<>();
        private String result;

        public Map<String, String> getHeaders() { return headers; }
        public List<String> getMoves() { return moves; }
        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }
        public void addMove(String move) { this.moves.add(move); }

        public void addHeader(String key, String value) {
            headers.put(key, value);
        }

        @Override
        public String toString() {
            return "Headers: " + headers + "\nMoves: " + moves + "\nResult: " + result;
        }
    }

    private final List<PGNGame> games = new ArrayList<>();

    public void loadPGN(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            loadFromReader(reader);
        }
    }

    public void loadPGNFromString(String pgnContent) throws IOException {
        try (BufferedReader reader = new BufferedReader(new StringReader(pgnContent))) {
            loadFromReader(reader);
        }
    }

    private void loadFromReader(BufferedReader reader) throws IOException {
        games.clear();
        String line;
        PGNGame currentGame = null;
        StringBuilder movesSection = new StringBuilder();
        boolean readingHeaders = false;
        boolean addedLastGame = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            if (line.isEmpty()) {
                if (readingHeaders && currentGame != null && movesSection.length() == 0) {
                    readingHeaders = false;
                }
                continue;
            }

            if (line.startsWith("[")) {
                // Start of a header line
                if (currentGame != null && movesSection.length() > 0) {
                    parseMoves(movesSection.toString(), currentGame);
                    if (!addedLastGame) games.add(currentGame);
                    addedLastGame = true;
                } else if (currentGame != null && !readingHeaders && !addedLastGame) {
                    // Headers ended, but no moves/result found before next game's headers started
                    if (!games.contains(currentGame)) {
                        games.add(currentGame);
                        addedLastGame = true;
                    }
                }
                // Reset
                if (currentGame == null || movesSection.length() > 0 || !readingHeaders) {
                    currentGame = new PGNGame();
                    movesSection.setLength(0);
                    readingHeaders = true;
                    addedLastGame = false;
                }
                parseHeader(line, currentGame);

            } else if (currentGame != null) {
                readingHeaders = false;
                movesSection.append(line).append(" ");
                addedLastGame = false;
            } else {
                System.err.println("Warning: Ignoring text found before the first PGN game header: " + line);
            }
        }

        if (currentGame != null && !addedLastGame) {
            if (movesSection.length() > 0) {
                parseMoves(movesSection.toString(), currentGame);
            }
            games.add(currentGame);
        }
    }

    private void parseHeader(String line, PGNGame game) {
        int firstQuote = line.indexOf("\"");
        int lastQuote = line.lastIndexOf("\"");
        if (firstQuote < 0 || lastQuote < 0 || firstQuote == lastQuote) return;

        String key = line.substring(1, line.indexOf(" ", 1)).trim();
        String value = line.substring(firstQuote + 1, lastQuote).trim();
        game.addHeader(key, value);
    }

    private void parseMoves(String moveSection, PGNGame game) {
        // Remove comments and NAGs
        moveSection = moveSection.replaceAll("\\{[^}]*}", ""); // Remove comments {}
        while (moveSection.contains("(")) {
            int open = moveSection.indexOf('(');
            int close = findMatchingParen(moveSection, open);
            if (close != -1) {
                moveSection = moveSection.substring(0, open).trim() + " " + moveSection.substring(close + 1).trim();
            } else {
                moveSection = moveSection.substring(0, open).trim(); // Remove potentially broken part
                System.err.println("Warning: Malformed variation found in game, potential parsing issues.");
                break;
            }
            moveSection = moveSection.replaceAll("\\s+", " ").trim();
        }
        moveSection = moveSection.replaceAll("\\$\\d+", ""); // remove NAGs

        moveSection = moveSection.replaceAll("\\s+", " ").trim();
        String[] tokens = moveSection.split(" ");

        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }

            // Skip move numbers
            if (token.matches("\\d+\\.+")) {
                continue;
            }

            // Check for game termination
            if (token.equals("1-0") || token.equals("0-1") || token.equals("1/2-1/2") || token.equals("*")) {
                game.setResult(token);
                break;
            } else {
                if (!token.matches("\\d+\\.\\.\\.")) {
                    game.addMove(token);
                }
            }
        }
    }

    // Helper to find matching parenthesis
    private int findMatchingParen(String text, int openPos) {
        int closePos = openPos;
        int counter = 1;
        while (counter > 0 && ++closePos < text.length()) {
            if (text.charAt(closePos) == '(') {
                counter++;
            } else if (text.charAt(closePos) == ')') {
                counter--;
            }
        }
        return (counter == 0) ? closePos : -1;
    }

    public List<PGNGame> getGames() {
        return games;
    }
}
