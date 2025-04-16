package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import model.piece.Pawn;

/**
 * Encapsulates the core chess game logic, including move validation,
 * generation of legal moves, check/checkmate/stalemate detection,
 * and applying moves to the board state.
 * It coordinates interactions between the Board and GameState.
 */
public class GameLogic {

    private final Board board; // The current board state this logic operates on

    public GameLogic(Board board) {
        this.board = board;
    }

    /**
     * Gets the underlying board object.
     * @return The Board instance.
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Gets the current game state.
     * @return The GameState instance from the board.
     */
    public GameState getGameState() {
        return board.getGameState();
    }


    /**
     * Attempts to make a move from the start square to the end square.
     * Validates the move, and if legal, applies it to the board and updates game state.
     * Also checks for game-ending conditions (checkmate, stalemate, draws) after the move.
     *
     * @param start The starting square.
     * @param end The ending square.
     * @param promotionType The type of piece to promote to if this is a pawn promotion (can be null).
     * @return true if the move was legal and applied, false otherwise.
     */
    public boolean makeMove(Square start, Square end, PieceType promotionType) {
        if (board.getGameState().isGameOver()) {
            System.out.println("Game is over. Cannot make moves.");
            return false;
        }

        Piece pieceToMove = start.getOccupyingPiece();
        if (pieceToMove == null) {
            System.out.println("Invalid move: No piece at start square " + start);
            return false;
        }

        if (pieceToMove.getColor() != board.getGameState().getCurrentPlayerTurn()) {
            System.out.println("Invalid move: It's not " + pieceToMove.getColor() + "'s turn.");
            return false;
        }

        // Generate all legal moves for the piece at the start square
        List<Move> legalMoves = getLegalMovesForPiece(pieceToMove);

        // Find the specific move matching the start, end, and promotion type
        Move requestedMove = null;
        for (Move legalMove : legalMoves) {
            if (legalMove.getEndSquare().equals(end)) {
                // If it's a promotion, check the promotion type matches
                if (legalMove.isPromotion()) {
                    if (legalMove.getPromotionType() == promotionType) {
                        requestedMove = legalMove;
                        break;
                    }
                } else {
                    // Not a promotion, or promotionType parameter is null
                    if (promotionType == null) {
                        requestedMove = legalMove;
                        break;
                    } else {
                        // Trying to specify promotion for a non-promotion move
                        System.out.println("Invalid move: Specified promotion type for non-promotion move.");
                        return false;
                    }
                }
            }
        }


        if (requestedMove != null) {
            // Apply the move to the board
            board.applyMove(requestedMove);

            // Update GameState (turn is switched in recordMove)
            GameState gameState = board.getGameState();
            // Set EP target square if the move was a double pawn push
            if (requestedMove.getPieceMoved().getType() == PieceType.PAWN &&
                    Math.abs(requestedMove.getStartSquare().getRank() - requestedMove.getEndSquare().getRank()) == 2) {
                int epRank = (requestedMove.getPieceMoved().getColor() == PieceColor.WHITE)
                        ? requestedMove.getStartSquare().getRank() + 1
                        : requestedMove.getStartSquare().getRank() - 1;
                gameState.setEnPassantTargetSquare(board.getSquare(requestedMove.getEndSquare().getFile(), epRank));
            } else {
                gameState.setEnPassantTargetSquare(null); // Clear EP target if not a double push
            }

            // Record move history and update counters (this also switches turn)
            gameState.recordMove(requestedMove, board.calculateBoardHash());

            // Check for game end conditions for the *next* player
            updateGameStatus(gameState.getCurrentPlayerTurn());

            return true;
        } else {
            System.out.println("Invalid move: " + start + " to " + end + " is not legal.");
            return false;
        }
    }

    /**
     * Simplified makeMove without promotion handling.
     * Assumes promotionType is null.
     * @param start The starting square.
     * @param end The ending square.
     * @return true if the move was legal and applied, false otherwise.
     */
    public boolean makeMove(Square start, Square end) {
        return makeMove(start, end, null);
    }


