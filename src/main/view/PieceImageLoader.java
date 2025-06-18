package main.view;
import main.model.pieces.Colour;
import main.model.pieces.Piece;
import main.model.pieces.Pawn;
import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class PieceImageLoader {
    private static final Map<String, Image> imageCache = new HashMap<>();
    private static final String RESOURCE_PATH = "/";

    /**
     * The primary method to get an image for a given piece object.
     */
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

    /**
     * Internal helper to load an image from resources and cache it.
     */
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

    /**
     * Helper for getting the application icon image.
     */
    public static Image getApplicationIcon() {
        // Create a temporary white pawn to get its image
        return getImage(new Pawn(Colour.WHITE));
    }

}