package io.anuke.mindustry.net;

import io.anuke.mindustry.world.Block;

public class EditLog {
	public String playername;
	public Block block;
	public int rotation;
	public EditAction action;
	
	EditLog(String playername, Block block, int rotation, EditAction action) {
		this.playername = playername;
		this.block = block;
		this.rotation = rotation;
		this.action = action;
	}
	
	public String info() {
		return String.format("Player: %s, Block: %s, Rotation: %s, Edit Action: %s", playername, block.name(), rotation, action.toString());
	}
	
	public enum EditAction {
		PLACE, BREAK
    }
}
