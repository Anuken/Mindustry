package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.SchematicsDialog.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Dialog for selecting loadout at sector launch. */
public class LaunchLoadoutDialog extends BaseDialog{
    Schematic selected;

    public LaunchLoadoutDialog(){
        super("$configure");
    }

    public void show(CoreBlock core, Building build, Runnable confirm){
        cont.clear();
        buttons.clear();

        addCloseButton();
        buttons.button("$ok", () -> {
            universe.updateLoadout(core, selected);
            confirm.run();
            hide();
        });

        int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);
        ButtonGroup<Button> group = new ButtonGroup<>();
        selected = universe.getLoadout(core);

        cont.pane(t -> {
            int i = 0;

            for(Schematic s : schematics.getLoadouts(core)){

                t.button(b -> b.add(new SchematicImage(s)), Styles.togglet, () -> selected = s)
                .group(group).pad(4).disabled(!build.items.has(s.requirements())).checked(s == selected).size(200f);

                if(++i % cols == 0){
                    t.row();
                }
            }
        }).growX().get().setScrollingDisabled(true, false);

        //TODO configure items to launch with

        show();

    }
}
