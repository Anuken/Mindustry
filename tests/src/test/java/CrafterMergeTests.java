import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.production.GenericCrafter.*;
import mindustry.world.meta.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class CrafterMergeTests{
    static final int cx = 10, cy = 10;
    static final float eps = 1e-4f;

    @BeforeAll
    public static void launchApplication(){
        ApplicationTests.launchApplication();
    }

    @BeforeEach
    void loadWorld() throws Throwable{
        logic.reset();
        state.set(State.menu);
        GameState.loadMap(ApplicationTests.testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;
    }

    static void fill(Block floor){
        for(int dx = -3; dx <= 3; dx++){
            for(int dy = -3; dy <= 3; dy++){
                state.world.tile(cx + dx, cy + dy).setFloor(floor.asFloor());
            }
        }
    }

    static Tile place(Block block){
        Tile tile = state.world.tile(cx, cy);
        tile.setBlock(block, Team.sharded);
        tile.build.updateProximity();
        return tile;
    }

    static GenericCrafterBuild crafter(Tile tile){
        return (GenericCrafterBuild)tile.build;
    }

    static float expectedEfficiency(GenericCrafter block, GenericCrafterBuild build, float solidFraction){
        return Math.max(block.baseEfficiency * solidFraction + Math.min(block.maxBoost, block.boostScale * build.attrsum) + block.attribute.env(), 0f);
    }

    static int writtenSize(Building build){
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        build.writeAll(new Writes(new DataOutputStream(out)));
        return out.size();
    }

    @Test
    void plainCrafterIgnoresAttributes(){
        fill(Blocks.sand);
        GenericCrafter kiln = (GenericCrafter)Blocks.kiln;
        Tile tile = place(kiln);
        GenericCrafterBuild build = crafter(tile);

        assertNull(kiln.attribute);
        assertTrue(kiln.canPlaceOn(tile, Team.sharded, 0));
        assertEquals(0f, build.attrsum, eps);
        assertEquals(1f, build.attributeScale(), eps);
        assertEquals(5f, build.scaleOutput(5f), eps);

        build.efficiency = 1f;
        assertEquals(1f / kiln.craftTime * build.delta(), build.getProgressIncrease(kiln.craftTime), eps);
    }

    @Test
    void cultivatorReadsSporeFloor(){
        fill(Blocks.sporeMoss);
        GenericCrafter cultivator = (GenericCrafter)Blocks.cultivator;
        GenericCrafterBuild build = crafter(place(cultivator));

        float perTile = Blocks.sporeMoss.asFloor().attributes.get(Attribute.spores);
        assertTrue(perTile > 0f);
        assertEquals(perTile * 4f, build.attrsum, eps);
        assertEquals(expectedEfficiency(cultivator, build, 1f), build.efficiencyMultiplier(), eps);
        assertTrue(build.efficiencyMultiplier() > cultivator.baseEfficiency);

        build.efficiency = 1f;
        assertEquals(1f / cultivator.craftTime * build.delta() * build.efficiencyMultiplier(), build.getProgressIncrease(cultivator.craftTime), eps);
    }

    @Test
    void cultivatorBoostIsCapped(){
        fill(Blocks.sporeMoss);
        GenericCrafter cultivator = (GenericCrafter)Blocks.cultivator;
        GenericCrafterBuild build = crafter(place(cultivator));

        build.attrsum = 1000f;
        assertEquals(cultivator.baseEfficiency + cultivator.maxBoost + Attribute.spores.env(), build.efficiencyMultiplier(), eps);
    }

    @Test
    void siliconCrucibleScalesOutput(){
        fill(Blocks.hotrock);
        GenericCrafter crucible = (GenericCrafter)Blocks.siliconCrucible;
        Tile tile = place(crucible);
        GenericCrafterBuild build = crafter(tile);

        assertEquals(Attribute.heat, crucible.attribute);
        assertTrue(crucible.canPlaceOn(tile, Team.sharded, 0));
        assertEquals(Blocks.hotrock.asFloor().attributes.get(Attribute.heat) * 9f, build.attrsum, eps);

        float expected = 8f * (crucible.baseEfficiency + Math.min(crucible.maxBoost, crucible.outputScale * build.attrsum) + Attribute.heat.env());
        assertEquals(expected, build.scaleOutput(8f), eps);
        assertTrue(build.scaleOutput(8f) > 8f);
    }

    @Test
    void ventCondenserNeedsFullVents(){
        GenericCrafter condenser = (GenericCrafter)Blocks.ventCondenser;
        Tile tile = state.world.tile(cx, cy);

        fill(Blocks.stone);
        assertFalse(condenser.canPlaceOn(tile, Team.sharded, 0));

        fill(Blocks.rhyoliteVent);
        assertTrue(condenser.canPlaceOn(tile, Team.sharded, 0));

        state.world.tile(cx, cy).setFloor(Blocks.stone.asFloor());
        assertFalse(condenser.canPlaceOn(tile, Team.sharded, 0));
    }

    @Test
    void ventCondenserOutputsWater(){
        fill(Blocks.rhyoliteVent);
        GenericCrafter condenser = (GenericCrafter)Blocks.ventCondenser;
        GenericCrafterBuild build = crafter(place(condenser));

        assertEquals(9f, build.attrsum, eps);
        float multiplier = build.efficiencyMultiplier();
        assertTrue(multiplier > 0f);

        build.efficiency = 1f;
        build.updateTile();

        float expectedWater = condenser.outputLiquid.amount * multiplier * build.delta();
        assertEquals(expectedWater, build.liquids.get(Liquids.water), eps);
        assertEquals(multiplier / condenser.craftTime * build.delta(), build.progress, eps);
    }

    @Test
    void waterExtractorPlacement(){
        GenericCrafter extractor = (GenericCrafter)Blocks.waterExtractor;
        Tile tile = state.world.tile(cx, cy);

        fill(Blocks.stone);
        assertTrue(extractor.canPlaceOn(tile, Team.sharded, 0));

        fill(Blocks.deepwater);
        assertFalse(extractor.canPlaceOn(tile, Team.sharded, 0));

        fill(Blocks.rhyolite);
        assertFalse(extractor.canPlaceOn(tile, Team.sharded, 0));

        fill(Blocks.mud);
        assertTrue(extractor.canPlaceOn(tile, Team.sharded, 0));
    }

    @Test
    void waterExtractorSolidFraction(){
        fill(Blocks.stone);
        state.world.tile(cx, cy).setFloor(Blocks.water.asFloor());
        state.world.tile(cx + 1, cy).setFloor(Blocks.water.asFloor());

        GenericCrafter extractor = (GenericCrafter)Blocks.waterExtractor;
        GenericCrafterBuild build = crafter(place(extractor));

        assertEquals(0.5f, build.solidFraction, eps);
        assertEquals(0.5f, build.efficiencyMultiplier(), eps);
    }

    @Test
    void waterExtractorOutput(){
        fill(Blocks.mud);
        GenericCrafter extractor = (GenericCrafter)Blocks.waterExtractor;
        GenericCrafterBuild build = crafter(place(extractor));

        assertEquals(4f, build.attrsum, eps);
        assertEquals(2f, build.efficiencyMultiplier(), eps);

        build.efficiency = 1f;
        build.updateTile();

        float expected = 0.11f * 2f * build.delta();
        assertEquals(expected, build.liquids.get(Liquids.water), eps);
        assertEquals(expected / build.delta(), build.lastOutput, eps);
        assertEquals(build.delta() / extractor.craftTime, build.progress, eps);
    }

    @Test
    void waterExtractorNeverRunsBackwards(){
        fill(Blocks.mud);
        GenericCrafterBuild build = crafter(place(Blocks.waterExtractor));

        build.attrsum = -100f;
        assertEquals(0f, build.efficiencyMultiplier(), eps);

        build.efficiency = 1f;
        build.updateTile();
        assertEquals(0f, build.liquids.get(Liquids.water), eps);
    }

    @Test
    void oilExtractorPlacement(){
        GenericCrafter extractor = (GenericCrafter)Blocks.oilExtractor;
        Tile tile = state.world.tile(cx, cy);

        fill(Blocks.stone);
        assertFalse(extractor.canPlaceOn(tile, Team.sharded, 0));

        fill(Blocks.sand);
        assertTrue(extractor.canPlaceOn(tile, Team.sharded, 0));

        fill(Blocks.deepwater);
        assertFalse(extractor.canPlaceOn(tile, Team.sharded, 0));
    }

    @Test
    void oilExtractorOutputAndFlatSandUse(){
        fill(Blocks.darksand);
        GenericCrafter extractor = (GenericCrafter)Blocks.oilExtractor;
        GenericCrafterBuild build = crafter(place(extractor));

        assertEquals(1.5f, build.efficiencyMultiplier(), eps);
        build.items.add(Items.sand, 5);

        build.efficiency = 1f;
        build.updateTile();
        assertEquals(0.25f * 1.5f * build.delta(), build.liquids.get(Liquids.oil), eps);
        assertEquals(build.delta() / extractor.craftTime, build.progress, eps);

        int ticks = 1;
        while(ticks < 200 && build.items.get(Items.sand) == 5){
            build.efficiency = 1f;
            build.updateTile();
            ticks++;
        }

        assertEquals(4, build.items.get(Items.sand));
        assertTrue(ticks >= 58 && ticks <= 62, "Sand should be used once per craftTime regardless of attribute boost, took " + ticks + " ticks");
    }

    @Test
    void extractorsAreNotFactories(){
        assertFalse(Blocks.waterExtractor.flags.contains(BlockFlag.factory));
        assertFalse(Blocks.oilExtractor.flags.contains(BlockFlag.factory));
        assertTrue(Blocks.kiln.flags.contains(BlockFlag.factory));
    }

    @Test
    void extractorSaveFormatHasNoProgress(){
        fill(Blocks.mud);
        GenericCrafter extractor = (GenericCrafter)Blocks.waterExtractor;
        GenericCrafterBuild build = crafter(place(extractor));

        int without = writtenSize(build);
        extractor.saveProgress = true;
        int with;
        try{
            with = writtenSize(build);
        }finally{
            extractor.saveProgress = false;
        }

        assertEquals(without + 8, with);
    }

    @Test
    void extractorLoadsOldSaveData(){
        fill(Blocks.mud);
        GenericCrafterBuild build = crafter(place(Blocks.waterExtractor));
        build.liquids.add(Liquids.water, 12f);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        build.writeAll(new Writes(new DataOutputStream(out)));

        build.liquids.clear();
        build.progress = 0.7f;
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        build.readAll(new Reads(new DataInputStream(in)), build.version());

        assertEquals(0, in.available());
        assertEquals(12f, build.liquids.get(Liquids.water), eps);
        assertEquals(0.7f, build.progress, eps);
    }

    @Test
    void attributeCraftersKeepSavingProgress(){
        for(Block block : new Block[]{Blocks.kiln, Blocks.cultivator, Blocks.siliconCrucible, Blocks.ventCondenser}){
            fill(Blocks.stone);
            GenericCrafterBuild build = crafter(place(block));
            build.progress = 0.5f;
            build.warmup = 0.25f;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            build.writeAll(new Writes(new DataOutputStream(out)));

            build.progress = 0f;
            build.warmup = 0f;
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            build.readAll(new Reads(new DataInputStream(in)), build.version());

            assertEquals(0, in.available(), block.name);
            assertEquals(0.5f, build.progress, eps, block.name);
            assertEquals(0.25f, build.warmup, eps, block.name);
        }
    }

    @Test
    void pickedUpResetsAttributeState(){
        fill(Blocks.sporeMoss);
        GenericCrafterBuild build = crafter(place(Blocks.cultivator));
        build.warmup = 1f;
        assertTrue(build.attrsum > 0f);

        build.pickedUp();

        assertEquals(0f, build.attrsum, eps);
        assertEquals(0f, build.warmup, eps);
    }
}
