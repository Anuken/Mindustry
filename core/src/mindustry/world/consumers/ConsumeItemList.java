package mindustry.world.consumers;

import arc.struct.*;
import mindustry.content.*;
import mindustry.type.*;

public class ConsumeItemList extends ConsumeItemFilter{
    public Seq<Item> items = new Seq<>();

    public ConsumeItemList(Item... items){
        this();
        this.items = Seq.with(items);
    }

    public ConsumeItemList(){
        filter = item -> this.items.contains(item);
    }
}
