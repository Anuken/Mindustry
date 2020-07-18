package mindustry.world.meta;

import arc.struct.*;
import mindustry.gen.*;

public class Producers{
    private Seq<Produce> producers = new Seq<>();

    public void add(Produce prod){
        producers.add(prod);
    }

    interface Produce{
        void add(Building entity);
    }
}
