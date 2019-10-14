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
}
