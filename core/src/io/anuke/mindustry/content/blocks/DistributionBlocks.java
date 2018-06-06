package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;

public class DistributionBlocks extends BlockList implements ContentList{
	public static Block conveyor, steelconveyor, pulseconveyor, router, multiplexer, junction, bridgeconveyor, laserconveyor, sorter, splitter, overflowgate;

	@Override
	public void load() {

		conveyor = new Conveyor("conveyor") {{
			health = 40;
			speed = 0.02f;
		}};

		steelconveyor = new Conveyor("steelconveyor") {{
			health = 55;
			speed = 0.04f;
		}};

		pulseconveyor = new Conveyor("poweredconveyor") {{
			health = 75;
			speed = 0.09f;
		}};

		router = new Router("router");

		multiplexer = new Router("multiplexer") {{
			size = 2;
			itemCapacity = 80;
		}};

		junction = new Junction("junction") {{
			speed = 26;
			capacity = 32;
		}};

		bridgeconveyor = new BufferedItemBridge("bridgeconveyor") {{
			range = 3;
			hasPower = false;
		}};

		laserconveyor = new ItemBridge("laserconveyor") {{
			range = 7;
		}};

		sorter = new Sorter("sorter");

		splitter = new Splitter("splitter");

		overflowgate = new OverflowGate("overflowgate");
	}
}
