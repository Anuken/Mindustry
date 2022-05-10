package mindustry.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
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
    //last calculated capacity
    int lastCapacity;

    public LaunchLoadoutDialog(){
        super("@configure");
    }

    public void show(CoreBlock core, Sector sector, Sector destination, Runnable confirm){
        cont.clear();
        buttons.clear();

        buttons.defaults().size(160f, 64f);
        buttons.button("@back", Icon.left, this::hide);

        addCloseListener();

        ItemSeq sitems = sector.items();

        //updates sum requirements
        Runnable update = () -> {
            int cap = lastCapacity = (int)(sector.planet.launchCapacityMultiplier * selected.findCore().itemCapacity);

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
            int capacity = lastCapacity;

            if(!sector.planet.allowLaunchLoadout){
                launches.clear();
                //TODO this should be set to a proper loadout based on sector.
                if(destination.preset != null){
                    var rules = destination.preset.generator.map.rules();
                    for(var stack : rules.loadout){
                        if(!sector.planet.hiddenItems.contains(stack.item)){
                            launches.add(stack.item, stack.amount);
                        }
                    }
                }

                universe.updateLaunchResources(launches);
            }else if(getMax()){
                for(Item item : content.items()){
                    launches.set(item, Mathf.clamp(sitems.get(item) - launches.get(item), 0, capacity));
                }

                universe.updateLaunchResources(launches);
            }

            for(ItemStack s : total){
                int as = schems.get(s.item), al = launches.get(s.item);

                if(as + al == 0) continue;

                table.image(s.item.uiIcon).left().size(iconSmall);

                String amountStr = (al + as) + (sector.planet.allowLaunchLoadout ? "[gray] (" + (al + " + " + as + ")") : "");

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

        if(sector.planet.allowLaunchLoadout){
            buttons.button("@resources.max", Icon.add, Styles.togglet, () -> {
                setMax(!getMax());
                update.run();
                rebuildItems.run();
            }).checked(b -> getMax());

            buttons.button("@resources", Icon.edit, () -> {
                ItemSeq stacks = universe.getLaunchResources();
                Seq<ItemStack> out = stacks.toSeq();

                ItemSeq realItems = sitems.copy();
                selected.requirements().each(realItems::remove);

                loadout.show(lastCapacity, realItems, out, UnlockableContent::unlocked, out::clear, () -> {}, () -> {
                    universe.updateLaunchResources(new ItemSeq(out));
                    update.run();
                    rebuildItems.run();
                });
            }).disabled(b -> getMax());
        }

        boolean rows = Core.graphics.isPortrait() && mobile;

        if(rows) buttons.row();

        var cell = buttons.button("@launch.text", Icon.ok, () -> {
            universe.updateLoadout(core, selected);
            confirm.run();
            hide();
        }).disabled(b -> !valid);

        if(rows){
            cell.colspan(2).size(160f + 160f + 4f, 64f);
        }

        int cols = Math.max((int)(Core.graphics.getWidth() / Scl.scl(230)), 1);
        ButtonGroup<Button> group = new ButtonGroup<>();
        selected = universe.getLoadout(core);
        if(selected == null) selected = schematics.getLoadouts().get((CoreBlock)Blocks.coreShard).first();

        cont.add(Core.bundle.format("launch.from", sector.name())).row();

        if(sector.planet.allowLaunchSchematics){
            cont.pane(t -> {
                int[] i = {0};

                Cons<Schematic> handler = s -> {
                    if(s.tiles.contains(tile -> !tile.block.supportsEnv(sector.planet.defaultEnv) ||
                    //make sure block can be built here.
                    (!sector.planet.hiddenItems.isEmpty() && Structs.contains(tile.block.requirements, stack -> sector.planet.hiddenItems.contains(stack.item))))){
                        return;
                    }

                    t.button(b -> b.add(new SchematicImage(s)), Styles.togglet, () -> {
                        selected = s;
                        update.run();
                        rebuildItems.run();
                    }).group(group).pad(4).checked(s == selected).size(200f);

                    if(++i[0] % cols == 0){
                        t.row();
                    }
                };

                if(sector.planet.allowLaunchSchematics || schematics.getDefaultLoadout(core) == null){
                    for(var entry : schematics.getLoadouts()){
                        if(entry.key.size <= core.size){
                            for(Schematic s : entry.value){
                                handler.get(s);
                            }
                        }
                    }
                }else{
                    //only allow launching with the standard loadout schematic
                    handler.get(schematics.getDefaultLoadout(core));
                }
            }).growX().scrollX(false);

            cont.row();

            cont.label(() -> Core.bundle.format("launch.capacity", lastCapacity)).row();
            cont.row();
        }else if(destination.preset != null && destination.preset.description != null){
            cont.pane(p -> {
                p.add(destination.preset.description).grow().wrap().labelAlign(Align.center);
            }).pad(10f).grow().row();
        }

        cont.pane(items);
        cont.row();
        cont.add("@sector.missingresources").visible(() -> !valid);

        update.run();
        rebuildItems.run();

        show();
    }

    void setMax(boolean max){
        Core.settings.put("maxresources", max);
    }

    boolean getMax(){
        return Core.settings.getBool("maxresources", true);
    }
}
