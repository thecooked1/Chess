# Java Chess Game - MVC Refactor

    This project is an MVC (Model-View-Controller) refactoring of an original Java Swing-based Chess game.

    ## Objective

    The goal was to separate concerns by extracting all game logic (rules, state management) into a clean, well-encapsulated Model layer, distinct from the user interface (View) and the input handling/coordination logic (Controller). This improves modularity, testability, and maintainability.

    ## What Was Changed

    *   **Architecture:** Migrated from a monolithic structure (mostly within `Board` and `GameWindow`) to a standard MVC pattern.
    *   **Model Layer (`src/model`):**
        *   Contains all chess rules, piece movement logic, game state (board position, turn, castling rights, en passant), and status checks (check, checkmate, stalemate).
        *   Completely independent of Swing/AWT (no UI code).
        *   Key classes: `GameLogic`, `GameState`, `Board`, `Move`, `Position`, `Color`, `Piece` (and subclasses in `pieces/`).
    *   **View Layer (`src/view`):**
        *   Responsible for displaying the game state visually using Swing.
        *   Receives data from the Model (via the Controller) to render the board, pieces, status, and clocks.
        *   Key classes: `GameFrame`, `ChessBoardPanel`, `SquarePanel`, `PieceImageLoader`, `StartMenu`.
    *   **Controller Layer (`src/controller`):**
        *   Acts as the intermediary between View and Model.
        *   Handles user input (mouse clicks, drags, button presses) from the View.
        *   Translates user actions into commands for the Model (e.g., validating and making moves via `GameLogic`).
        *   Updates the View when the Model's state changes.
        *   Manages game timers.
        *   Key class: `GameController`.
    *   **Piece Logic:** `getLegalMoves` in Piece subclasses now calculates *pseudo-legal* moves (ignoring check safety). The `GameLogic` handles the full legality check, including preventing moves into check.
    *   **Check/Checkmate:** Logic previously in `CheckmateDetector` is integrated into `GameState` and `GameLogic`.
    *   **Input Handling:** Mouse listeners are now in the `GameController`, not the `Board` panel itself.
    *   **Dependencies:** Clean separation aims to minimize dependencies between layers (View depends on Controller, Controller depends on Model and View, Model is independent).
    *   **Testing:** JUnit 5 tests were added (`test/`) to verify the Model's core logic (piece moves, game rules, status checks).
    *   **Build System:** An Ant `build.xml` is provided for compiling, testing, and packaging the application.

## How to Build and Run

    ### Prerequisites

    *   Java Development Kit (JDK) 11 or later (code uses features like `List.of`, `Optional`, `stream().toList()`, adjust if using older JDK).
    *   Apache Ant build tool.
    *   (For Testing) JUnit 5 JARs (e.g., `junit-platform-console-standalone-X.Y.Z.jar`). Download and place them in a `lib/` directory or update the classpath in `build.xml`.

    ### Building with Ant

    1.  Open a terminal or command prompt in the `chess-game` directory.
    2.  (If Testing) Ensure JUnit JARs are in the `lib/` directory or `build.xml` points to them.
    3.  Run Ant commands:
        *   Clean previous build: `ant clean`
        *   Compile: `ant compile`
        *   Compile and run tests: `ant test` (Requires JUnit JARs)
        *   Create runnable JAR (includes dependencies using Jar-in-Jar): `ant create_run_jar`
        *   Create simple JAR (no dependencies bundled): `ant create_simple_jar`

    ### Running the Application

    1.  **Using the runnable JAR (created by `ant create_run_jar`):**
        ```bash
        java -jar build/jar/chess-game.jar
        ```

    2.  **Using the simple JAR (requires dependencies on classpath):**
        *(Assuming JUnit was needed only for tests)*
        ```bash
        java -cp build/jar/chess-game-simple.jar com.chess.game.Game
        ```
        *(If external libraries besides JUnit were needed at runtime and not bundled, add them to the `-cp`)*

    3.  **Running from compiled classes (after `ant compile`):**
        ```bash
        # On Windows
        java -cp build/classes com.chess.game.Game
        # On Linux/macOS
        java -cp build/classes com.chess.game.Game
        ```

    The application should start with the "Setup New Chess Game" window. Configure players/time and click "Start Game".

## New Structure Organization


    chess-game/
    ├── src/                      # Source code root
    │   ├── com/chess/controller/ # Controller classes (GameController)
    │   ├── com/chess/model/      # Model classes (GameLogic, GameState, Board, Move, etc.)
    │   │   └── pieces/           # Piece subclasses and related enums
    │   ├── com/chess/view/       # View classes (GameFrame, ChessBoardPanel, etc.)
    │   └── com/chess/game/       # Main application entry point (Game.java)
    │
    ├── resources/                # Image assets (.png files)
    │
    ├── test/                     # Unit test source code root
    │   └── com/chess/model/      # Tests mirroring model structure
    │       └── pieces/
    │
    ├── lib/                      # Optional: For third-party libraries (like JUnit JARs)
    │
    ├── build/                    # Build output directory (created by Ant)
    │   ├── classes/              # Compiled .class files
    │   ├── test-classes/         # Compiled test .class files
    │   ├── test-reports/         # JUnit test reports (XML, HTML)
    │   └── jar/                  # Packaged .jar file(s)
    │
    ├── jar-in-jar-loader.zip     # Eclipse Jar-in-Jar loader (if used)
    │
    ├── build.xml                 # Ant build script
    │
    └── README.md                 # This file