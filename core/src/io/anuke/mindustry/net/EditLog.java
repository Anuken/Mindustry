package io.anuke.mindustry.net;

import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Block;

public class EditLog {
	public Player player;
	public Block block;
	public int rotation;
	public EditAction action;
	
	EditLog(Player player, Block block, int rotation, EditAction action) {
		this.player = player;
		this.block = block;
		this.rotation = rotation;
		this.action = action;
	}
	
	public String info() {
		return String.format("Player: %s, Block: %s, Rotation: %s, Edit Action: %s", player.name, block.name(), rotation, action.toString());
	}
	
	public enum EditAction {
		PLACE, BREAK;
	}
}
