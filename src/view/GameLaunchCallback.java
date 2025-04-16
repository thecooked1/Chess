package view;

// Define a functional interface for the callback
@FunctionalInterface
public interface GameLaunchCallback {
    void launch(String whiteName, String blackName, int hh, int mm, int ss);
}
