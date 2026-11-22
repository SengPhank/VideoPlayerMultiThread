/*
 * Starter code supplied for Adelaide University COMP3011 Assignment 2.
 * Students are free to modify this file for assessment purposes.
 * 
 * Authors:
 *   1. Simon Ratcliffe, in collaboration with GPT-5.6 Terra
 *   2. Sengphachanh Phank a1967663
 *
 * Copyright 2026 Simon Ratcliffe
 */
package comp3011;

import java.util.List;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;

/**
 * Extents JavaFX's Application class and is the entry point for the video player.
 * Provided required JavaFX methods start and stop and also basic on-screen window
 * management functions. Owns the {@link VideoPlayerController}, which in turn owns
 * the {@link VideoPlayerView} and {@link VideoPlayerController}.
 *
 * <p>
 * Creates the JavaFX Stage and Scene, the VideoPlayerController, and applies
 * command-line launch settings, and starts playback. It also resizes the video
 * display to fit the selected screen.
 * </p>
 */

/*
 * AI Attribution: AI Was used to help test and optimise
 * window drag and resizing
 */
public class VideoPlayerApp extends Application {
    private static CommandLineController commandLineController;

    private VideoPlayerController controller;
    private Stage stage;
    private Rectangle2D selectedScreenBounds;
    private int videoWidth;
    private int videoHeight;
    
    private boolean placed;
    private double dragOffsetX;
    private double dragOffsetY;
    
    // Resizing fields
    private static final double RESIZE_MARGIN = 6;
    private static final double MIN_WIDTH = 320;
    private static final double MIN_HEIGHT = 240;
    private boolean resizing;
    private boolean wasResized;
    private boolean resizeLeft, resizeRight, resizeTop, resizeBottom;
    private double startX, startY, startW, startH, startMouseX, startMouseY;
    
    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
        
        controller = new VideoPlayerController(
                commandLineController.isAudioRequested(),
                this::setVideoSize,
                commandLineController.getFrameProcessors(),
                commandLineController.getSubtitleProcessor());

        Scene scene = new Scene(controller.getView().getRoot());
        controller.installKeyboardShortcuts(scene);

        stage.setTitle("COMP3011 Video Player");
        stage.setScene(scene);
        moveToSelectedScreen(stage);
        stage.setMaximized(commandLineController.isMaximiseRequested());
        
        // Hook top bar with mouse click
        installWindowControls(controller.getView());
        
        stage.show();

        stage.outputScaleXProperty().addListener((_, _, _) -> updateVideoSize());
        stage.outputScaleYProperty().addListener((_, _, _) -> updateVideoSize());
        stage.setOnCloseRequest(_ -> controller.shutdown());
        
