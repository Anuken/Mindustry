package mindustry.world.draw;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.part.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.*;

public class DrawBlockParts extends DrawBlock{
    public Seq<DrawPart> parts = new Seq<>();
    public TextureRegion preview;

    @Override
    public void getRegionsToOutline(Block block, Seq<TextureRegion> out){
        for(var part : parts){
            part.getOutlines(out);
        }
    }

    @Override
    public void drawPlan(Block block, BuildPlan plan, Eachable<BuildPlan> list){
        if(preview.found()){
            Draw.rect(preview, plan.drawx(), plan.drawy(), block.rotate ? plan.rotation * 90f - 90f : 0f);
        }
    }

    @Override
    public void draw(Building build){
        if(parts.size > 0){
            float progress = build.progress();

            var params = DrawPart.params.set(build.warmup(), 1f - progress, 1f - progress, 0f, 0f, 0f, build.x, build.y, build.rotdeg());

            for(var part : parts){
                part.draw(params);
            }
        }
    }

    @Override
    public void load(Block block){
        preview = Core.atlas.find(block.name + "-preview");

        for(var part : parts){
            part.load(block.name);
        }
    }

    @Override
    public TextureRegion[] icons(Block block){
        return preview.found() ? new TextureRegion[]{preview} : super.icons(block);
    }
}
