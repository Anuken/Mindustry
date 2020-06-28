package mindustry.ui.dialogs;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.SchematicsDialog.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Dialog for selecting loadout at sector launch. */
public class LaunchLoadoutDialog extends BaseDialog{
    Schematic selected;
    boolean valid;

    public LaunchLoadoutDialog(){
        super("$configure");
    }

    public void show(CoreBlock core, Building build, Runnable confirm){
        cont.clear();
        buttons.clear();

        addCloseButton();
        buttons.button("$launch.text", Icon.ok, () -> {
            universe.updateLoadout(core, selected);
            confirm.run();
            hide();
        }).disabled(b -> !valid);

        int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);
        ButtonGroup<Button> group = new ButtonGroup<>();
        selected = universe.getLoadout(core);

        Table items = new Table();

        Runnable rebuildItems = () -> {
            items.clearChildren();
            int i = 0;

            for(ItemStack s : selected.requirements()){
                items.image(s.item.icon(Cicon.small)).left();
                items.add((state.rules.infiniteResources || build.items.has(s.item, s.amount)) ? "[lightgray]" + s.amount + "" :
                    ((build.items.has(s.item, s.amount) ? "[lightgray]" : "[scarlet]") + Math.min(build.items.get(s.item), s.amount) + "[lightgray]/" + s.amount))
                    .padLeft(2).left().padRight(4);

                if(++i % 4 == 0){
                    items.row();
                }
            }
        };

        cont.pane(t -> {
            int i = 0;

            for(Schematic s : schematics.getLoadouts(core)){

                t.button(b -> b.add(new SchematicImage(s)), Styles.togglet, () -> {
                    selected = s;
                    rebuildItems.run();
                    valid = build.items.has(selected.requirements());
                }).group(group).pad(4).disabled(!build.items.has(s.requirements())).checked(s == selected).size(200f);

                if(++i % cols == 0){
                    t.row();
                }
            }
        }).growX().get().setScrollingDisabled(true, false);

        cont.row();

        cont.add(items);

        rebuildItems.run();

        //TODO configure items to launch with

        show();

    }
}
