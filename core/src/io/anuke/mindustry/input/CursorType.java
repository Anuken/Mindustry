package io.anuke.mindustry.input;

import io.anuke.ucore.scene.utils.Cursors;

/**
 * Type of cursor for displaying on desktop.
 */
public enum CursorType{
    normal(Cursors::restoreCursor),
    hand(Cursors::setHand),
    drill(() -> Cursors.set("drill")),
    unload(() -> Cursors.set("unload"));

    private final Runnable call;

    CursorType(Runnable call){
        this.call = call;
    }

    /**
     * Sets the current system cursor to this.
     */
    void set(){
        call.run();
    }
}
