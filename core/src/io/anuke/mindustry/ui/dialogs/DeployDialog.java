package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.type.Item;

public class DeployDialog extends FloatingDialog{

    public DeployDialog(){
        super("$text.play");

        shown(this::setup);
    }

    void setup(){
        content().clear();

        content().stack(new Table(){{
            top().left().margin(10);

            ObjectIntMap<Item> items = Vars.data.items();
            for(Item item : Vars.content.items()){
                if(Vars.data.isUnlocked(item)){
                    add(items.get(item, 0) + "");
                    addImage(item.region).size(8*3).pad(3);
                    add(item.localizedName());
                    row();
                }
            }
        }}, new Table(){{
            addButton("$text.play", () -> Vars.world.generator.playRandomMap()).margin(15);
        }}).grow();
    }
}
