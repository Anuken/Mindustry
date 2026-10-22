import arc.struct.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.*;
import org.junit.jupiter.api.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/** Put every block and every unit in front of the camera and see if anything crashes. */
public class ClientDrawTests extends ClientTestBase{
    /** Empty space left around the area that content is placed in, in tiles. */
    static final int margin = 3;

    record Placement(Block block, int x, int y){}

    /** Zooms out and returns how many tiles fit on screen, minus a safety margin for the HUD. */
    static int[] visibleTiles(){
        //any playing world will do for this; the zoom survives world changes
        playCustomWorld(8, 8, w -> {});
        zoomOut();
        float[] size = ClientHarness.call(() -> new float[]{arc.Core.camera.width, arc.Core.camera.height});
        int w = (int)(size[0] / tilesize) - 2 * margin, h = (int)(size[1] / tilesize) - 2 * margin;
        assertTrue(w >= 20 && h >= 12, "the window is too small for this test (" + w + "x" + h + " tiles visible)");
        return new int[]{w, h};
    }

    @Test
    void drawEveryBlock(){
        int[] box = visibleTiles();
        int boxW = box[0], boxH = box[1];

        Seq<Block> blocks = ClientHarness.call(() -> content.blocks().select(Block::canBeBuilt));
        blocks.sort(b -> -b.size);

        //shelf-pack the blocks into as many screens as needed
        Seq<Seq<Placement>> batches = new Seq<>();
        Seq<Placement> batch = new Seq<>();
        int x = 0, y = 0, rowHeight = 0;
        for(Block block : blocks){
            if(x + block.size > boxW){
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
            if(y + block.size > boxH){
                batches.add(batch);
                batch = new Seq<>();
                x = y = rowHeight = 0;
            }
            batch.add(new Placement(block, x, y));
            x += block.size;
            rowHeight = Math.max(rowHeight, block.size);
        }
        if(batch.any()) batches.add(batch);

        for(Seq<Placement> current : batches){
            playCustomWorld(boxW + 2 * margin, boxH + 2 * margin, world -> {
                for(Placement p : current){
                    int offset = Math.max(p.block.size % 2 == 0 ? p.block.size / 2 - 1 : p.block.size / 2, 0);
                    //cores make the player spawn, which moves the camera. Derelict cores don't.
                    Team team = p.block instanceof CoreBlock ? Team.derelict : Team.sharded;
                    world.tile(margin + p.x + offset, margin + p.y + offset).setBlock(p.block, team, 0);
                }
            });

            ClientHarness.frames(90);

            assertVisible(margin * tilesize, margin * tilesize, "the first block");
            assertVisible((margin + boxW) * tilesize, (margin + boxH) * tilesize, "the last block");

            ClientHarness.run(() -> {
                for(Placement p : current){
                    int offset = Math.max(p.block.size % 2 == 0 ? p.block.size / 2 - 1 : p.block.size / 2, 0);
                    Tile tile = state.world.tile(margin + p.x + offset, margin + p.y + offset);
                    assertSame(p.block, tile.block(), "block was replaced or removed while running: " + p.block);
                    assertNotNull(tile.build, "block lost its building: " + p.block);
                }
            });
        }
    }

    @Test
    void drawEveryUnit(){
        int[] box = visibleTiles();
        int boxW = box[0], boxH = box[1];

        Seq<UnitType> types = ClientHarness.call(() -> content.units().select(u -> !u.internal));
        Seq<StatusEffect> statuses = ClientHarness.call(() -> content.statusEffects().copy());

        //large units overlap their neighbours, that's fine: they still draw
        int spacing = 6, perRow = Math.max(1, boxW / spacing), perBatch = perRow * Math.max(1, boxH / spacing);

        for(int start = 0; start < types.size; start += perBatch){
            int from = start, to = Math.min(types.size, start + perBatch);

            playCustomWorld(boxW + 2 * margin, boxH + 2 * margin, world -> {});

            ClientHarness.run(() -> {
                for(int i = from; i < to; i++){
                    int slot = i - from;
                    float ux = (margin + (slot % perRow) * spacing + spacing / 2f) * tilesize;
                    float uy = (margin + (slot / perRow) * spacing + spacing / 2f) * tilesize;

                    //left half vs right half of the screen: they shoot each other, so weapons, bullets and effects are drawn
                    Team team = (slot % perRow) < perRow / 2 ? Team.sharded : Team.crux;
                    var unit = types.get(i).spawn(team, ux, uy);
                    if(statuses.any()) unit.apply(statuses.get(i % statuses.size), 60f * 20f);
                }
            });

            ClientHarness.frames(120);

            assertVisible(margin * tilesize, margin * tilesize, "the first unit");
            ClientHarness.run(() -> assertFalse(state.entities.unit.isEmpty(), "every unit died or none spawned"));
        }
    }
}
