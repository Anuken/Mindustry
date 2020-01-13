package mindustry.ui.dialogs;

import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

import static mindustry.Vars.ui;

public class PlanetDialog extends FloatingDialog{
    private PlanetRenderer renderer = new PlanetRenderer();

    public PlanetDialog(){
        super("", Styles.fullDialog);

        addCloseButton();
        buttons.addImageTextButton("$techtree", Icon.tree, () -> ui.tech.show()).size(230f, 64f);

        shown(this::setup);
    }

    void setup(){
        cont.clear();
        titleTable.remove();

        cont.addRect((x, y, w, h) -> {
            renderer.render(Planets.starter);
        }).grow();
    }
}
