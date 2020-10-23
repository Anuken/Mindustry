package mindustry.world.meta.values;

import arc.func.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BlockFilterValue implements StatValue{
    public final Boolf<Block> pred;

    public BlockFilterValue(Boolf<Block> pred){
        this.pred = pred;
    }

    @Override
    public void display(Table table){
        Seq<Block> list = content.blocks().select(pred);

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
