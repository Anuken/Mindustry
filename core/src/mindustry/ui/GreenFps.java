package mindustry.ui;

import arc.util.Align;

/** Shared constants for the developer green FPS overlay. */
public final class GreenFps{
    public static final int cornerTopLeft = 0;
    public static final int cornerTopRight = 1;
    public static final int cornerBottomLeft = 2;
    public static final int cornerBottomRight = 3;

    /** Top-right is the default: it is close to the minimap but offset so it does not cover it. */
    public static final int defaultCorner = cornerTopRight;

    private GreenFps(){}

    /** Maps a saved corner index (0..3) to a scene alignment, clamping invalid values. */
    public static int cornerAlign(int corner){
        int clamped = Math.max(cornerTopLeft, Math.min(cornerBottomRight, corner));
        return switch(clamped){
            case cornerTopLeft -> Align.topLeft;
            case cornerTopRight -> Align.topRight;
            case cornerBottomLeft -> Align.bottomLeft;
            default -> Align.bottomRight;
        };
    }
}
