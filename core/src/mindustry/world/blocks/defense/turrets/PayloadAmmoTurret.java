package mindustry.world.blocks.defense.turrets;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//TODO visuals!
public class PayloadAmmoTurret extends Turret{
    public ObjectMap<UnlockableContent, BulletType> ammoTypes = new ObjectMap<>();

    protected UnlockableContent[] ammoKeys;

    public PayloadAmmoTurret(String name){
        super(name);

        maxAmmo = 3;
    }

    /** Initializes accepted ammo map. Format: [block1, bullet1, block2, bullet2...] */
    public void ammo(Object... objects){
        ammoTypes = ObjectMap.of(objects);
    }

    /** Makes copies of all bullets and limits their range. */
    public void limitRange(){
        limitRange(1f);
    }

    /** Makes copies of all bullets and limits their range. */
    public void limitRange(float margin){
        for(var entry : ammoTypes.copy().entries()){
            entry.value.lifetime = (range + margin) / entry.value.speed;
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.itemCapacity);
        stats.add(Stat.ammo, StatValues.ammo(ammoTypes));
    }

    @Override
    public void init(){
        consume(new ConsumePayloadFilter(i -> ammoTypes.containsKey(i)){
            @Override
            public void build(Building build, Table table){
                MultiReqImage image = new MultiReqImage();
                content.blocks().each(i -> filter.get(i) && i.unlockedNow(), block -> image.add(new ReqImage(new Image(block.uiIcon),
                () -> build instanceof PayloadTurretBuild it && !it.payloads.isEmpty() && it.currentBlock() == block)));

                table.add(image).size(8 * 4);
            }

            @Override
            public float efficiency(Building build){
                //valid when there's any ammo in the turret
                return build instanceof PayloadTurretBuild it && it.payloads.any() ? 1f : 0f;
            }

            @Override
            public void display(Stats stats){
                //don't display
            }
        });

        ammoKeys = ammoTypes.keys().toSeq().toArray(Block.class);

        super.init();
    }

    public class PayloadTurretBuild extends TurretBuild{
        public PayloadSeq payloads = new PayloadSeq();

        public UnlockableContent currentBlock(){
            for(var block : ammoKeys){
                if(payloads.contains(block)){
                    return block;
                }
            }
            return null;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return payload instanceof BuildPayload build && payloads.total() < maxAmmo && ammoTypes.containsKey(build.block());
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            payloads.add(((BuildPayload)payload).block());
        }

        @Override
        public boolean hasAmmo(){
            return payloads.total() > 0;
        }

        @Override
        public BulletType useAmmo(){
            for(var block : ammoKeys){
                if(payloads.contains(block)){
                    payloads.remove(block);
                    return ammoTypes.get(block);
                }
            }
            return null;
        }

        @Override
        public BulletType peekAmmo(){
            for(var block : ammoKeys){
                if(payloads.contains(block)){
                    return ammoTypes.get(block);
                }
            }
            return null;
        }

        @Override
        public PayloadSeq getPayloads(){
            return payloads;
        }

        @Override
        public void updateTile(){
            totalAmmo = payloads.total();
            unit.ammo((float)unit.type().ammoCapacity * totalAmmo / maxAmmo);

            super.updateTile();
        }

        @Override
        public void displayBars(Table bars){
            super.displayBars(bars);

            bars.add(new Bar("stat.ammo", Pal.ammo, () -> (float)totalAmmo / maxAmmo)).growX();
            bars.row();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            payloads.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            payloads.read(read);
            //TODO remove invalid ammo
        }
    }
}
