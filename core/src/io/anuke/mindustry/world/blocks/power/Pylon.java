package io.anuke.mindustry.world.blocks.power;

import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.world.*;

import static io.anuke.mindustry.Vars.*;

public class Pylon extends PowerNode{
    private static final IntSet healed = new IntSet();

    public Pylon(String name){
        super(name);
        entityType = PylonEntity::new;
    }

    @Override
    public void update(Tile tile){
        super.update(tile);

        int tileRange = (int)laserRange;
        int realRange = tileRange * tilesize;
        float realBoost = 1f;
        float reload = 2f;

        healed.clear();
        for(int x = -tileRange + tile.x; x <= tileRange + tile.x; x++){
            for(int y = -tileRange + tile.y; y <= tileRange + tile.y; y++){
                if(!Mathf.within(x * tilesize, y * tilesize, tile.drawx(), tile.drawy(), realRange)) continue;

                Tile other = world.ltile(x, y);

                if(other == null) continue;

                if(other.getTeamID() == tile.getTeamID() && !healed.contains(other.pos()) && other.entity != null){
                    if(other.entity.timeScale <= realBoost){
                        other.entity.timeScaleDuration = Math.max(other.entity.timeScaleDuration, reload + 1f);
                        other.entity.timeScale = Math.max(other.entity.timeScale, realBoost);
                    }
                    healed.add(other.pos());
                }
            }
        }
    }

    class PylonEntity extends TileEntity implements ProjectorTrait{

        float realRadius(){
            return laserRange * tilesize;
        }

        @Override
        public void drawOver(){
            Draw.color(Color.white);
            Draw.alpha(1f - power.status);
            Fill.circle(x, y, realRadius());
            Draw.color();
        }

        @Override
        public void drawSimple(){
            float rad = realRadius();
            Draw.color(color);
            Lines.stroke(1.5f);
            Draw.alpha(0.17f);
            Fill.circle(x, y, rad);
            Draw.alpha(1f);
            Lines.circle(x, y, rad);
            Draw.reset();
        }

        @Override
        public Color accent(){
            return Color.valueOf("add8e6");
        }

        @Override
        public String projectorSet(){
            return "pylonSet";
        }

        @Override
        public void draw(){
            Draw.color(color);
            Fill.circle(x, y, realRadius());
            Draw.color();
        }

        @Override
        public EntityGroup targetGroup(){
            return projectorGroup;
        }
    }
}
