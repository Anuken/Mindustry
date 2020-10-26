package mindustry.world.meta.values;

import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class BlockListValue implements StatValue{
    public final Seq<Block> list;

    public BlockListValue(Seq<Block> list){
        this.list = list;
    }

    @Override
    public void display(Table table){

        table.table(l -> {
            l.left();

            for(int i = 0; i < list.size; i++){
                Block item = list.get(i);

                l.image(item.icon(Cicon.small)).size(8 * 3).padRight(2).padLeft(2).padTop(3).padBottom(3);
                l.add(item.localizedName).left().padLeft(1).padRight(4);
                if(i % 5 == 4){
                    l.row();
                }
            }
        });
    }
}
