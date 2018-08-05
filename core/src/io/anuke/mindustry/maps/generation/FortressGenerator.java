package io.anuke.mindustry.maps.generation;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.StructureFormat.StructBlock;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.defense.turrets.ItemTurret;
import io.anuke.mindustry.world.blocks.defense.turrets.PowerTurret;
import io.anuke.mindustry.world.blocks.defense.turrets.Turret;

import static io.anuke.mindustry.Vars.world;

public class FortressGenerator{
    private final static int minCoreDst = 60;
    private static Structure[] structures;

    private int enemyX, enemyY, coreX, coreY;
    private Team team;
    private Generation gen;

    private static void init(){
        if(structures != null) return;

        String vaults = "BQMADWNhcmJpZGUtZHJpbGwCAA10dW5nc3Rlbi13YWxsAQATdHVuZ3N0ZW4td2FsbC1sYXJnZQAAA2FpcgQABXZhdWx0CQUAAgABAAEAAQABAAIAAAABAAAAAAACAAAAAQAAAAABAQABAgEBAQAAAAIAAgMBAAEAAAICAAAAAAAAAgACAgAABAIAAAIAAgIAAAAAAAACAAICAgMCAwIDAgM=";

        structures = new Structure[]{
            //tiny duo outpost
            new Structure(0.03f, Items.tungsten, "BAMADnR1bmdzdGVuLWRyaWxsAgADZHVvAQANdHVuZ3N0ZW4td2FsbAAAA2FpcgMFAQABAwEDAQMBAAEAAgMDAwIDAQABAAEBAQEBAQEA"),

            //basic outposts with duos
            new Structure(0.03f, Items.tungsten, "BAIAA2R1bwMADWNhcmJpZGUtZHJpbGwBAA10dW5nc3Rlbi13YWxsAAADYWlyBQUAAAEAAQABAAAAAQABAAIAAQABAAEAAgADAwIAAQABAAEAAgABAAEAAAABAAEAAQAAAA=="),

            //more advanced duo outpost
            new Structure(0.04f, Items.lead, "BwYADnR1bmdzdGVuLWRyaWxsAwADZHVvBAAIc3BsaXR0ZXIBAA10dW5nc3Rlbi13YWxsAgATdHVuZ3N0ZW4td2FsbC1sYXJnZQAAA2FpcgUACGNvbnZleW9yCQkAAAAAAQEBAQEBAQEBAgAAAAAAAAICAAEDAAQDAwACAgAAAAABAgACAAABAgUCAQEAAAAAAQABAgMAAQIBAgUCAQEBAQMAAQABAgQCBQMFAwYCBQEFAQQDAQABAgMAAQEBAQUAAQMBAwMAAQABAwICAAMBAQUAAQMCAgADAQMAAAAAAAIDAAQDAwAAAwADAAAAAAAAAQIBAwEDAQMBAwAAAAA="),

            //lead storage
            new Structure(0.02f, Items.lead, vaults),

            //salvo outpost
            new Structure(0.02f, Items.tungsten, "BAIABXNhbHZvAwANY2FyYmlkZS1kcmlsbAAAA2FpcgEADGNhcmJpZGUtd2FsbAcHAAAAAAEDAQMBAwEDAAABAwEDAQMCAAAAAQMAAAEAAgAAAAAAAAABAwEDAQAAAAAAAwACAAAAAQIBAAEBAgAAAAAAAAABAgAAAQEAAAAAAQEBAQEBAAABAQEBAQEBAQAAAAA="),

            //advanced laser outpost
            new Structure(0.03f, null, "BQIABmxhbmNlcgEAEmNhcmJpZGUtd2FsbC1sYXJnZQQAEXNvbGFyLXBhbmVsLWxhcmdlAAADYWlyAwALc29sYXItcGFuZWwLCwAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAEAAAAAAAAAAAABAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAMAAwAAAAAAAwABAAAAAAABAAAAAgAAAAAAAAAAAAMAAAAAAAAAAAAAAAAAAAAAAAQAAAACAAAAAQAAAAAAAQAAAAMAAAAAAAAAAAAAAAAAAAAAAAAAAAADAAIAAAADAAMAAQAAAAAAAAAAAAEAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"),

            //titanium storage
            new Structure(0.02f, Items.titanium, vaults),

            //coal laser outpost
            new Structure(0.03f, null, "BgEADHRob3JpdW0td2FsbAMABmxhbmNlcgUAFGNvbWJ1c3Rpb24tZ2VuZXJhdG9yBAANY2FyYmlkZS1kcmlsbAAAA2FpcgIAC3NvbGFyLXBhbmVsBwcAAAEAAQABAQEBAQEBAAAAAQACAgMAAAACAAEAAAABAAICAAAAAAIAAQAAAAEAAQAEAQUAAQABAAAAAQACAAMBAAMCAAEAAAABAAIAAAMAAwIAAQAAAAEAAQABAwEDAQABAA=="),

            //ultimate laser outpost
            new Structure(0.02f, null, "BgMABmxhbmNlcgIAEmNhcmJpZGUtd2FsbC1sYXJnZQUAEXNvbGFyLXBhbmVsLWxhcmdlAAADYWlyBAALc29sYXItcGFuZWwBAAxjYXJiaWRlLXdhbGwPDwAAAAAAAAAAAAABAwIDAAABAwAAAAAAAAAAAAAAAAAAAAACAwAAAgMAAAAAAAACAwAAAgMAAAAAAAAAAAAAAQMAAAAAAAAAAAMDAAAAAAAAAAAAAAIDAAAAAAAAAgMAAAMDAAAEAwAAAAADAwAAAwMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIDAAAAAAAAAgMAAAMDAAAAAAUDAAAAAAAAAAAEAwAAAAABAwEDAAAAAAAAAAAAAAAAAAAAAAUDAAADAwAAAgMAAAIDAAADAwAAAAAAAAAABAMAAAAAAAAAAwAAAAAAAAAAAAAAAAAAAAAFAwAAAAAAAAAAAwMAAAIDAAABAwEDAgMAAAQDAAAAAAAAAAAFAwAAAAAAAAAAAAAAAAAAAAAAAAMDAAADAwAAAAAAAAAAAwMAAAIDAAAAAAAAAgMAAAAAAAAAAAAAAwMAAAQDAAAAAAAAAAAAAAAAAAAAAAIDAAACAwAAAAAAAAIDAAACAwAAAQMAAAAAAAABAwAAAAAAAAAAAgMAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEDAAAAAAEDAAAAAAAAAAAAAA=="),

            //coal storage
            new Structure(0.02f, Items.coal, vaults),
        };
    }

