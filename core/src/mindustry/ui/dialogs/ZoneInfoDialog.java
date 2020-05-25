package mindustry.ui.dialogs;

import arc.*;
import arc.struct.*;
import arc.graphics.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import mindustry.game.*;
import mindustry.game.Objectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.Cicon;

import static mindustry.Vars.*;

//TODO remove
public class ZoneInfoDialog extends FloatingDialog{
    private LoadoutDialog loadout = new LoadoutDialog();

    public ZoneInfoDialog(){
        super("");

        titleTable.remove();
        addCloseButton();
    }

    public void show(SectorPreset zone){
        setup(zone);
        show();
    }

    private void setup(SectorPreset zone){
        cont.clear();

        Table iteminfo = new Table();
        Runnable rebuildItems = () -> {
            int i = 0;
            iteminfo.clear();

            if(!zone.unlocked()) return;

            for(ItemStack stack : zone.getLaunchCost()){
                if(stack.amount == 0) continue;

                if(i++ % 2 == 0){
                    iteminfo.row();
                }
                iteminfo.image(stack.item.icon(Cicon.small)).size(8 * 3).padRight(1);
                iteminfo.add(stack.amount + "").color(Color.lightGray).padRight(5);
            }
        };

        rebuildItems.run();

        cont.pane(cont -> {
            if(zone.locked()){
                cont.image(Icon.lock);
                cont.row();
                cont.add("$locked").padBottom(6);
                cont.row();

                cont.table(req -> {
                    req.defaults().left();

                    Array<Objectives.Objective> zones = zone.requirements.select(o -> !(o instanceof Unlock));

                    if(!zones.isEmpty()){
                        req.table(r -> {
                            r.add("$complete").colspan(2).left();
                            r.row();
                            for(Objectives.Objective o : zones){
                                r.image(Icon.terrain).padRight(4);
                                r.add(o.display()).color(Color.lightGray);
                                r.image(o.complete() ? Icon.ok : Icon.cancel, o.complete() ? Color.lightGray : Color.scarlet).padLeft(3);
                                r.row();
                            }
                        });
                    }

                    req.row();
                    Array<Unlock> blocks = zone.requirements.select(o -> o instanceof Unlock).as();

                    if(!blocks.isEmpty()){
                        req.table(r -> {
                            r.add("$research.list").colspan(2).left();
                            r.row();
                            for(Unlock blocko : blocks){
                                r.image(blocko.block.icon(Cicon.small)).size(8 * 3).padRight(5);
                                r.add(blocko.block.localizedName).color(Color.lightGray).left();
                                r.image(blocko.block.unlocked() ? Icon.ok : Icon.cancel, blocko.block.unlocked() ? Color.lightGray : Color.scarlet).padLeft(3);
                                r.row();
                            }

                        }).padTop(10);
                    }
                }).growX();

            }else{
                cont.add(zone.localizedName).color(Pal.accent).growX().center();
                cont.row();
                cont.image().color(Pal.accent).height(3).pad(6).growX();
                cont.row();
                cont.table(desc -> {
                    desc.left().defaults().left().width(Core.graphics.isPortrait() ? 350f : 500f);
                    desc.pane(t -> t.marginRight(12f).add(zone.description).wrap().growX()).fillX().maxHeight(mobile ? 300f : 450f).pad(2).padBottom(8f).get().setScrollingDisabled(true, false);
                    desc.row();

                    desc.table(t -> {
                        t.left();
                        t.add("$zone.resources").padRight(6);

                        /*
                        if(zone.resources.size > 0){
                            t.table(r -> {
                                t.left();
                                int i = 0;
                                for(Item item : zone.resources){
                                    r.image(item.icon(Cicon.small)).size(8 * 3);
                                    if(++i % 4 == 0){
                                        r.row();
                                    }
                                }
                            });
                        }else{
                            t.add("$none");
                        }*/
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
            }
            cont.marginRight(12f);
        });
        cont.row();

        /*
        cont.button(zone.canConfigure() ? "$configure" : Core.bundle.format("configure.locked", zone.configureObjective.display()),
        () -> loadout.show(zone.loadout.findCore().itemCapacity, zone.getStartingItems(), zone::resetStartingItems, zone::updateLaunchCost, rebuildItems)
        ).fillX().pad(3).disabled(b -> !zone.canConfigure());*/

        cont.row();

        Button button = cont.button(zone.locked() ? "$uncover" : "$launch", () -> {
            if(!data.isUnlocked(zone)){
                Sounds.unlock.play();
                data.unlockContent(zone);
                ui.planet.setup();
                setup(zone);
            }else{
                ui.planet.hide();
                data.removeItems(zone.getLaunchCost());
                hide();
                //control.playZone(zone);
            }
        }).minWidth(200f).margin(13f).padTop(5).disabled(b -> zone.locked() ? !zone.canUnlock() : !data.hasItems(zone.getLaunchCost())).uniformY().get();

        button.row();
        button.add(iteminfo);
    }
}
