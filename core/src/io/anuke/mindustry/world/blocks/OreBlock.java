package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;

public class OreBlock extends Floor {
    protected Floor base;

    public OreBlock(Item ore, Floor base){
        super("ore-" + ore.name + "-" + base.name);
        this.drops = new ItemStack(ore, 1);
        this.base = base;
        this.variants = 3;
        this.minimapColor = ore.color;
        this.blends = block -> false;
        this.edge = base.name;
    }

    @Override
    public void draw(Tile tile){

        Draw.rect(base.variants > 0 ? (base.name + MathUtils.random(1, base.variants))  : base.name, tile.worldx(), tile.worldy());

        int rand = variants > 0 ? MathUtils.random(1, variants) : 0;

        Draw.color(0f, 0f, 0f, 0.2f);
        Draw.rect(variants > 0 ? (drops.item.name + rand)  : name, tile.worldx(), tile.worldy() - 1);
        Draw.color();
        Draw.rect(variants > 0 ? (drops.item.name + rand)  : name, tile.worldx(), tile.worldy());
    }

    @Override
    public TextureRegion[] getIcon() {
        if(icon == null){
            icon = new TextureRegion[]{Draw.region(drops.item.name + "1")};
        }
        return icon;
    }

    @Override
    public void drawNonLayer(Tile tile){
        MathUtils.random.setSeed(tile.id());

        base.drawEdges(tile, true);
    }

    @Override
    protected void drawEdges(Tile tile, boolean sameLayer){
        base.drawEdges(tile, sameLayer);
    }

    @Override
    public boolean blendOverride(Block block) {
        return block == base;
    }
}
