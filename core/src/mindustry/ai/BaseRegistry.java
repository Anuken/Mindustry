package mindustry.ai;

import arc.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.tilesize;

public class BaseRegistry{
    public Array<BasePart> cores = new Array<>();
    public Array<BasePart> parts = new Array<>();
    public ObjectMap<Item, Array<BasePart>> itemParts = new ObjectMap<>();

    public Array<BasePart> forItem(Item item){
        return itemParts.get(item, Array::new);
    }

    public void load(){
        cores.clear();
        parts.clear();
        itemParts.clear();

        String[] names = Core.files.internal("basepartnames").readString().split("\n");

        for(String name : names){
            try{
                Schematic schem = Schematics.read(Core.files.internal("baseparts/" + name));

                BasePart part = new BasePart(schem);
                Tmp.v1.setZero();
                int drills = 0;

                for(Stile tile : schem.tiles){
                    //make note of occupied positions
                    tile.block.iterateTaken(tile.x, tile.y, part.occupied::set);

                    //keep track of core type
                    if(tile.block instanceof CoreBlock){
                        part.core = tile.block;
                    }

                    //save the required resource based on item source - multiple sources are not allowed
                    if(tile.block instanceof ItemSource){
                        Item config = (Item)tile.config;
                        if(config != null) part.requiredItem = config;
                    }

                    //same for liquids - this is not used yet
                    if(tile.block instanceof LiquidSource){
                        Liquid config = (Liquid)tile.config;
                        if(config != null) part.requiredLiquid = config;
                    }

                    //calculate averages
                    if(tile.block instanceof Drill){
                        Tmp.v1.add(tile.x*tilesize + tile.block.offset(), tile.y*tilesize + tile.block.offset());
                        drills ++;
                    }
                }
                schem.tiles.removeAll(s -> s.block.buildVisibility == BuildVisibility.sandboxOnly);

                part.tier = schem.tiles.sumf(s -> s.block.buildCost / s.block.buildCostMultiplier);

                if(part.core != null){
                    cores.add(part);
                }else if(part.requiredItem == null){
                    parts.add(part);
                }

                if(drills > 0){
                    Tmp.v1.scl(1f / drills).scl(1f / tilesize);
                    part.centerX = (int)Tmp.v1.x;
                    part.centerY = (int)Tmp.v1.y;
                }else{
                    part.centerX = part.schematic.width/2;
                    part.centerY = part.schematic.height/2;
                }

                if(part.requiredItem != null){
                    itemParts.get(part.requiredItem, Array::new).add(part);
                }
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }

        cores.sort(Structs.comps(Structs.comparingFloat(b -> b.core.health), Structs.comparingFloat(b -> b.tier)));
        parts.sort();
        itemParts.each((key, arr) -> arr.sort());
    }

    public static class BasePart implements Comparable<BasePart>{
        public final Schematic schematic;
        public final GridBits occupied;

        //offsets for drills
        public int centerX, centerY;

        public @Nullable Liquid requiredLiquid;
        public @Nullable Item requiredItem;
        public @Nullable Block core;

        //total build cost
        public float tier;

        public BasePart(Schematic schematic){
            this.schematic = schematic;
            this.occupied = new GridBits(schematic.width, schematic.height);
        }

        @Override
        public int compareTo(BasePart other){
            return Float.compare(tier, other.tier);
        }
    }
}
