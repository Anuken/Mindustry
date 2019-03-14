package io.anuke.mindustry.editor;

import io.anuke.mindustry.ui.dialogs.FloatingDialog;

public class WaveInfoDialog extends FloatingDialog{
    private final MapEditor editor;

    public WaveInfoDialog(MapEditor editor){
        super("$editor.waves");
        this.editor = editor;

        shown(this::setup);
    }

    void setup(){

    }
}
