package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
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
    //total as a map
    ObjectIntMap<Item> totalMap = new ObjectIntMap<>();
    //total required items
    Seq<ItemStack> total = new Seq<>();
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
        totalMap.clear();

        Seq<ItemStack> stacks = universe.getLaunchResources();

        addCloseButton();

        //updates sum requirements
        Runnable update = () -> {
            totalMap.clear();
            total.clear();
            selected.requirements().each(i -> totalMap.increment(i.item, i.amount));
            universe.getLaunchResources().each(i -> totalMap.increment(i.item, i.amount));
            for(Item item : content.items()){
                if(totalMap.containsKey(item)) total.add(new ItemStack(item, totalMap.get(item)));
            }
            valid = build.items.has(total);
        };

        Cons<Table> rebuild = table -> {
            table.clearChildren();
            int i = 0;

            for(ItemStack s : total){
                table.image(s.item.icon(Cicon.small)).left();
                table.add((build.items.has(s.item, s.amount)) ? "[lightgray]" + s.amount + "" :
                "[scarlet]" + (Math.min(build.items.get(s.item), s.amount) + "[lightgray]/" + s.amount)).padLeft(2).left().padRight(4);

                if(++i % 4 == 0){
                    table.row();
                }
            }
        };

        Table items = new Table();

        Runnable rebuildItems = () -> rebuild.get(items);

        buttons.button("@resources", Icon.terrain, () -> {
            loadout.show(core.itemCapacity, stacks, UnlockableContent::unlocked, stacks::clear, () -> {}, () -> {
                universe.updateLaunchResources(stacks);
                update.run();
                rebuildItems.run();
            });
        });

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