    /**
     * Checks the current game status (checkmate, stalemate, draws) for the player whose turn it is.
     * Updates the GameState's status if the game has ended.
     *
     * @param playerToAssess The player whose status is being checked (usually the one whose turn it now is).
     */
    private void updateGameStatus(PieceColor playerToAssess) {
        GameState gameState = board.getGameState();
        if (gameState.isGameOver()) return; // Don't re-evaluate if already over

        List<Move> legalMoves = getAllLegalMoves(playerToAssess);
        boolean inCheck = isKingInCheck(playerToAssess);

        if (legalMoves.isEmpty()) {
            if (inCheck) {
                // Checkmate
                gameState.setGameStatus(
                        playerToAssess == PieceColor.WHITE ? GameState.GameStatus.BLACK_WINS_CHECKMATE
                                : GameState.GameStatus.WHITE_WINS_CHECKMATE
                );
            } else {
                // Stalemate
                gameState.setGameStatus(GameState.GameStatus.STALEMATE);
            }
        } else {
            // Check for draws by rule
            if (gameState.isFiftyMoveRule()) {
                gameState.setGameStatus(GameState.GameStatus.DRAW_FIFTY_MOVE_RULE);
            } else if (gameState.isThreefoldRepetition(board.calculateBoardHash())) {
                // Check repetition AFTER the move is fully completed and hash recorded
                gameState.setGameStatus(GameState.GameStatus.DRAW_THREEFOLD_REPETITION);
            } else if (hasInsufficientMaterial()) {
                gameState.setGameStatus(GameState.GameStatus.DRAW_INSUFFICIENT_MATERIAL);
            }
            // Note: Draw by agreement / resignation needs external trigger
        }

        // Check for out of time AFTER checking mate/stalemate
        if (!gameState.isGameOver()) { // Only check time if game isn't already over
            Clock clock = gameState.getClock(playerToAssess);
            // Need to check the *opponent's* clock, as time ran out *before* this player's turn
            Clock opponentClock = gameState.getClock(playerToAssess.opposite());
            if (opponentClock.isOutOfTime()) {
                gameState.setGameStatus(
                        playerToAssess == PieceColor.WHITE ? GameState.GameStatus.WHITE_WINS_TIME // White wins if Black ran out
                                : GameState.GameStatus.BLACK_WINS_TIME // Black wins if White ran out
                );
            }
        }
    }

    /**
     * Generates all legal moves for the specified player in the current board position.
     * A legal move is a potential move that does not leave the player's king in check.
     *
     * @param playerColor The color of the player whose moves to generate.
     * @return A List of all valid Move objects.
     */
    public List<Move> getAllLegalMoves(PieceColor playerColor) {
        List<Move> legalMoves = new ArrayList<>();
        List<Piece> pieces = board.getPieces(playerColor);

        for (Piece piece : pieces) {
            legalMoves.addAll(getLegalMovesForPiece(piece));
        }
        return legalMoves;
    }

    /**
     * Generates all legal moves for a specific piece.
     *
     * @param piece The piece to generate moves for.
     * @return A List of valid Move objects for that piece. Returns empty list if piece is null.
     */
    public List<Move> getLegalMovesForPiece(Piece piece) {
        if (piece == null) return new ArrayList<>(); // Handle null piece case

        List<Move> potentialMoves = generatePotentialMovesForPiece(piece);
        List<Move> legalMoves = new ArrayList<>();

        // Filter out moves that leave the king in check
        for (Move move : potentialMoves) {
            if (isMoveLegal(move)) {
                legalMoves.add(move);
            }
        }
        return legalMoves;
    }


