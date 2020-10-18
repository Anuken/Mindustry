package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.SchematicsDialog.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Dialog for selecting loadout at sector launch. */
public class LaunchLoadoutDialog extends BaseDialog{
    LoadoutDialog loadout = new LoadoutDialog();
    //total required items
    ItemSeq total = new ItemSeq();
    //currently selected schematic
    Schematic selected;
    //validity of loadout items
    boolean valid;

    public LaunchLoadoutDialog(){
        super("@configure");
    }

    public void show(CoreBlock core, Building build, Runnable confirm){
        cont.clear();
        buttons.clear();

        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide);

        keyDown(key -> {
            if(key == KeyCode.escape || key == KeyCode.back){
                Core.app.post(this::hide);
            }
        });

        //updates sum requirements
        Runnable update = () -> {
            total.clear();
            selected.requirements().each(total::add);
            universe.getLaunchResources().each(total::add);
            valid = build.items.has(total);
        };

        Cons<Table> rebuild = table -> {
            table.clearChildren();
            int i = 0;

            ItemSeq schems = selected.requirements();
            ItemSeq launches = universe.getLaunchResources();

            for(ItemStack s : total){
                table.image(s.item.icon(Cicon.small)).left();
                int as = schems.get(s.item), al = launches.get(s.item);

                String amountStr = "[lightgray]" + (al + " + [accent]" + as + "[lightgray]");

                table.add(
                    build.items.has(s.item, s.amount) ? amountStr :
                    "[scarlet]" + (Math.min(build.items.get(s.item), s.amount) + "[lightgray]/" + amountStr)).padLeft(2).left().padRight(4);

                if(++i % 4 == 0){
                    table.row();
                }
            }
        };

        Table items = new Table();

        Runnable rebuildItems = () -> rebuild.get(items);

        buttons.button("@resources", Icon.terrain, () -> {
            ItemSeq stacks = universe.getLaunchResources();
            Seq<ItemStack> out = stacks.toSeq();

            loadout.show(core.itemCapacity, out, UnlockableContent::unlocked, out::clear, () -> {}, () -> {
                universe.updateLaunchResources(new ItemSeq(out));
                update.run();
                rebuildItems.run();
            });
        }).width(204);

        buttons.button("@launch.text", Icon.ok, () -> {
            universe.updateLoadout(core, selected);
            confirm.run();
            hide();
        }).disabled(b -> !valid);

        int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);
        ButtonGroup<Button> group = new ButtonGroup<>();
        selected = universe.getLoadout(core);

        cont.pane(t -> {
            int i = 0;

            for(Schematic s : schematics.getLoadouts(core)){

                t.button(b -> b.add(new SchematicImage(s)), Styles.togglet, () -> {
                    selected = s;
                    update.run();
                    rebuildItems.run();
                }).group(group).pad(4).disabled(!build.items.has(s.requirements())).checked(s == selected).size(200f);

                if(++i % cols == 0){
                    t.row();
                }
            }
        }).growX().get().setScrollingDisabled(true, false);

        cont.row();
        cont.add(items);

        update.run();
        rebuildItems.run();

        show();
    }
}