    public void generate(Generation gen, Team team, int coreX, int coreY, int enemyX, int enemyY){
        init();

        this.enemyX = enemyX;
        this.enemyY = enemyY;
        this.coreX = coreX;
        this.coreY = coreY;
        this.gen = gen;
        this.team = team;

        genOutposts();
    }

    void genOutposts(){
        int padding = 10;
        int maxDifficulty = 10;
        float baseChance = 0.75f;
        Array<Structure> selected = new Array<>();
        Array<Rectangle> used = new Array<>();
        Rectangle rect = new Rectangle();

        int maxIndex = (int)(1 + ((float)gen.sector.difficulty / maxDifficulty * (structures.length-2)));

        for(int i = maxIndex/3; i < maxIndex; i++){
            selected.add(structures[i]);
        }

        for(Structure struct : selected){
            for(int x = padding; x < gen.width - padding; x++){
                loop:
                for(int y = padding; y < gen.height - padding; y++){
                    rect.set(x - struct.layout.length, y - struct.layout[0].length, struct.layout.length, struct.layout[0].length);
                    if(Vector2.dst(x, y, coreX, coreY) > minCoreDst && Vector2.dst(x, y, enemyX, enemyY) > 30 && world.tile(x, y).floor().liquidDrop == null &&
                    (struct.ore == null || gen.tiles[x][y].floor().dropsItem(struct.ore)) && gen.random.chance(struct.chance * baseChance)){
                        for(Rectangle other : used){
                            if(other.overlaps(rect)){
                                continue loop;
                            }
                        }
                        used.add(new Rectangle(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2));
                        int elevation = world.tile(x, y).getElevation();
                        for(int cx = 0; cx < struct.layout.length; cx++){
                            for(int cy = 0; cy < struct.layout[0].length; cy++){
                                int wx = x + cx - struct.layout.length/2;
                                int wy = y + cy - struct.layout[0].length/2;
                                StructBlock block = struct.layout[cx][cy];
                                Tile tile = world.tile(wx, wy);
                                if(block.block != Blocks.air && tile.block().alwaysReplace){
                                    tile.setElevation(elevation);
                                    tile.setRotation(block.rotation);
                                    tile.setBlock(block.block, team);

                                    if(block.block instanceof Turret){
                                        fillTurret(tile);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void fillTurret(Tile tile){
        Block block = tile.block();
        if(block instanceof PowerTurret){
            tile.entity.power.amount = block.powerCapacity;
        }else if(block instanceof ItemTurret){
            ItemTurret turret = (ItemTurret)block;
            AmmoType[] type = turret.getAmmoTypes();
            block.handleStack(type[0].item, block.acceptStack(type[0].item, 1000, tile, null), tile, null);
        }
    }

    static class Structure{
        public final StructBlock[][] layout;
        public final Item ore;
        public final float chance;

        public Structure(float chance, Item ore, String encoding){
            this.ore = ore;
            this.layout = StructureFormat.read(encoding);
            this.chance = chance;
        }
    }
}
