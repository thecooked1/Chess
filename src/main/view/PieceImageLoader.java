package main.view;

import main.model.Color;
import main.model.PieceType;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PieceImageLoader {
    private static final Map<String, Image> imageCache = new HashMap<>();
    private static final String RESOURCE_PATH = "/"; // Assuming resources are at the root of the classpath

    public static Image getImage(PieceType type, Color color) {
        String key = generateKey(type, color);
        if (!imageCache.containsKey(key)) {
            loadImage(key, getFileName(type, color));
        }
        return imageCache.get(key);
    }

    private static void loadImage(String key, String fileName) {
        try {
            String fullPath = RESOURCE_PATH + fileName;
            System.out.println("Attempting to load resource: " + fullPath); // DEBUG
            InputStream is = PieceImageLoader.class.getResourceAsStream(fullPath);
            if (is == null) {
                // More specific error:
                System.err.println("FAILED to find resource stream for: " + fullPath + ". Check classpath and build resource copying.");
                throw new IOException("Resource not found in classpath: " + fullPath);
            }
            Image img = ImageIO.read(is);
            imageCache.put(key, img);
            is.close();
            System.out.println("Successfully loaded: " + fileName); // DEBUG
        } catch (IOException e) {
            System.err.println("Failed to load image: " + fileName + " - " + e.getMessage());
            // Optionally put a placeholder image or throw an exception
            imageCache.put(key, null); // Or some default error image
        }
    }

    private static String generateKey(PieceType type, Color color) {
        return color.name().toLowerCase() + "_" + type.name().toLowerCase();
    }

    private static String getFileName(PieceType type, Color color) {
        char colorPrefix = (color == Color.WHITE) ? 'w' : 'b';
        String typeSuffix = "";
        switch (type) {
            case PAWN: typeSuffix = "pawn"; break;
            case ROOK: typeSuffix = "rook"; break;
            case KNIGHT: typeSuffix = "knight"; break;
            case BISHOP: typeSuffix = "bishop"; break;
            case QUEEN: typeSuffix = "queen"; break;
            case KING: typeSuffix = "king"; break;
        }
        return colorPrefix + typeSuffix + ".png";
        // Legacy names: wp.png, bp.png, wrook.png, brook.png etc. Adjust if needed.
            /* Example legacy mapping:
             switch (type) {
                case PAWN:   return colorPrefix + "pawn.png"; // wpawn.png, bpawn.png
                case ROOK:   return colorPrefix + "rook.png"; // wrook.png, brook.png
                case KNIGHT: return colorPrefix + "knight.png"; // wknight.png, bknight.png
                case BISHOP: return colorPrefix + "bishop.png"; // wbishop.png, bbishop.png
                case QUEEN:  return colorPrefix + "queen.png"; // wqueen.png, bqueen.png
                case KING:   return colorPrefix + "king.png"; // wking.png, bking.png
                default:     return "error.png"; // Should not happen
             }
             */
    }

    // Helper for getting the application icon image (if needed)
    public static Image getApplicationIcon() {
        String key = "icon_wp";
        if (!imageCache.containsKey(key)) {
            // Assuming wp.png or similar can be used as an icon
            loadImage(key, "wpawn.png"); // Use a standard piece image like white pawn
        }
        return imageCache.get(key);
    }

    public static Image getImage(PieceType wpawn, java.awt.Color wpawn1) {
        return null;
    }
}