package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;

public class OreBlock extends Floor{
    public Floor base;

    public OreBlock(Item ore, Floor base){
        super("ore-" + ore.name + "-" + base.name);
        this.formalName = ore.localizedName() + " " + base.formalName;
        this.drops = new ItemStack(ore, 1);
        this.base = base;
        this.variants = 3;
        this.minimapColor = ore.color;
        this.blends = block -> (block instanceof OreBlock && ((OreBlock) block).base != base) || (!(block instanceof OreBlock) && block != base);
        this.tileBlends = (tile, other) -> tile.getElevation() < other.getElevation();
        this.edge = base.name;
    }

    @Override
    public String getDisplayName(Tile tile){
        return drops.item.localizedName();
    }

    @Override
    public TextureRegion getEditorIcon(){
        if(editorIcon == null){
            editorIcon = variantRegions[0];
        }
        return editorIcon;
    }

    @Override
    public void draw(Tile tile){
        Draw.rect(variantRegions[Mathf.randomSeed(tile.id(), 0, Math.max(0, variantRegions.length - 1))], tile.worldx(), tile.worldy());

        drawEdges(tile, false);
    }

    @Override
    public void drawNonLayer(Tile tile){
        MathUtils.random.setSeed(tile.id());

        base.drawEdges(tile, true);
    }

    @Override
    public boolean blendOverride(Block block){
        return block == base;
    }
}
