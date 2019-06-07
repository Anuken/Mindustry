package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.Button;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.type.Zone.ZoneRequirement;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;

import static io.anuke.mindustry.Vars.*;

public class ZoneInfoDialog extends FloatingDialog{
    private ZoneLoadoutDialog loadout = new ZoneLoadoutDialog();

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

            if(!zone.unlocked()) return;

            ItemStack[] stacks = zone.getLaunchCost();
            for(ItemStack stack : stacks){
                if(stack.amount == 0) continue;

                if(i++ % 2 == 0){
                    iteminfo.row();
                }
                iteminfo.addImage(stack.item.icon(Item.Icon.medium)).size(8 * 3).padRight(1);
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
                            for(ZoneRequirement other : zone.zoneRequirements){
                                r.addImage("icon-zone").padRight(4);
                                r.add(Core.bundle.format("zone.requirement", other.wave, other.zone.localizedName())).color(Color.LIGHT_GRAY);
                                r.addImage(other.zone.bestWave() >= other.wave ? "icon-check-2" : "icon-cancel-2")
                                .color(other.zone.bestWave() >= other.wave ? Color.LIGHT_GRAY : Color.SCARLET).padLeft(3);
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
                cont.addButton(zone.canConfigure() ? "$configure" : Core.bundle.format("configure.locked", zone.configureWave), () -> loadout.show(zone, rebuildItems)).fillX().pad(3).disabled(b -> !zone.canConfigure());
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
            }
        });
        cont.row();

        Button button = cont.addButton(zone.locked() ? "$uncover" : "$launch", () -> {
            if(!data.isUnlocked(zone)){
                data.unlockContent(zone);
                ui.deploy.setup();
                setup(zone);
            }else{
                ui.deploy.hide();
                data.removeItems(zone.getLaunchCost());
                hide();
                control.playZone(zone);
            }
        }).minWidth(150f).margin(13f).padTop(5).disabled(b -> zone.locked() ? !canUnlock(zone) : !data.hasItems(zone.getLaunchCost())).uniformY().get();

        button.row();
        button.add(iteminfo);
    }

    private boolean canUnlock(Zone zone){
        if(data.isUnlocked(zone)){
            return true;
        }

        for(ZoneRequirement other : zone.zoneRequirements){
            if(other.zone.bestWave() < other.wave){
                return false;
            }
        }

        for(Block other : zone.blockRequirements){
            if(!data.isUnlocked(other)){
                return false;
            }
        }

        return true;
    }
}
