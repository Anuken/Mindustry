package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.type.ContentList;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.distribution.*;

public class DistributionBlocks extends BlockList implements ContentList{
	public static Block conveyor, titaniumconveyor, router, multiplexer, junction,
			bridgeconveyor, laserconveyor, sorter, splitter, overflowgate, massdriver;

	@Override
	public void load() {

		conveyor = new Conveyor("conveyor") {{
			health = 45;
			speed = 0.03f;
		}};

		titaniumconveyor = new Conveyor("titanium-conveyor") {{
			health = 65;
			speed = 0.07f;
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

		massdriver = new MassDriver("mass-driver"){{
			size = 3;
			itemCapacity = 80;
			range = 300f;
		}};
	}
}