        installResizeHandles(scene);
        playVideo();
    }

    private void moveToSelectedScreen(Stage stage) {
        List<Screen> screens = Screen.getScreens();
        Integer displayId = commandLineController.getDisplayId();
        Screen targetScreen;

        if (displayId == null) {
            targetScreen = screens.stream()
                    .filter(screen -> !screen.equals(Screen.getPrimary()))
                    .findFirst()
                    .orElse(Screen.getPrimary());
        } else if (displayId <= screens.size()) {
            targetScreen = screens.get(displayId - 1);
        } else {
            targetScreen = Screen.getPrimary();
            controller.getView().setStatus("Display " + displayId + " not found");
        }

        Rectangle2D bounds = targetScreen.getVisualBounds();
        selectedScreenBounds = bounds;
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
    }

    private void playVideo() {
        controller.play(commandLineController.getVideoFile());
    }

    private void setVideoSize(int width, int height) {
        videoWidth = width;
        videoHeight = height;
        updateVideoSize();
    }

    private void updateVideoSize() {
        if (videoWidth <= 0 || videoHeight <= 0) {
            return;
        }

        double outputScaleX = stage.getOutputScaleX();
        double outputScaleY = stage.getOutputScaleY();

        if (outputScaleX <= 0) {
            outputScaleX = 1;
        }
        if (outputScaleY <= 0) {
            outputScaleY = 1;
        }

        double displayWidth = videoWidth / outputScaleX;
        double displayHeight = videoHeight / outputScaleY;

        controller.getView().setVideoDisplaySize(displayWidth, displayHeight);
        if (!stage.isMaximized() && !wasResized) {
            stage.sizeToScene();
            if (!placed) {
            	// First launch, screen appears in the corner
                alignSceneToSelectedScreen();
                placed = true;
            }
        }
    }

    private void alignSceneToSelectedScreen() {
        if (selectedScreenBounds == null || stage.getScene() == null) {
            return;
        }

        stage.setX(selectedScreenBounds.getMinX() - stage.getScene().getX());
        stage.setY(selectedScreenBounds.getMinY() - stage.getScene().getY());
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    // Entry point.
    public static void main(String[] args) {
        commandLineController = new CommandLineController(args);

        if (!commandLineController.shouldLaunchApplication()) {
            System.exit(commandLineController.getExitCode());
        }

        launch(args);
    }
    
    // Hook mouse events to the top navigation bar
    private void installWindowControls(VideoPlayerView view) {
        view.getTitleBar().setOnMousePressed(event -> {
        	// where inside the window we grabbed it
            dragOffsetX = event.getScreenX() - stage.getX();
            dragOffsetY = event.getScreenY() - stage.getY();
        });
        view.getTitleBar().setOnMouseDragged(event -> {
            if (stage.isMaximized()) { return; }
            stage.setX(event.getScreenX() - dragOffsetX);
            stage.setY(event.getScreenY() - dragOffsetY);
        });
        view.getMinimiseButton().setOnAction(_ -> stage.setIconified(true));
        
        // Platform.exit() calls stop()
        view.getCloseButton().setOnAction(_ -> javafx.application.Platform.exit());
    }
    
    // Hook mouse events to each screen edge for resizing
    private void installResizeHandles(Scene scene) {
        // Filters run before child handlers, so edges work even over the title bar and video.
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (stage.isMaximized()) { scene.setCursor(Cursor.DEFAULT); return; }
            updateEdgeFlags(scene, e);
            scene.setCursor(edgeCursor());
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (stage.isMaximized()) { return; }
            updateEdgeFlags(scene, e);
            resizing = resizeLeft || resizeRight || resizeTop || resizeBottom;
            if (resizing) {
                startX = stage.getX();
                startY = stage.getY();
                startW = stage.getWidth();
                startH = stage.getHeight();
                startMouseX = e.getScreenX();
                startMouseY = e.getScreenY();
                // don't let the title bar start a window drag
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!resizing) { return; }
            double dx = e.getScreenX() - startMouseX;
            double dy = e.getScreenY() - startMouseY;

            if (resizeRight) {
                stage.setWidth(Math.max(MIN_WIDTH, startW + dx));
            } else if (resizeLeft) {
                double w = Math.max(MIN_WIDTH, startW - dx);
                stage.setWidth(w);
                // keep right edge fixed
                stage.setX(startX + startW - w);
            }
            if (resizeBottom) {
                stage.setHeight(Math.max(MIN_HEIGHT, startH + dy));
            } else if (resizeTop) {
                double h = Math.max(MIN_HEIGHT, startH - dy);
                stage.setHeight(h);
                // keep bottom edge fixed
                stage.setY(startY + startH - h);
            }
            wasResized = true;
            e.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> resizing = false);
    }
    
    
    private void updateEdgeFlags(Scene scene, MouseEvent e) {
        resizeLeft = e.getSceneX() < RESIZE_MARGIN;
        resizeRight = e.getSceneX() > scene.getWidth() - RESIZE_MARGIN;
        resizeTop = e.getSceneY() < RESIZE_MARGIN;
        resizeBottom = e.getSceneY() > scene.getHeight() - RESIZE_MARGIN;
    }
    
    private Cursor edgeCursor() {
        if (resizeTop && resizeLeft) return Cursor.NW_RESIZE;
        if (resizeTop && resizeRight) return Cursor.NE_RESIZE;
        if (resizeBottom && resizeLeft) return Cursor.SW_RESIZE;
        if (resizeBottom && resizeRight) return Cursor.SE_RESIZE;
        if (resizeLeft) return Cursor.W_RESIZE;
        if (resizeRight) return Cursor.E_RESIZE;
        if (resizeTop) return Cursor.N_RESIZE;
        if (resizeBottom) return Cursor.S_RESIZE;
        return Cursor.DEFAULT;
    }
}
