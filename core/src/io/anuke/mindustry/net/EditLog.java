package io.anuke.mindustry.net;

import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.world.Block;
import static io.anuke.mindustry.Vars.playerGroup;

public class EditLog {
	
	public Player player;
	public Block block;
	public int rotation;
	public EditAction action;
	
	EditLog(Player player, Block block, int rotation, EditAction action){
		this.player = player;
		this.block = block;
		this.rotation = rotation;
		this.action = action;
	}
	
	public String info() {
		return String.format("Player: %s, Block: %s, Rotation: %s, Edit Action: %s", player.name, block.name(), rotation, action.toString());
	}
	
	public static EditLog valueOf(String string) {
		String[] parts = string.split(":");
		return new EditLog(playerGroup.getByID(Integer.valueOf(parts[0])),
						   Block.getByID(Integer.valueOf(parts[1])),
						   Integer.valueOf(parts[2]),
						   EditAction.valueOf(parts[3]));
	}
	
	public String toString() {
		return String.format("%s:%s:%s:%s", player.id, block.id, rotation, action.toString());
	}
	
	public enum EditAction{
		PLACE,
		BREAK;
	}
}
