package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.type.Zone.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class ZoneInfoDialog extends FloatingDialog{
    private LoadoutDialog loadout = new LoadoutDialog();

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
                iteminfo.add(stack.amount + "").color(Color.lightGray).padRight(5);
            }
        };

        rebuildItems.run();

        cont.pane(cont -> {
            if(zone.locked()){
                cont.addImage(Icon.locked);
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
                                r.addImage(Icon.terrain).padRight(4);
                                r.add(Core.bundle.format("zone.requirement", other.wave, other.zone.localizedName())).color(Color.lightGray);
                                r.addImage(other.zone.bestWave() >= other.wave ? Icon.checkSmall : Icon.cancelSmall, other.zone.bestWave() >= other.wave ? Color.lightGray : Color.scarlet).padLeft(3);
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
                                r.addImage(block.icon(Block.Icon.small)).size(8 * 3).padRight(4);
                                r.add(block.localizedName).color(Color.lightGray);
                                r.addImage(data.isUnlocked(block) ? Icon.checkSmall : Icon.cancelSmall, data.isUnlocked(block) ? Color.lightGray : Color.scarlet).padLeft(3);
                                r.row();
                            }

                        }).padTop(10);
                    }
                }).growX();

            }else{
                cont.add(zone.localizedName()).color(Pal.accent).growX().center();
                cont.row();
                cont.addImage().color(Pal.accent).height(3).pad(6).growX();
                cont.row();
                cont.table(desc -> {
                    desc.left().defaults().left().width(Core.graphics.isPortrait() ? 350f : 500f);
                    desc.pane(t -> t.marginRight(12f).add(zone.description).wrap().growX()).fillX().maxHeight(mobile ? 300f : 450f).pad(2).padBottom(8f).get().setScrollingDisabled(true, false);
                    desc.row();

                    desc.table(t -> {
                        t.left();
                        t.add("$zone.resources").padRight(6);

                        if(zone.resources.length > 0){
                            t.table(r -> {
                                t.left();
                                int i = 0;
                                for(Item item : zone.resources){
                                    r.addImage(item.icon(Item.Icon.medium)).size(8 * 3);
                                    if(++i % 4 == 0){
                                        r.row();
                                    }
                                }
                            });
                        }else{
                            t.add("$none");
                        }
                    });

                    Rules rules = zone.getRules();

                    desc.row();
                    desc.add(Core.bundle.format("zone.objective", Core.bundle.get(!rules.attackMode ? "zone.objective.survival" : "zone.objective.attack")));

                    if(zone.bestWave() > 0){
                        desc.row();
                        desc.add(Core.bundle.format("bestwave", zone.bestWave()));
                    }
                });

                cont.row();

                cont.addButton(zone.canConfigure() ? "$configure" : Core.bundle.format("configure.locked", zone.configureWave),
                () -> loadout.show(zone.loadout.core().itemCapacity, zone::getStartingItems, zone::resetStartingItems, zone::updateLaunchCost, rebuildItems, item -> data.getItem(item) > 0 && item.type == ItemType.material)
                ).fillX().pad(3).disabled(b -> !zone.canConfigure());
            }
            cont.marginRight(12f);
        });
        cont.row();

        Button button = cont.addButton(zone.locked() ? "$uncover" : "$launch", () -> {
            if(!data.isUnlocked(zone)){
                Sounds.unlock.play();
                data.unlockContent(zone);
                ui.deploy.setup();
                setup(zone);
            }else{
                ui.deploy.hide();
                data.removeItems(zone.getLaunchCost());
                hide();
                control.playZone(zone);
            }
        }).minWidth(150f).margin(13f).padTop(5).disabled(b -> zone.locked() ? !zone.canUnlock() : !data.hasItems(zone.getLaunchCost())).uniformY().get();

        button.row();
        button.add(iteminfo);
    }
}
