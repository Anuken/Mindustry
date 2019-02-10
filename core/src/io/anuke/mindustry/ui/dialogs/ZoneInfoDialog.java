package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.scene.ui.Button;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
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

    public void show(Zone zone){
        setup(zone);
        show();
    }

    private void setup(Zone zone){
        cont.clear();

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
                                r.addImage(data.isCompleted(other) ? "icon-check-2" : "icon-cancel-2")
                                .color(data.isCompleted(other) ? Color.LIGHT_GRAY : Color.SCARLET).padLeft(3);
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
                                r.add(block.formalName).color(Color.LIGHT_GRAY);
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

                if(data.getWaveScore(zone) > 0){
                    cont.row();
                    cont.add(Core.bundle.format("bestwave", data.getWaveScore(zone)));
                }
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
                data.removeItems(zone.deployCost);
                hide();
                world.playZone(zone);
            }
        }).size(300f, 70f).padTop(5).disabled(b -> zone.locked() ? !canUnlock(zone) : !data.hasItems(zone.deployCost)).get();

        button.row();
        button.table(r -> {
            ItemStack[] stacks = zone.unlocked() ? zone.deployCost : zone.itemRequirements;
            for(ItemStack stack : stacks){
                r.addImage(stack.item.icon(Item.Icon.medium)).size(8*3).padRight(1);
                r.add(stack.amount + "").color(Color.LIGHT_GRAY).padRight(5);
            }
        });
    }

    private boolean canUnlock(Zone zone){
        if(data.isUnlocked(zone)){
            return true;
        }

        for(Zone other : zone.zoneRequirements){
            if(!data.isCompleted(other)){
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
