package view;

import model.*;
import controller.GameController; // Needs controller reference

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

public class BoardPanel extends JPanel {

    private static final int BOARD_SIZE = 8;
    private static final Color LIGHT_SQUARE_COLOR = new Color(221, 192, 127); // Light Wood
    private static final Color DARK_SQUARE_COLOR = new Color(101, 67, 33);  // Dark Wood
    private static final Color HIGHLIGHT_COLOR = new Color(118, 150, 86, 150); // Semi-transparent green for legal moves
    private static final Color PRE_MOVE_HIGHLIGHT_COLOR = new Color(255, 255, 0, 100); // Semi-transparent yellow
    private static final String RESOURCES_WQUEEN_PNG = "wqueen.png";
    private static final String RESOURCES_BQUEEN_PNG = "bqueen.png";
    private static final String RESOURCES_WROOK_PNG = "wrook.png";
    private static final String RESOURCES_BROOK_PNG = "brook.png";
    private static final String RESOURCES_WBISHOP_PNG = "wbishop.png";
    private static final String RESOURCES_BBISHOP_PNG = "bbishop.png";
    private static final String RESOURCES_WKNIGHT_PNG = "wknight.png";
    private static final String RESOURCES_BKNIGHT_PNG = "bknight.png";

    private final GameController controller;
    private final Board boardModel; // Reference to the model's board
    private final Map<String, BufferedImage> pieceImages;

    private Piece selectedPiece = null; // Piece being dragged
    private Point dragPoint = null;     // Current mouse position during drag
    private Square sourceSquare = null; // Square where dragging started

    private List<Square> legalMoveSquares = null; // Cache legal moves for highlighting

    private int squareSize; // Calculated based on panel size

    public BoardPanel(GameController controller, Board boardModel) {
        this.controller = controller;
        this.boardModel = boardModel;
        this.pieceImages = new HashMap<>();

        loadPieceImages();

        BoardMouseListener mouseListener = new BoardMouseListener();
        addMouseListener(mouseListener);
        addMouseMotionListener(mouseListener);

        // Set preferred size - adjust as needed
        setPreferredSize(new Dimension(500, 500));
    }

