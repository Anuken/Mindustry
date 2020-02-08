package mindustry.world.blocks.storage;

import arc.Core;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.Thresholds;
import mindustry.entities.traits.BuilderTrait.*;
import mindustry.entities.type.*;
import mindustry.graphics.Pal;
import mindustry.type.*;
import mindustry.ui.Bar;
import mindustry.world.*;
import mindustry.world.blocks.*;

import java.io.*;

import static mindustry.Vars.content;

public class ThreshUnloader extends Block{
    public float speed = 1f;

    public final int timerUnload = timers++;
    private static Threshold lastThreshold;

    public ThreshUnloader(String name){
        super(name);
        update = true;
        solid = true;
        health = 75;
        hasItems = true;
        configurable = true;
        entityType = ThreshUnloaderEntity::new;
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.remove("items");
    }

    @Override
    public void playerPlaced(Tile tile){
        if(lastThreshold != null){
            tile.configure(lastThreshold.id);
        }
    }

    @Override
    public void configured(Tile tile, Player player, int value){
        tile.entity.items.clear();
        tile.<ThreshUnloaderEntity>ent().threshold = content.threshold(value);
    }

    @Override
    public boolean canDump(Tile tile, Tile to, Item item){
        return !(to.block() instanceof StorageBlock);
    }

    @Override
    public void update(Tile tile){
        ThreshUnloaderEntity entity = tile.ent();

        if(tile.entity.timer.get(timerUnload, speed * entity.polladjust / entity.timeScale) && tile.entity.items.total() == 0){
            for(Tile other : tile.entity.proximity()){
                if(other.interactable(tile.getTeam()) && other.block().unloadable && other.block().hasItems && entity.items.total() == 0){
                    // If the target is a vault, try to take an item that is above threshold.
                    if(other.block() instanceof Vault) {
                        int nThreshold = (int)(other.block().getMaximumAccepted(other, null) * entity.threshold.vaultFrac);
                        if(other.entity.items.total() > nThreshold) {
                            Item iitem = other.entity.items.takeMaxItem(nThreshold);
                            if (iitem != null) {
                                // Every time an item gets deliverd, cut the polling rate penalty by 15% to quickly bring the rate back to full.
                                // Also produces a neat ramp-up visual effect as the unloader comes out hibernation.
                                entity.polladjust = entity.polladjust * 0.85f + 0.15f;
                                offloadNear(tile, iitem);
                            } else {
                                // Since the max search algorithm is mildly CPU-intensive, reduce the polling rate by 3% each time it fails to return anything to pass along.
                                entity.polladjust *= 1.03;
                                // Cap the polling rate reduction to 100X
                                if (entity.polladjust > 100f)
                                    entity.polladjust = 100f;
                            }
                        }
                    }
                    // If the target is a launcher loaded above threshold, take whichever item there is the most of.
                    if(other.block() instanceof LaunchPad) {
                        int nThreshold = (int)(other.block().getMaximumAccepted(other, null) * entity.threshold.launcherFrac);
                        if(other.entity.items.total() > nThreshold) {
                            offloadNear(tile, other.entity.items.takeMaxItem(0));
                            entity.polladjust = entity.polladjust * 0.85f + 0.15f;
                        } else {
                            entity.polladjust *= 1.03;
                            if (entity.polladjust > 100f)
                                entity.polladjust = 100f;
                        }
                    }
                }
            }
        }

        // Still got to empty items that got pushed into the block to avoid stalling.
        if(entity.items.total() > 0){
            tryDump(tile);
        }
    }

    /**
     * Removes an item and returns it. If item is not null, it should return the item.
     * Returns null if no items are there.
     */
    private Item removeItem(Tile tile, Item item){
        TileEntity entity = tile.entity;

        if(item == null){
            return entity.items.take();
        }else{
            if(entity.items.has(item)){
                entity.items.remove(item, 1);
                return item;
            }

            return null;
        }
    }

    /**
     * Returns whether this storage block has the specified item.
     * If the item is null, it should return whether it has ANY items.
     */
    private boolean hasItem(Tile tile, Item item){
        TileEntity entity = tile.entity;
        if(item == null){
            return entity.items.total() > 0;
        }else{
            return entity.items.has(item);
        }
    }

    // Reflect configuration status with the associated sprite.
    @Override
    public void draw(Tile tile){
        ThreshUnloader.ThreshUnloaderEntity entity = tile.ent();
        Draw.rect("threshunloader" + entity.threshold.id, tile.worldx(), tile.worldy());
    }

    @Override
    public void buildConfiguration(Tile tile, Table table){
        ThreshUnloaderEntity entity = tile.ent();
        ThresholdSelection.buildThresholdTable(table, () -> entity.threshold, threshold -> {
            lastThreshold = threshold;
            tile.configure(threshold == null ? entity.threshold.id : threshold.id);
        });
    }

    public static class ThreshUnloaderEntity extends TileEntity{
        public Threshold threshold = Thresholds.threshold00;
        public float polladjust = 10f;

        @Override
        public int config(){return threshold.id;}

        @Override
        public void write(DataOutput stream) throws IOException{
            super.write(stream);
            stream.writeByte(threshold.id);
        }

        @Override
        public void read(DataInput stream, byte revision) throws IOException{
            super.read(stream, revision);
            byte id = stream.readByte();

            if(id>=content.thresholds().size)
                id = (byte)(content.thresholds().size-1);

            threshold = content.thresholds().get(id);
        }
    }
}
