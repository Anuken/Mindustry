package mindustry.world.blocks.environment;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;

import static mindustry.Vars.*;

public class ColoredFloor extends Floor{
    /** If the alpha value of the color is set to this value, different colors are ignored and no border is drawn. */
    public static final int flagIgnoreDifferentColor = 1;
    /** If the alpha value of the color is set to this value, colors are interpolated across corners. This is essentially linear filtering for the whole "image". */
    public static final int flagSmoothBlend = 2;

    //4x (pos2 color uv2)
    private static final float[] verts = new float[4 * 5];

    public Color defaultColor = Color.white;
    protected int defaultColorRgba;

    public ColoredFloor(String name){
        super(name);
        saveData = true;
        editorConfigurable = true;
        saveConfig = true;
    }

    @Override
    public void init(){
        super.init();
        lastConfig = defaultColorRgba = defaultColor.rgba();
    }

    @Override
    public void buildEditorConfig(Table table){
        showColorEdit(table, this);
    }

    public static void showColorEdit(Table t, Block block){
        t.button(b -> {
            b.margin(4f);
            b.left();
            b.table(Tex.pane, in -> {
                in.image(Tex.whiteui).update(i -> {
                    if(block.lastConfig instanceof Integer col){
                        i.color.set(col | 0xff);
                    }
                }).grow();
            }).margin(4).size(50f).padRight(10);
            b.add("@color");
        }, Styles.cleart, () ->
            ui.picker.show(
                block.lastConfig instanceof Integer col ? new Color(col | 0xff) : new Color(Color.white), false,
                col -> block.lastConfig = col.rgba8888())).left().width(250f).pad(3f).row();
    }

    @Override
    public Object getConfig(Tile tile){
        return tile.extraData;
    }

    @Override
    public void drawBase(Tile tile){
        //make sure to mask out the alpha channel - it's generally undesirable, and leads to invisible blocks when the data is not initialized
        Draw.color(tile.extraData | 0xff);
        if((tile.extraData & 0xff) == flagSmoothBlend && autotile){
            //Only autotiling is supported right now for the sake of simplicity
            int bits = 0;

            for(int i = 0; i < 8; i++){
                Tile other = tile.nearby(Geometry.d8[i]);
                //force flagIgnoreDifferentColor by bypassing checkAutotileSame
                if(other != null && other.floor().blendGroup == blendGroup){
                    bits |= (1 << i);
                }
            }
            var region = autotileRegions[TileBitmask.values[bits]];
            float s = Vars.tilesize/2f;
            float x = tile.worldx(), y = tile.worldy();

            verts[0] = x - s;
            verts[1] = y - s;
            verts[2] = sample(this, tile.x, tile.y, tile.x - 1, tile.y - 1, tile.x, tile.y - 1, tile.x - 1, tile.y);
            verts[3] = region.u;
            verts[4] = region.v2;

            verts[5] = x + s;
            verts[6] = y - s;
            verts[7] = sample(this, tile.x, tile.y, tile.x, tile.y - 1, tile.x + 1, tile.y - 1, tile.x + 1, tile.y);
            verts[8] = region.u2;
            verts[9] = region.v2;

            verts[10] = x + s;
            verts[11] = y + s;
            verts[12] = sample(this, tile.x, tile.y, tile.x + 1, tile.y + 1, tile.x, tile.y + 1, tile.x + 1, tile.y);
            verts[13] = region.u2;
            verts[14] = region.v;

            verts[15] = x - s;
            verts[16] = y + s;
            verts[17] = sample(this, tile.x, tile.y, tile.x - 1, tile.y + 1, tile.x, tile.y + 1, tile.x - 1, tile.y);
            verts[18] = region.u;
            verts[19] = region.v;

            Draw.vert(region.texture, verts, 0, verts.length);
        }else{
            super.drawBase(tile);
        }
        Draw.color();
    }

    static float sample(Block target, int tx1, int ty1, int tx2, int ty2, int tx3, int ty3, int tx4, int ty4){
        int total = 0;
        float r = 0f, g = 0f, b = 0f;
        Tile t1 = Vars.world.tile(tx1, ty1);
        Tile t2 = Vars.world.tile(tx2, ty2);
        Tile t3 = Vars.world.tile(tx3, ty3);
        Tile t4 = Vars.world.tile(tx4, ty4);

        //manually unrolled loops, hooray
        if(t1 != null && t1.floor() == target){
            total ++;
            r += Color.ri(t1.extraData);
            g += Color.gi(t1.extraData);
            b += Color.bi(t1.extraData);
        }

        if(t2 != null && t2.floor() == target){
            total ++;
            r += Color.ri(t2.extraData);
            g += Color.gi(t2.extraData);
            b += Color.bi(t2.extraData);
        }

        if(t3 != null && t3.floor() == target){
            total ++;
            r += Color.ri(t3.extraData);
            g += Color.gi(t3.extraData);
            b += Color.bi(t3.extraData);
        }

        if(t4 != null && t4.floor() == target){
            total ++;
            r += Color.ri(t4.extraData);
            g += Color.gi(t4.extraData);
            b += Color.bi(t4.extraData);
        }

        return Color.toFloatBits((int)(r/total), (int)(g/total), (int)(b/total), 255);
    }

    @Override
    public void drawOverlay(Tile tile){
        //make sure color doesn't carry over
        Draw.color();
        super.drawOverlay(tile);
    }

    @Override
    public void floorChanged(Tile tile){
        //reset to white
        if(tile.extraData == 0){
            tile.extraData = defaultColorRgba;
        }
    }

    @Override
    public void placeEnded(Tile tile, @Nullable Unit builder, int rotation, @Nullable Object config){
        //config is assumed to be an integer RGBA color
        if(config instanceof Integer i){
            tile.extraData = i;
        }
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        if(plan.config instanceof Integer i){
            Draw.tint(Tmp.c1.set(i | 0xff));
        }
        drawDefaultPlanRegion(plan, list);
    }

    @Override
    public boolean checkAutotileSame(Tile tile, @Nullable Tile other){
        return other != null && other.floor().blendGroup == blendGroup && ((tile.extraData & 0xff) == flagIgnoreDifferentColor || tile.extraData == other.extraData);
    }

    @Override
    public int minimapColor(Tile tile){
        return tile.extraData | 0xff;
    }
}
