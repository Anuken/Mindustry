package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.Button;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;

import static io.anuke.mindustry.Vars.*;

public class ZoneInfoDialog extends FloatingDialog{

    public ZoneInfoDialog(){
        super("");

        titleTable.remove();
        addCloseButton();
    }

    @Override
    protected void drawBackground(float x, float y){
        drawDefaultBackground(x, y);
    }

    public void show(Zone zone){
        setup(zone);
        show();
    }

    private void setup(Zone zone){
        cont.clear();

        Table iteminfo = new Table();
        Runnable rebuildItems = () -> {
            int i = 0;
            iteminfo.clear();
            ItemStack[] stacks = zone.unlocked() ? zone.getLaunchCost() : zone.itemRequirements;
            for(ItemStack stack : stacks){
                if(stack.amount == 0) continue;

                if(i++ % 2 == 0){
                    iteminfo.row();
                }
                iteminfo.addImage(stack.item.icon(Item.Icon.medium)).size(8*3).padRight(1);
                iteminfo.add(stack.amount + "").color(Color.LIGHT_GRAY).padRight(5);
            }
        };

        rebuildItems.run();

        cont.table(cont -> {
            if(zone.locked()){
                cont.addImage("icon-zone-locked");
                cont.row();
                cont.add("$locked").padBottom(6);
                cont.row();

                cont.table(req -> {
                    req.defaults().left();

                    if(zone.zoneRequirements.length > 0){
                        req.table(r -> {
                            r.add("$complete").colspan(2).left();
                            r.row();
                            for(Zone other : zone.zoneRequirements){
                                r.addImage("icon-zone").padRight(4);
                                r.add(other.localizedName()).color(Color.LIGHT_GRAY);
                                r.addImage(other.isCompleted() ? "icon-check-2" : "icon-cancel-2")
                                .color(other.isCompleted() ? Color.LIGHT_GRAY : Color.SCARLET).padLeft(3);
                                r.row();
                            }
                        });
                    }

                    req.row();

                    if(zone.blockRequirements.length > 0){
                        req.table(r -> {
                            r.add("$research.list").colspan(2).left();
                            r.row();
                            for(Block block : zone.blockRequirements){
                                r.addImage(block.icon(Icon.small)).size(8 * 3).padRight(4);
                                r.add(block.localizedName).color(Color.LIGHT_GRAY);
                                r.addImage(data.isUnlocked(block) ? "icon-check-2" : "icon-cancel-2")
                                .color(data.isUnlocked(block) ? Color.LIGHT_GRAY : Color.SCARLET).padLeft(3);
                                r.row();
                            }

                        }).padTop(10);
                    }
                }).growX();

            }else{
                cont.add(zone.localizedName()).color(Pal.accent).growX().center();
                cont.row();
                cont.addImage("white").color(Pal.accent).height(3).pad(6).growX();
                cont.row();
                cont.table(res -> {
                    res.add("$zone.resources").padRight(6);
                    if(zone.resources.length > 0){
                        for(Item item : zone.resources){
                            res.addImage(item.icon(Item.Icon.medium)).size(8 * 3);
                        }
                    }else{
                        res.add("$none");
                    }
                });

                if(zone.bestWave() > 0){
                    cont.row();
                    cont.add(Core.bundle.format("bestwave", zone.bestWave()));
                }

                Table load = new Table();
                //thanks java, absolutely brilliant syntax here
                Runnable[] rebuildLoadout = {null};
                rebuildLoadout[0] = () -> {
                    load.clear();
                    float bsize = 40f;
                    int step = 50;

                    load.left();
                    for(ItemStack stack : zone.getStartingItems()){
                        load.addButton("x", () -> {
                            zone.getStartingItems().remove(stack);
                            zone.updateLaunchCost();
                            rebuildItems.run();
                            rebuildLoadout[0].run();
                        }).size(bsize).pad(2);

                        load.addButton("-", () -> {
                            stack.amount = Math.max(stack.amount - step, 0);
                            zone.updateLaunchCost();
                            rebuildItems.run();
                        }).size(bsize).pad(2);
                        load.addButton("+", () -> {
                            stack.amount = Math.min(stack.amount + step, zone.loadout.core().itemCapacity);
                            zone.updateLaunchCost();
                            rebuildItems.run();
                        }).size(bsize).pad(2);

                        load.addImage(stack.item.icon(Item.Icon.medium)).size(8 * 3).padRight(4);
                        load.label(() -> stack.amount + "").left();

                        load.row();
                    }

                    load.addButton("$add", () -> {
                        FloatingDialog dialog = new FloatingDialog("");
                        dialog.setFillParent(false);
                        for(Item item : content.items().select(item -> data.getItem(item) > 0 && item.type == ItemType.material && zone.getStartingItems().find(stack -> stack.item == item) == null)){
                            TextButton button = dialog.cont.addButton("", () -> {
                                zone.getStartingItems().add(new ItemStack(item, 0));
                                zone.updateLaunchCost();
                                rebuildLoadout[0].run();
                                dialog.hide();
                            }).size(300f, 35f).pad(1).get();
                            button.clearChildren();
                            button.left();
                            button.addImage(item.icon(Item.Icon.medium)).size(8*3).pad(4);
                            button.add(item.localizedName);
                            dialog.cont.row();
                        }
                        dialog.show();
                    }).colspan(4).size(100f, bsize).left().disabled(b -> !content.items().contains(item -> data.getItem(item) > 0 && item.type == ItemType.material && !zone.getStartingItems().contains(stack -> stack.item == item)));
                };

                rebuildLoadout[0].run();

                cont.row();
                cont.table(zone.canConfigure() ? "button" : "button-disabled", t -> {
                    t.left();
                    t.add(!zone.canConfigure() ? Core.bundle.format("configure.locked", zone.configureWave) : "$configure").growX().wrap();
                    if(zone.canConfigure()){
                        t.row();
                        t.pane(load).pad(2).growX().left();
                    }
                }).width(300f).pad(4).left();
            }
        });

        cont.row();

        Button button = cont.addButton(zone.locked() ? "$uncover" : "$launch", () -> {
            if(!data.isUnlocked(zone)){
                data.removeItems(zone.itemRequirements);
                data.unlockContent(zone);
                ui.deploy.setup();
                setup(zone);
            }else{
                ui.deploy.hide();
                data.removeItems(zone.getLaunchCost());
                hide();
                world.playZone(zone);
            }
        }).minWidth(150f).margin(13f).padTop(5).disabled(b -> zone.locked() ? !canUnlock(zone) : !data.hasItems(zone.getLaunchCost())).get();

        button.row();
        button.add(iteminfo);
    }

    private boolean canUnlock(Zone zone){
        if(data.isUnlocked(zone)){
            return true;
        }

        for(Zone other : zone.zoneRequirements){
            if(!other.isCompleted()){
                return false;
            }
        }

        for(Block other : zone.blockRequirements){
            if(!data.isUnlocked(other)){
                return false;
            }
        }

        return data.hasItems(zone.itemRequirements);
    }
}