    /**
     * Generates potential moves for a piece based on its movement rules,
     * including special moves like castling and promotion possibilities.
     * Does NOT filter for checks.
     */
    private List<Move> generatePotentialMovesForPiece(Piece piece) {
        List<Move> potentialMoves = new ArrayList<>();
        List<Square> targetSquares = piece.getPotentialMoves(board); // Use piece's own logic

        for (Square target : targetSquares) {
            // Handle Promotions
            if (piece.getType() == PieceType.PAWN) {
                int promotionRank = ((Pawn) piece).getPromotionRank();
                if (target.getRank() == promotionRank) {
                    // Add moves for each possible promotion piece type
                    potentialMoves.add(new Move(piece.getPosition(), target, piece, target.getOccupyingPiece(), PieceType.QUEEN));
                    potentialMoves.add(new Move(piece.getPosition(), target, piece, target.getOccupyingPiece(), PieceType.ROOK));
                    potentialMoves.add(new Move(piece.getPosition(), target, piece, target.getOccupyingPiece(), PieceType.BISHOP));
                    potentialMoves.add(new Move(piece.getPosition(), target, piece, target.getOccupyingPiece(), PieceType.KNIGHT));
                    continue; // Don't add the standard move if it's a promotion
                }
            }
            // Create standard move (includes captures, castling intent, en passant intent)
            potentialMoves.add(new Move(board, piece.getPosition(), target));
        }
        return potentialMoves;
    }


    /**
     * Checks if a specific move is legal (i.e., does not leave the mover's king in check).
     * This involves simulating the move on a temporary board copy.
     *
     * @param move The move to validate.
     * @return true if the move is legal, false otherwise.
     */
    private boolean isMoveLegal(Move move) {
        Piece movingPiece = move.getPieceMoved();
        PieceColor movingColor = movingPiece.getColor();

        // Simulate the move on a copy of the board
        Board boardCopy = board.copy();
        // Need to find the corresponding start/end squares on the copied board
        Square startCopy = boardCopy.getSquare(move.getStartSquare().getFile(), move.getStartSquare().getRank());
        Square endCopy = boardCopy.getSquare(move.getEndSquare().getFile(), move.getEndSquare().getRank());
        Piece pieceCopy = startCopy.getOccupyingPiece(); // Get the piece copy

        // Create the move object within the context of the copied board
        Move moveCopy;
        if(move.isPromotion()){
            moveCopy = new Move(startCopy, endCopy, pieceCopy, endCopy.getOccupyingPiece(), move.getPromotionType());
        } else {
            moveCopy = new Move(boardCopy, startCopy, endCopy);
        }


        // Apply the move copy
        boardCopy.applyMove(moveCopy); // Apply the move on the copy


        // Check if the moving player's king is in check *after* the move
        GameLogic logicCopy = new GameLogic(boardCopy); // Use logic on the copy
        return !logicCopy.isKingInCheck(movingColor);
    }

    /**
     * Checks if the king of the specified color is currently in check.
     *
     * @param kingColor The color of the king to check.
     * @return true if the king is in check, false otherwise.
     */
    public boolean isKingInCheck(PieceColor kingColor) {
        Square kingSquare = board.getKingSquare(kingColor);
        if (kingSquare == null) {
            // Should not happen in a valid game, but handle defensively
            System.err.println("Error: King square not found for " + kingColor);
            return false;
        }
        return isSquareAttackedBy(kingSquare, kingColor.opposite());
    }

