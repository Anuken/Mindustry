package power;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.anuke.arc.Core;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.power.PowerGraph;

public class PowerGraphTest extends PowerTestFixture{
	// Ensure we don't update when lastFrameId is equal to event.core.lastFrameId
	@Test
	public void testUpdateFrameEqualsLastFrame() {
		PowerGraph pg = new PowerGraph();
		((FakeGraphics)Core.graphics).setFrameId(-1);
		float result = pg.update();
		
		assertEquals(0.0f, result);
	}
	
	@Test
	public void testUpdateCheat() {
		// set 'cheat' to true
		Vars.state = new GameState();
		Vars.state.rules.enemyCheat = true;

		// Create cheat tile and ensure its not already set to 1f (we assert this is updated later)
		Tile testTile = createCheatTile(0, 0, 0);
		assertNotEquals(1f, testTile.entity.power.satisfaction);
		
		PowerGraph pg = new PowerGraph();
		pg.add(testTile);
		
		float result = pg.update();
		assertEquals(0.0f, result);
		assertEquals(1f, testTile.entity.power.satisfaction);
	}
	
	@Test
	public void testUpdateConsumersProducersBatteriesEmpty()
	{
		PowerGraph pg = new PowerGraph();
		
		float result = pg.update();
		assertEquals(0.0f, result);
	}
	
	// TODO: this test is invalid.
	/*
	@Test
	public void testUpdateConsumersProducersBatteries()
	{
		Tile consumerTile1 = createFakeTile(0, 0, createFakeBatteryPower(100.50f));
		Tile consumerTile2 = createFakeTile(1, 0, createFakeBatteryPower(100.50f));
		Tile producerTile = createFakeTile(0, 1, createFakePowerGenerator(201.00f));
		
		PowerGraph pg = new PowerGraph();
		pg.add(consumerTile1);
		pg.add(consumerTile2);
		pg.add(producerTile);
		
		float result = pg.update();
		assertEquals(0.0f, result);
	}
	*/
	
	// TODO: move to superclass
	private Tile createCheatTile(int x, int y, float capacity){
		Tile fakeTile = createFakeTile(x, y, createFakeBatteryPower(capacity));
		
		fakeTile.setTeam(Vars.waveTeam);
		
		return fakeTile;
	}
}
