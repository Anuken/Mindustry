package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.world.Tile;

public class PowerGraph{
    private ObjectSet<Tile> producers = new ObjectSet<>();
    private ObjectSet<Tile> consumers = new ObjectSet<>();
    private ObjectSet<Tile> all = new ObjectSet<>();

    public void distribute(Tile tile){

    }

    public void add(Tile tile){
        all.add(tile);
        if(tile.block().outputsPower){
            producers.add(tile);
        }
    }

    public void remove(Tile tile){
        all.add(tile);
        producers.remove(tile);
        consumers.remove(tile);
    }
}