    /**
     * Checks if a given square is attacked by any piece of the specified attacker color.
     *
     * @param targetSquare The square to check.
     * @param attackerColor The color of the pieces potentially attacking the square.
     * @return true if the square is attacked, false otherwise.
     */
    public boolean isSquareAttackedBy(Square targetSquare, PieceColor attackerColor) {
        List<Piece> opponentPieces = board.getPieces(attackerColor);

        for (Piece piece : opponentPieces) {
            // Important: Use getPotentialMoves which doesn't filter for check itself.
            // We are checking raw attack coverage here.
            List<Square> potentialMoves = piece.getPotentialMoves(board);

            // Special check for pawn attacks (getPotentialMoves includes forward moves)
            if (piece.getType() == PieceType.PAWN) {
                int currentFile = piece.getPosition().getFile();
                int currentRank = piece.getPosition().getRank();
                int direction = (piece.getColor() == PieceColor.WHITE) ? 1 : -1;
                int[] captureFiles = { currentFile - 1, currentFile + 1 };
                int captureRank = currentRank + direction;

                for (int captureFile : captureFiles) {
                    if (Board.isValidCoordinate(captureFile, captureRank)) {
                        if (board.getSquare(captureFile, captureRank).equals(targetSquare)) {
                            return true; // Pawn attacks this square diagonally
                        }
                    }
                }
                // Continue to next piece after checking pawn attack squares
                continue;
            }


            // For other pieces, check if their potential moves include the target square
            if (potentialMoves.contains(targetSquare)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks for insufficient material draw condition.
     * Based on FIDE Laws of Chess Article 5.2.2 and common interpretations.
     * @return True if the material is insufficient for checkmate, false otherwise.
     */
    private boolean hasInsufficientMaterial() {
        List<Piece> whitePieces = board.getPieces(PieceColor.WHITE);
        List<Piece> blackPieces = board.getPieces(PieceColor.BLACK);

        // King vs King
        if (isKingOnly(whitePieces) && isKingOnly(blackPieces)) return true;

        // King vs King and Bishop OR King vs King and Knight
        if (isKingOnly(whitePieces) && isKingAndOneMinor(blackPieces)) return true;
        if (isKingAndOneMinor(whitePieces) && isKingOnly(blackPieces)) return true;

        // King and Bishop vs King and Bishop (Bishops on same color squares)
        if (isKingAndOneBishop(whitePieces) && isKingAndOneBishop(blackPieces)) {
            Piece whiteBishop = getFirstPieceOfType(whitePieces, PieceType.BISHOP);
            Piece blackBishop = getFirstPieceOfType(blackPieces, PieceType.BISHOP);
            if (whiteBishop != null && blackBishop != null) {
                boolean whiteBishopOnLight = isLightSquare(whiteBishop.getPosition());
                boolean blackBishopOnLight = isLightSquare(blackBishop.getPosition());
                if (whiteBishopOnLight == blackBishopOnLight) {
                    return true;
                }
            }
        }

        // Other combinations (like K+N vs K+N) are generally not automatic draws
        // by rule but often end in draws by agreement or repetition/50-move.

        return false;
    }

    private boolean isKingOnly(List<Piece> pieces) {
        return pieces.size() == 1 && pieces.get(0).getType() == PieceType.KING;
    }

    private boolean isKingAndOneMinor(List<Piece> pieces) {
        if (pieces.size() != 2) return false;
        boolean kingFound = false;
        boolean minorFound = false;
        for (Piece p : pieces) {
            if (p.getType() == PieceType.KING) kingFound = true;
            if (p.getType() == PieceType.BISHOP || p.getType() == PieceType.KNIGHT) minorFound = true;
        }
        return kingFound && minorFound;
    }

    private boolean isKingAndOneBishop(List<Piece> pieces) {
        if (pieces.size() != 2) return false;
        boolean kingFound = false;
        boolean bishopFound = false;
        for (Piece p : pieces) {
            if (p.getType() == PieceType.KING) kingFound = true;
            if (p.getType() == PieceType.BISHOP) bishopFound = true;
        }
        return kingFound && bishopFound;
    }

    private Piece getFirstPieceOfType(List<Piece> pieces, PieceType type) {
        for (Piece p : pieces) {
            if (p.getType() == type) {
                return p;
            }
        }
        return null;
    }

    private boolean isLightSquare(Square s) {
        // Sum of file (0-7) and rank (0-7) is even for light squares in standard setup (a1 is dark)
        // Let's re-verify. a1 (0,0) sum 0 (even) -> dark. h1 (7,0) sum 7 (odd) -> light.
        // a8 (0,7) sum 7 (odd) -> light. h8 (7,7) sum 14 (even) -> dark.
        // So, (file + rank) % 2 != 0 means light square.
        return (s.getFile() + s.getRank()) % 2 != 0;
    }

}