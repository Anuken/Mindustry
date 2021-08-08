package mindustry.world.blocks.payloads;

import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.storage.*;

import static mindustry.Vars.*;

/** Generic building that produces other buildings. */
public class PayloadSource extends PayloadBlock{

    public PayloadSource(String name){
        super(name);

        size = 3;
        update = true;
        outputsPayload = true;
        hasPower = false;
        rotate = true;
        configurable = true;
        //make sure to display large units.
        clipSize = 120;
        noUpdateDisabled = true;

        config(Block.class, (PayloadSourceBuild build, Block block) -> {
            if(canProduce(block) && build.block != block){
                build.block = block;
                build.unit = null;
                build.payload = null;
                build.scl = 0f;
            }
        });

        config(UnitType.class, (PayloadSourceBuild build, UnitType unit) -> {
            if(canProduce(unit) && build.unit != unit){
                build.unit = unit;
                build.block = null;
                build.payload = null;
                build.scl = 0f;
            }
        });

        configClear((PayloadSourceBuild build) -> {
            build.block = null;
            build.unit = null;
            build.payload = null;
            build.scl = 0f;
        });
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(outRegion, req.drawx(), req.drawy(), req.rotation * 90);
        Draw.rect(topRegion, req.drawx(), req.drawy());
    }

    public boolean canProduce(Block b){
        return b.isVisible() && b.size < size && !(b instanceof CoreBlock) && !state.rules.bannedBlocks.contains(b);
    }

    public boolean canProduce(UnitType t){
        return !t.isHidden() && !t.isBanned();
    }
    
    public class PayloadSourceBuild extends PayloadBlockBuild<Payload>{
        public UnitType unit;
        public Block block;
        public float scl;

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(table,
                content.blocks().select(PayloadSource.this::canProduce).<UnlockableContent>as()
                .and(content.units().select(PayloadSource.this::canProduce).as()),
            () -> (UnlockableContent)config(), this::configure);
        }

        @Override
        public Object config(){
            return unit == null ? block : unit;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return false;
        }

        @Override
        public void updateTile(){
            if(payload == null){
                scl = 0f;
                if(unit != null){
                    payload = new UnitPayload(unit.create(team));
                }else if(block != null){
                    payload = new BuildPayload(block, team);
                }
                payVector.setZero();
                payRotation = rotdeg();
            }
            scl = Mathf.lerpDelta(scl, 1f, 0.1f);

            moveOutPayload();
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());
            Draw.rect(topRegion, x, y);

            Draw.scl(scl);
            drawPayload();
            Draw.reset();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(unit == null ? -1 : unit.id);
            write.s(block == null ? -1 : block.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            unit = Vars.content.unit(read.s());
            block = Vars.content.block(read.s());
        }
    }
}
