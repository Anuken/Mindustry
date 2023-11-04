package mindustry.ai;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.Schematic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;
import mindustry.world.meta.*;

import java.io.*;

import static mindustry.Vars.*;

public class BaseRegistry {

    // Cores, sorted by tier
    public Seq<BasePart> cores = new Seq<>();

    // Parts with no requirement
    public Seq<BasePart> parts = new Seq<>();

    // Map associating Content (e.g., items) with a sequence of BasePart objects
    public ObjectMap<Content, Seq<BasePart>> reqParts = new ObjectMap<>();

    // Map associating Item objects with OreBlock objects
    public ObjectMap<Item, OreBlock> ores = new ObjectMap<>();

    // Map associating Item objects with Floor objects
    public ObjectMap<Item, Floor> oreFloors = new ObjectMap<>();

    // Get BasePart objects that require a specific resource
    public Seq<BasePart> forResource(Content item) {
        return reqParts.get(item, Seq::new);
    }

    // Load and initialize base parts
    public void load() {
        cores.clear();
        parts.clear();
        reqParts.clear();

        // Load ore types and corresponding items
        for (Block block : content.blocks()) {
            if (block instanceof OreBlock ore && ore.itemDrop != null && !ore.wallOre && !ores.containsKey(ore.itemDrop)) {
                ores.put(ore.itemDrop, ore);
            } else if (block.isFloor() && block.asFloor().itemDrop != null && !oreFloors.containsKey(block.asFloor().itemDrop)) {
                oreFloors.put(block.asFloor().itemDrop, block.asFloor());
            }
        }

        // Read base part names from a file
        String[] names = Core.files.internal("basepartnames").readString().split("\n");

        for (String name : names) {
            try {
                // Read schematic data for each base part
                Schematic schematic = Schematics.read(Core.files.internal("baseparts/" + name));

                // Create a BasePart object for the base part
                BasePart part = new BasePart(schematic);
                Tmp.v1.setZero();
                int drills = 0;

                for (Stile tile : schematic.tiles) {

                    // Keep track of core type
                    if (tile.block instanceof CoreBlock) {
                        part.core = tile.block;
                    }

                    // Save the required resource based on item source
                    if (tile.block instanceof ItemSource) {
                        Item config = (Item) tile.config;
                        if (config != null) part.required = config;
                    }

                    // Similar logic for liquids (not used yet)
                    if (tile.block instanceof LiquidSource) {
                        Liquid config = (Liquid) tile.config;
                        if (config != null) part.required = config;
                    }

                    // Calculate averages for drills and pumps
                    if (tile.block instanceof Drill || tile.block instanceof Pump) {
                        Tmp.v1.add(tile.x * tilesize + tile.block.offset, tile.y * tilesize + tile.block.offset);
                        drills++;
                    }
                }
                // Remove tiles with sandboxOnly visibility
                schematic.tiles.removeAll(s -> s.block.buildVisibility == BuildVisibility.sandboxOnly);

                // Calculate the tier of the base part
                part.tier = schematic.tiles.sumf(s -> Mathf.pow(s.block.buildCost / s.block.buildCostMultiplier, 1.4f));

                if (part.core != null) {
                    cores.add(part);
                } else if (part.required == null) {
                    parts.add(part);
                }

                if (drills > 0) {
                    // Calculate the center coordinates for drills
                    Tmp.v1.scl(1f / drills).scl(1f / tilesize);
                    part.centerX = (int) Tmp.v1.x;
                    part.centerY = (int) Tmp.v1.y;
                } else {
                    // Set default center coordinates
                    part.centerX = part.schematic.width / 2;
                    part.centerY = part.schematic.height / 2;
                }

                if (part.required != null && part.core == null) {
                    reqParts.get(part.required, Seq::new).add(part);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Sort cores, parts, and reqParts
        cores.sort(b -> b.tier);
        parts.sort();
        reqParts.each((key, arr) -> arr.sort());
    }

    // Nested class to represent a base part
    public static class BasePart implements Comparable<BasePart> {
        public final Schematic schematic;
        // Offsets for drills
        public int centerX, centerY;
        public @Nullable Content required;
        public @Nullable Block core;
        // Total build cost tier
        public float tier;

        public BasePart(Schematic schematic) {
            this.schematic = schematic;
        }

        // Compare base parts based on their tier
        @Override
        public int compareTo(BasePart other) {
            return Float.compare(tier, other.tier);
        }
    }
}
