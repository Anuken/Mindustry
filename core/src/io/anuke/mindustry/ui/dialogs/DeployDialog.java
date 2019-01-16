package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.Vars;
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

        cont.stack(new Table(){{
            top().left().margin(10);

            ObjectIntMap<Item> items = data.items();
            for(Item item : Vars.content.items()){
                if(item.type == ItemType.material && data.isUnlocked(item)){
                    add(items.get(item, 0) + "").left();
                    addImage(item.region).size(8*4).pad(4);
                    add("[LIGHT_GRAY]" + item.localizedName()).left();
                    row();
                }
            }

        }}, new Table(){{

            for(Zone zone : Vars.content.zones()){
                if(data.isUnlocked(zone)){
                    table(t -> {
                        t.addButton(zone.localizedName(), () -> {
                            data.removeItems(zone.deployCost);
                            hide();
                            world.playZone(zone);
                        }).size(150f)/*.disabled(b -> !data.hasItems(zone.deployCost))*/;
                        t.row();
                        t.table(req -> {
                            req.left();
                            for(ItemStack stack : zone.deployCost){
                                req.addImage(stack.item.region).size(8*3);
                                req.add(stack.amount + "").left();
                            }
                        }).pad(3).growX();
                    }).pad(3);
                }
            }
        }}).grow();
    }
}
