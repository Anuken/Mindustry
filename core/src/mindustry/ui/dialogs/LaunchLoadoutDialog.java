package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.content.*;
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

    public void show(CoreBlock core, Sector sector, Runnable confirm){
        cont.clear();
        buttons.clear();

        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide);

        addCloseListener();

        ItemSeq sitems = sector.items();

        //updates sum requirements
        Runnable update = () -> {
            int cap = selected.findCore().itemCapacity;

            //cap resources based on core type
            ItemSeq resources = universe.getLaunchResources();
            resources.min(cap);
            universe.updateLaunchResources(resources);

            total.clear();
            selected.requirements().each(total::add);
            universe.getLaunchResources().each(total::add);
            valid = sitems.has(total);
        };

        Cons<Table> rebuild = table -> {
            table.clearChildren();
            int i = 0;

            ItemSeq schems = selected.requirements();
            ItemSeq launches = universe.getLaunchResources();

            for(ItemStack s : total){
                table.image(s.item.uiIcon).left().size(iconSmall);
                int as = schems.get(s.item), al = launches.get(s.item);

                String amountStr = (al + as) + "[gray] (" + (al + " + " + as + ")");

                table.add(
                    sitems.has(s.item, s.amount) ? amountStr :
                    "[scarlet]" + (Math.min(sitems.get(s.item), s.amount) + "[lightgray]/" + amountStr)).padLeft(2).left().padRight(4);

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

            ItemSeq realItems = sitems.copy();
            selected.requirements().each(realItems::remove);

            loadout.show(selected.findCore().itemCapacity, realItems, out, UnlockableContent::unlocked, out::clear, () -> {}, () -> {
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
        if(selected == null) selected = schematics.getLoadouts().get((CoreBlock)Blocks.coreShard).first();

        cont.add(Core.bundle.format("launch.from", sector.name())).row();

        cont.pane(t -> {
            int i = 0;

            for(var entry : schematics.getLoadouts()){
                if(entry.key.size <= core.size){
                    for(Schematic s : entry.value){

                        t.button(b -> b.add(new SchematicImage(s)), Styles.togglet, () -> {
                            selected = s;
                            update.run();
                            rebuildItems.run();
                        }).group(group).pad(4).checked(s == selected).size(200f);

                        if(++i % cols == 0){
                            t.row();
                        }
                    }
                }
            }


        }).growX().get().setScrollingDisabled(true, false);

        cont.row();
        cont.pane(items);
        cont.row();
        cont.add("@sector.missingresources").visible(() -> !valid);

        update.run();
        rebuildItems.run();

        show();
    }
}
