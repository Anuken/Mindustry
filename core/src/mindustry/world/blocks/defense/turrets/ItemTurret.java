package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;
import mindustry.world.meta.values.*;

import static mindustry.Vars.*;

public class ItemTurret extends Turret{
    public int maxAmmo = 30;
    public ObjectMap<Item, BulletType> ammoTypes = new ObjectMap<>();

    public ItemTurret(String name){
        super(name);
        hasItems = true;
    }

    /** Initializes accepted ammo map. Format: [item1, bullet1, item2, bullet2...] */
    protected void ammo(Object... objects){
        ammoTypes = OrderedMap.of(objects);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.itemCapacity);
        stats.add(BlockStat.ammo, new AmmoListValue<>(ammoTypes));
        consumes.add(new ConsumeItemFilter(i -> ammoTypes.containsKey(i)){
            @Override
            public void build(Building tile, Table table){
                MultiReqImage image = new MultiReqImage();
                content.items().each(i -> filter.get(i) && i.unlockedNow(), item -> image.add(new ReqImage(new ItemImage(item.icon(Cicon.medium)),
                    () -> tile != null && !((ItemTurretBuild)tile).ammo.isEmpty() && ((ItemEntry)((ItemTurretBuild)tile).ammo.peek()).item == item)));

                table.add(image).size(8 * 4);
            }

            @Override
            public boolean valid(Building entity){
                //valid when there's any ammo in the turret
                return !((ItemTurretBuild)entity).ammo.isEmpty();
            }

            @Override
            public void display(BlockStats stats){
                //don't display
            }
        });
    }

    public class ItemTurretBuild extends TurretBuild{

        @Override
        public void onProximityAdded(){
            super.onProximityAdded();

            //add first ammo item to cheaty blocks so they can shoot properly
            if(cheating() && ammo.size > 0){
                handleItem(this, ammoTypes.entries().next().key);
            }
        }

        @Override
        public void updateTile(){
            unit.ammo((float)unit.type().ammoCapacity * totalAmmo / maxAmmo);

            super.updateTile();
        }

        @Override
        public void displayBars(Table bars){
            super.displayBars(bars);

            bars.add(new Bar("blocks.ammo", Pal.ammo, () -> (float)totalAmmo / maxAmmo)).growX();
            bars.row();
        }

        @Override
        public int acceptStack(Item item, int amount, Teamc source){
            BulletType type = ammoTypes.get(item);

            if(type == null) return 0;

            return Math.min((int)((maxAmmo - totalAmmo) / ammoTypes.get(item).ammoMultiplier), amount);
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            for(int i = 0; i < amount; i++){
                handleItem(null, item);
            }
        }

        //currently can't remove items from turrets.
        @Override
        public int removeStack(Item item, int amount){
            return 0;
        }

        @Override
        public void handleItem(Building source, Item item){

            if(item == Items.pyratite){
                Events.fire(Trigger.flameAmmo);
            }

            BulletType type = ammoTypes.get(item);
            totalAmmo += type.ammoMultiplier;

            //find ammo entry by type
            for(int i = 0; i < ammo.size; i++){
                ItemEntry entry = (ItemEntry)ammo.get(i);

                //if found, put it to the right
                if(entry.item == item){
                    entry.amount += type.ammoMultiplier;
                    ammo.swap(i, ammo.size - 1);
                    return;
                }
            }

            //must not be found
            ammo.add(new ItemEntry(item, (int)type.ammoMultiplier));

            //fire events for the tutorial
            if(state.rules.tutorial){
                Events.fire(new TurretAmmoDeliverEvent());
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return ammoTypes.get(item) != null && totalAmmo + ammoTypes.get(item).ammoMultiplier <= maxAmmo;
        }

        @Override
        public byte version(){
            return 2;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.b(ammo.size);
            for(AmmoEntry entry : ammo){
                ItemEntry i = (ItemEntry)entry;
                write.s(i.item.id);
                write.s(i.amount);
            }
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            int amount = read.ub();
            for(int i = 0; i < amount; i++){
                Item item = Vars.content.item(revision < 2 ? read.ub() : read.s());
                short a = read.s();
                totalAmmo += a;

                //only add ammo if this is a valid ammo type
                if(ammoTypes.containsKey(item)){
                    ammo.add(new ItemEntry(item, a));
                }
            }
        }
    }

    class ItemEntry extends AmmoEntry{
        protected Item item;

        ItemEntry(Item item, int amount){
            this.item = item;
            this.amount = amount;
        }

        @Override
        public BulletType type(){
            return ammoTypes.get(item);
        }
    }
}