    private void loadPieceImages() {
        // Iterate through all pieces currently on the board to get their image filenames
        for (Piece piece : boardModel.getAllPieces()) {
            String filename = piece.getImageFileName();
            if (!pieceImages.containsKey(filename)) {
                try {
                    // Load from resources folder using ClassLoader
                    URL imageUrl = getClass().getClassLoader().getResource(filename);
                    if (imageUrl == null) {
                        // Try adding "resources/" prefix if not found directly
                        imageUrl = getClass().getClassLoader().getResource("resources/" + filename);
                    }

                    if (imageUrl != null) {
                        pieceImages.put(filename, ImageIO.read(imageUrl));
                        System.out.println("Loaded image: " + filename);
                    } else {
                        System.err.println("Error loading image resource: " + filename);
                        // Put a placeholder or handle error?
                    }
                } catch (IOException e) {
                    System.err.println("Error loading image " + filename + ": " + e.getMessage());
                }
            }
        }
        // Also load images for potential promotions (if not already loaded)
        String[] potentialFiles = {"wqueen.png", "bqueen.png", "wrook.png", "brook.png",
                "wbishop.png", "bbishop.png", "wknight.png", "bknight.png"};
        for (String filename : potentialFiles) {
            if (!pieceImages.containsKey(filename)) {
                try {
                    URL imageUrl = getClass().getClassLoader().getResource(filename);
                    if (imageUrl == null) imageUrl = getClass().getClassLoader().getResource("resources/" + filename);

                    if (imageUrl != null) {
                        pieceImages.put(filename, ImageIO.read(imageUrl));
                        System.out.println("Loaded promotion image: " + filename);
                    } else {
                        System.err.println("Error loading image resource: " + filename);
                    }
                } catch (IOException e) {
                    System.err.println("Error loading image " + filename + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Calculate square size dynamically
        int width = getWidth();
        int height = getHeight();
        squareSize = Math.min(width, height) / BOARD_SIZE;

        // Center the board if panel is larger than board
        int boardPixelSize = squareSize * BOARD_SIZE;
        int offsetX = (width - boardPixelSize) / 2;
        int offsetY = (height - boardPixelSize) / 2;

        // Draw squares
        for (int rank = 0; rank < BOARD_SIZE; rank++) {
            for (int file = 0; file < BOARD_SIZE; file++) {
                // Flip rank for drawing (0,0 is top-left)
                int drawRank = BOARD_SIZE - 1 - rank;
                int x = offsetX + file * squareSize;
                int y = offsetY + drawRank * squareSize;

                // Determine square color
                boolean isLightSquare = (file + rank) % 2 != 0;
                g2d.setColor(isLightSquare ? LIGHT_SQUARE_COLOR : DARK_SQUARE_COLOR);
                g2d.fillRect(x, y, squareSize, squareSize);

                // Highlight last move source/destination squares
                Move lastMove = boardModel.getGameState().getLastMove();
                if (lastMove != null) {
                    Square modelStart = lastMove.getStartSquare();
                    Square modelEnd = lastMove.getEndSquare();
                    if ((modelStart.getFile() == file && modelStart.getRank() == rank) ||
                            (modelEnd.getFile() == file && modelEnd.getRank() == rank)) {
                        g2d.setColor(PRE_MOVE_HIGHLIGHT_COLOR);
                        g2d.fillRect(x, y, squareSize, squareSize);
                    }
                }

                // Highlight legal move destinations for selected piece
                if (legalMoveSquares != null) {
                    Square currentSquare = boardModel.getSquare(file, rank); // Get model square
                    if (legalMoveSquares.contains(currentSquare)) {
                        g2d.setColor(HIGHLIGHT_COLOR);
                        // Draw a circle or fill square slightly smaller? Let's fill.
                        // g2d.fillOval(x + squareSize / 4, y + squareSize / 4, squareSize / 2, squareSize / 2);
                        g2d.fillRect(x, y, squareSize, squareSize); // Fill whole square for simplicity
                    }
                }

                // Draw pieces (except the one being dragged)
                Piece piece = boardModel.getPiece(file, rank);
                if (piece != null && piece != selectedPiece) {
                    drawPiece(g2d, piece, x, y);
                }
            }
        }

        // Draw algebraic notation labels (optional)
        g2d.setColor(Color.BLACK);
        for(int file=0; file < BOARD_SIZE; file++) {
            g2d.drawString(String.valueOf((char)('a' + file)), offsetX + file * squareSize + squareSize/2 - 4, offsetY + boardPixelSize + 15);
        }
        for(int rank=0; rank < BOARD_SIZE; rank++) {
            g2d.drawString(String.valueOf(rank + 1), offsetX - 15, offsetY + (BOARD_SIZE - 1 - rank) * squareSize + squareSize/2 + 4);
        }


        // Draw the dragged piece last at the mouse cursor position
        if (selectedPiece != null && dragPoint != null) {
            BufferedImage img = pieceImages.get(selectedPiece.getImageFileName());
            if (img != null) {
                // Center image on cursor
                int imgX = dragPoint.x - squareSize / 2;
                int imgY = dragPoint.y - squareSize / 2;
                g2d.drawImage(img, imgX, imgY, squareSize, squareSize, null);
            }
        }
    }

    private void drawPiece(Graphics2D g2d, Piece piece, int x, int y) {
        BufferedImage img = pieceImages.get(piece.getImageFileName());
        if (img != null) {
            g2d.drawImage(img, x, y, squareSize, squareSize, null);
        } else {
            // Draw placeholder if image missing
            g2d.setColor(piece.getColor() == PieceColor.WHITE ? Color.LIGHT_GRAY : Color.DARK_GRAY);
            g2d.fillRect(x + 5, y + 5, squareSize - 10, squareSize - 10);
            g2d.setColor(Color.RED);
            g2d.drawString("?", x + squareSize / 2 - 4, y + squareSize / 2 + 5);
        }
    }

    // Converts a pixel point to board coordinates (file, rank)
    private Square pointToSquare(Point p) {
        if (squareSize <= 0) return null; // Avoid division by zero if panel not sized yet

        int boardPixelSize = squareSize * BOARD_SIZE;
        int offsetX = (getWidth() - boardPixelSize) / 2;
        int offsetY = (getHeight() - boardPixelSize) / 2;

        if (p.x < offsetX || p.x >= offsetX + boardPixelSize ||
                p.y < offsetY || p.y >= offsetY + boardPixelSize) {
            return null; // Outside board bounds
        }

        int file = (p.x - offsetX) / squareSize;
        int rank = BOARD_SIZE - 1 - ((p.y - offsetY) / squareSize); // Flip rank

        if (Board.isValidCoordinate(file, rank)) {
            return boardModel.getSquare(file, rank);
        } else {
            return null;
        }
    }

    // Called by the controller or main view to update the display
    public void updateBoard() {
        // Ensure images are loaded for any new pieces (e.g., promotions)
        loadPieceImages();
        // Clear selection highlights if needed (e.g., after opponent moves)
        // selectedPiece = null; // Keep selection based on user interaction? Let controller manage this.
        // dragPoint = null;
        // sourceSquare = null;
        legalMoveSquares = null; // Clear legal moves cache
        repaint();
    }

    // --- Mouse Listener ---
    private class BoardMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (boardModel.getGameState().isGameOver()) return; // No interaction if game over

            Point point = e.getPoint();
            Square clickedSquare = pointToSquare(point);
            if (clickedSquare == null) return;

            Piece clickedPiece = clickedSquare.getOccupyingPiece();

            // Only allow picking up pieces of the current player's color
            if (clickedPiece != null && clickedPiece.getColor() == boardModel.getGameState().getCurrentPlayerTurn()) {
                selectedPiece = clickedPiece;
                sourceSquare = clickedSquare;
                dragPoint = point;
                // Ask controller for legal moves for this piece to highlight them
                legalMoveSquares = controller.getLegalMovesForPiece(selectedPiece);
                repaint();
            } else {
                // Clicked on empty square or opponent's piece
                selectedPiece = null;
                sourceSquare = null;
                dragPoint = null;
                legalMoveSquares = null;
                repaint();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selectedPiece != null) {
                dragPoint = e.getPoint();
                repaint(); // Redraw to show piece following mouse
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (sourceSquare != null && selectedPiece != null) {
                Point point = e.getPoint();
                Square targetSquare = pointToSquare(point);

                if (targetSquare != null && !targetSquare.equals(sourceSquare)) {
                    // Tell the controller to attempt the move
                    controller.handleMoveAttempt(sourceSquare, targetSquare);
                }
                // else: Dropped outside board or on the same square - cancel move

                // Clear selection state regardless of move success (controller handles update)
                selectedPiece = null;
                sourceSquare = null;
                dragPoint = null;
                legalMoveSquares = null; // Clear highlights
                repaint(); // Redraw in final state (controller might trigger another update soon)
            }
        }
    }

    // --- Promotion Handling ---

    /**
     * Displays a dialog to ask the user for promotion choice.
     * Called by the Controller.
     * @param color The color of the pawn promoting.
     * @return The chosen PieceType, or null if cancelled.
     */
    public PieceType getPromotionChoice(PieceColor color) {
        // Load icons for the dialog
        Map<PieceType, ImageIcon> icons = new HashMap<>();
        String[] optionsFiles = (color == PieceColor.WHITE)
                ? new String[]{RESOURCES_WQUEEN_PNG, RESOURCES_WROOK_PNG, RESOURCES_WBISHOP_PNG, RESOURCES_WKNIGHT_PNG}
                : new String[]{RESOURCES_BQUEEN_PNG, RESOURCES_BROOK_PNG, RESOURCES_BBISHOP_PNG, RESOURCES_BKNIGHT_PNG};
        PieceType[] types = {PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT};

        Object[] options = new Object[types.length];
        for(int i = 0; i < types.length; i++){
            BufferedImage img = pieceImages.get(optionsFiles[i]);
            if(img != null){
                // Scale image for dialog
                Image scaledImg = img.getScaledInstance(squareSize > 0 ? squareSize : 50, squareSize > 0 ? squareSize : 50, Image.SCALE_SMOOTH);
                icons.put(types[i], new ImageIcon(scaledImg));
                options[i] = icons.get(types[i]);
            } else {
                options[i] = types[i].toString(); // Fallback to text
            }
        }


        int choice = JOptionPane.showOptionDialog(
                this, // Parent component
                "Choose piece for promotion:",
                "Pawn Promotion",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, // Use plain message to show icons properly
                null, // No custom icon for the dialog itself
                options, // Array of choice objects (icons or text)
                options[0] // Default selection (Queen)
        );

        if (choice >= 0 && choice < types.length) {
            return types[choice];
        } else {
            return null; // User cancelled or closed the dialog
        }
    }
}