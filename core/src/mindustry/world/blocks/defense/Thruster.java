package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class Thruster extends Wall{
    public int topVariants;

    public TextureRegion[] topRegions;
    public TextureRegion topRegion;

    public Thruster(String name){
        super(name);
        rotate = true;
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(topRegions[0], req.drawx(), req.drawy(), req.rotation * 90);
    }

    @Override
    public void load(){
        super.load();

        if(topVariants != 0){
            topRegions = new TextureRegion[topVariants];

            for(int i = 0; i < topVariants; i++){
                topRegions[i] = Core.atlas.find(name + "-top" + (i + 1));
            }
        }
        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, topRegions[0]};
    }

    public class ThrusterBuild extends WallBuild{

        @Override
        public void draw(){
            super.draw();

            Draw.rect(variants == 0 ? topRegion : topRegions[Mathf.randomSeed(tile.pos(), 0, Math.max(0, variantRegions.length - 1))], x, y, rotdeg());
        }
    }
}
