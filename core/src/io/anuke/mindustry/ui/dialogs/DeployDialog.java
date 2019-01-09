package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemType;

public class DeployDialog extends FloatingDialog{

    public DeployDialog(){
        super("$text.play");

        shown(this::setup);
    }

    void setup(){
        buttons().clear();
        content().clear();

        addCloseButton();

        content().stack(new Table(){{
            top().left().margin(10);

            ObjectIntMap<Item> items = Vars.data.items();
            for(Item item : Vars.content.items()){
                if(item.type == ItemType.material && Vars.data.isUnlocked(item)){
                    add(items.get(item, 0) + "").left();
                    addImage(item.region).size(8*4).pad(4);
                    add("[LIGHT_GRAY]" + item.localizedName()).left();
                    row();
                }
            }
        }}, new Table(){{
            addButton("Wasteland", () -> {
                hide();
                Vars.world.generator.playRandomMap();
            }).size(190f, 60f);
        }}).grow();
    }
}
