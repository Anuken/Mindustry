package io.anuke.mindustry.ui.dialogs;

public class SchematicsDialog extends FloatingDialog{

    public SchematicsDialog(){
        super("$schematics");

        shown(this::setup);
    }

    void setup(){
        cont.clear();
    }
}
