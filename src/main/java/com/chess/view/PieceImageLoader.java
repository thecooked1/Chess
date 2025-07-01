package com.chess.view;
import com.chess.common.Colour;
import com.chess.model.pieces.Piece;
import com.chess.model.pieces.Pawn;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PieceImageLoader {
    private static final Map<String, Image> imageCache = new HashMap<>();
    private static final String RESOURCE_PATH = "/";

    public static Image getImage(Piece piece) {
        if (piece == null) return null;

        char pieceSymbol = piece.getSymbol();
        Colour pieceColor = piece.getColor();

        String typeSuffix = "";
        switch (Character.toLowerCase(pieceSymbol)) {
            case 'p': typeSuffix = "pawn"; break;
            case 'r': typeSuffix = "rook"; break;
            case 'n': typeSuffix = "knight"; break; // 'n' for knight
            case 'b': typeSuffix = "bishop"; break;
            case 'q': typeSuffix = "queen"; break;
            case 'k': typeSuffix = "king"; break;
            default: return null; // Unknown piece symbol
        }

        char colorPrefix = (pieceColor == Colour.WHITE) ? 'w' : 'b';
        String fileName = colorPrefix + typeSuffix + ".png";
        String key = fileName;

        if (!imageCache.containsKey(key)) {
            loadImage(key, fileName);
        }
        return imageCache.get(key);
    }


    private static void loadImage(String key, String fileName) {
        try {
            String fullPath = RESOURCE_PATH + fileName;
            InputStream is = PieceImageLoader.class.getResourceAsStream(fullPath);
            if (is == null) {
                System.err.println("FAILED to find resource stream for: " + fullPath);
                throw new IOException("Resource not found in classpath: " + fullPath);
            }
            Image img = ImageIO.read(is);
            imageCache.put(key, img);
            is.close();
        } catch (IOException e) {
            System.err.println("Failed to load image: " + fileName + " - " + e.getMessage());
            imageCache.put(key, null);
        }
    }

    public static Image getApplicationIcon() {
        // Create a temporary white pawn to get its image
        return getImage(new Pawn(Colour.WHITE));
    }

}