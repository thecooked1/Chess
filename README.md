# Refactored Java Chess Game

## Objective

This project is a refactoring of a legacy Java chess game. The primary goal was to modernize the architecture by implementing the Model-View-Controller (MVC) design pattern, improving code structure, readability, and maintainability.

## What Was Changed

The original codebase was refactored significantly to separate concerns according to the MVC pattern:

1.  **Model:**
    *   All core chess logic, game rules, piece movement, board state, and check/checkmate detection were extracted into the `src/model` package.
    *   Classes like `Piece`, `Board`, `Square` were stripped of all UI-related code (Swing/AWT imports and logic).
    *   Introduced `PieceColor` and `PieceType` enums for better type safety.
    *   Added a `GameState` class to encapsulate game state information like player turn, castling rights, en passant target, clocks, move history, and game status.
    *   Added a `GameLogic` class to handle move validation, generation of legal moves (considering checks), and determining game outcomes (checkmate, stalemate, draws).
    *   Added a `Move` class to represent individual moves.
    *   The logic previously in `CheckmateDetector` was integrated into `GameLogic`.

2.  **View:**
    *   All graphical user interface (GUI) components were moved to the `src/view` package.
    *   `BoardPanel`: Responsible for drawing the chessboard and pieces, handling mouse interactions for piece dragging, and highlighting legal moves. Loads piece images from the `resources` folder.
    *   `GameInfoPanel`: Displays player names, clocks, the current turn, and game status messages.
    *   `ChessView`: The main application window (JFrame) that assembles the `BoardPanel`, `GameInfoPanel`, and control buttons.
    *   `StartMenu`: Provides a startup dialog to configure player names and timer settings before launching the main game window.

3.  **Controller:**
    *   The `src/controller` package contains the `GameController`.
    *   It acts as the mediator between the `View` and the `Model`.
    *   It listens for user input from the `View` (e.g., mouse clicks/drags on `BoardPanel`).
    *   It translates user actions into requests for the `Model` (e.g., asking `GameLogic` to validate and make a move).
    *   It receives updates from the `Model` (implicitly, by calling model methods and then deciding when to refresh the view).
    *   It updates the `View` when the `Model`'s state changes.
    *   It manages the game timer (`javax.swing.Timer`) for player clocks.

4.  **Main Class:**
    *   `src/Game.java` is the main entry point. It initializes the Model, View, and Controller components, links them together, and starts the application by first displaying the `StartMenu`.


## How to Build

This project uses Apache Ant for building. Ensure you have Ant installed and configured in your system PATH.

The provided `build.xml` script compiles the code and packages it into a runnable JAR file.

1.  **Open a terminal or command prompt.**
2.  **Navigate to the project's root directory** (`chess-java/`).
3.  **Run Ant:**
    ```bash
    ant
    ```
    (This runs the default target, `create_run_jar`, which depends on `compile`)
    *Alternatively, you can run specific targets:*
    ```bash
    ant clean compile create_run_jar
    ```

4.  **Output:** This will:
    *   Compile the source files from `src/` into the `bin/` directory.
    *   Copy image files from `resources/` into the `bin/` directory.
    *   Create a runnable JAR file named `chess-java.jar` in the project root directory.

## How to Run

After building the project:

1.  **Open a terminal or command prompt.**
2.  **Navigate to the project's root directory** (`chess-java/`).
3.  **Execute the JAR file using Java:**
    ```bash
    java -jar chess-java.jar
    ```
4.  This will launch the Start Menu, allowing you to configure and start the chess game.

## Unit Tests

Unit tests are intended to verify the correctness of the chess rules model (`src/model` package).

*   **Location:** Test files should be placed in the `test/` directory (structure mirroring `src/model`).
*   **Constraint:** As per requirements, JUnit is **not** used. Tests will be implemented using simple `main` methods within test classes or custom assertion mechanisms.
*   **Focus:** Tests should cover valid and invalid move scenarios for each piece type, check detection, checkmate, stalemate, castling, en passant, and promotion rules.
*   **(Note:** The test files themselves are not yet included in this refactoring step but should be added to ensure model correctness.)*