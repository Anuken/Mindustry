package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.SaveIO.SaveException;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.type.Zone;

import static io.anuke.mindustry.Vars.*;

public class DeployDialog extends FloatingDialog{

    public DeployDialog(){
        super("$play");

        shown(this::setup);
    }

    void setup(){
        buttons.clear();
        cont.clear();

        addCloseButton();
        buttons.addImageTextButton("$techtree", "icon-tree", 16 * 2, () -> ui.tech.show()).size(230f, 64f);

        cont.stack(new Table(){{
            top().left().margin(10);

            ObjectIntMap<Item> items = data.items();
            for(Item item : content.items()){
                if(item.type == ItemType.material && data.isUnlocked(item)){
                    label(() -> items.get(item, 0) + "").left();
                    addImage(item.region).size(8*4).pad(4);
                    add("[LIGHT_GRAY]" + item.localizedName()).left();
                    row();
                }
            }

        }}, new Table(){{

            if(control.saves.getZoneSlot() == null){

                int i = 0;
                for(Zone zone : content.zones()){
                    table(t -> {
                        TextButton button = t.addButton(zone.localizedName(), () -> {
                            data.removeItems(zone.deployCost);
                            hide();
                            world.playZone(zone);
                        }).size(150f).disabled(b -> !data.hasItems(zone.deployCost) || !data.isUnlocked(zone)).get();

                        button.row();
                        button.table(req -> {
                            for(ItemStack stack : zone.deployCost){
                                req.addImage(stack.item.region).size(8 * 3);
                                req.add(stack.amount + "").left();
                            }
                        }).pad(3).growX();

                        button.row();
                        button.addImage("icon-zone-locked").visible(() -> !data.isUnlocked(zone));

                        button.update(() -> button.setText(data.isUnlocked(zone) ? zone.localizedName() : "???"));
                    }).pad(3);

                    if(++i % 4 == 0){
                        row();
                    }
                }
            }else{
                addButton(Core.bundle.format("resume", control.saves.getZoneSlot().getZone().localizedName()), () -> {
                    hide();
                    ui.loadAnd(() -> {
                        try{
                            control.saves.getZoneSlot().load();
                            state.set(State.playing);
                        }catch(SaveException e){ //make sure to handle any save load errors!
                            e.printStackTrace();
                            if(control.saves.getZoneSlot() != null) control.saves.getZoneSlot().delete();
                            ui.showInfo("$save.corrupted");
                            show();
                        }
                    });
                }).size(200f);
            }
        }}).grow();
    }
}
