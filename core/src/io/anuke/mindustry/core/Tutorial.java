package io.anuke.mindustry.core;

import com.badlogic.gdx.math.GridPoint2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.builders.button;
import io.anuke.ucore.scene.builders.label;
import io.anuke.ucore.scene.builders.table;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

public class Tutorial{
	private Stage stage;
	private Label info;
	private TextButton next, prev;
	
	public Tutorial(){
		reset();
	}
	
	public boolean active(){
		return World.getMap() == Map.tutorial && !GameState.is(State.menu);
	}
	
	public void buildUI(table table){
		
		//TODO maybe align it to the bottom?
		table.atop();
		
		new table("pane"){{
			atop();
			get().pad(Unit.dp.inPixels(12));
			
			info = new label(()->stage.text).pad(10f).padBottom(5f).width(340f).units(Unit.dp).colspan(2).get();
			info.setWrap(true);
			
			row();
			
			prev = new button("< Prev", ()->{
				if(!prev.isDisabled())
					move(false);
			}).left().get();
			
			next = new button("Next >", ()->{
				if(!next.isDisabled())
					move(true);
			}).right().get();
			
			
		}}.end();
		
		prev.pad(Unit.dp.inPixels(16));
		next.pad(Unit.dp.inPixels(16));
		
		prev.setDisabled(()->!canMove(false) || !stage.canBack);
		next.setDisabled(()->!stage.canForward);
	}
	
	public void update(){
		stage.update(this);
		//info.setText(stage.text);
		
		if(stage.showBlock){
			Tile tile = World.tile(World.core.x + stage.blockPlaceX, World.core.y + stage.blockPlaceY);
			
			if(tile.block() == stage.targetBlock && (tile.rotation == stage.blockRotation || stage.blockRotation == -1)){
				move(true);
			}
		}
	}
	
	public void reset(){
		stage = Stage.values()[0];
		stage.onSwitch();
	}
	
	public void complete(){
		//new TextDialog("Congratulations!", "You have completed the tutorial!").padText(Unit.dp.inPixels(10f)).show();
		GameState.set(State.menu);
		reset();
	}
	
	void move(boolean forward){
		
		if(forward && !canMove(forward)){
			complete();
		}else{
			int current = stage.ordinal();
			
			while(true){
				current += Mathf.sign(forward);
				
				if(current < 0 || current >= Stage.values().length){
					break;
				}else if(Vars.android == Stage.values()[current].androidOnly || Vars.android != Stage.values()[current].desktopOnly){
					stage = Stage.values()[current];
					stage.onSwitch();
					break;
				}
			}
		}
	}
	
	boolean canMove(boolean forward){
		int current = stage.ordinal();
		
		while(true){
			current += Mathf.sign(forward);
			
			if(current < 0 || current >= Stage.values().length){
				return false;
			}else if(Vars.android == Stage.values()[current].androidOnly || Vars.android != Stage.values()[current].desktopOnly){
				return true;
			}
		}
		
	}
	
	public boolean showTarget(){
		return stage == Stage.shoot;
	}
	
	public boolean canPlace(){
		return stage.canPlace;
	}
	
	public boolean showBlock(){
		return stage.showBlock;
	}
	
	public Block getPlaceBlock(){
		return stage.targetBlock;
	}
	
	public GridPoint2 getPlacePoint(){
		return Tmp.g1.set(stage.blockPlaceX, stage.blockPlaceY);
	}
	
	public int getPlaceRotation(){
		return stage.blockRotation;
	}
	
	public void setDefaultBlocks(int corex, int corey){
		World.tile(corex, corey - 1).setBlock(Blocks.air);
		World.tile(corex, corey - 2).setBlock(Blocks.air);
		World.tile(corex, corey - 2).setFloor(Blocks.stone);
	}
	
	enum Stage{
		intro{
			{
				text = "[yellow]Welcome to the tutorial.[] To begin, press 'next'.";
			}
		},
		moveDesktop{
			{
				desktopOnly = true;
				text = "To move, use the [orange][[WASD][] keys. Use the [orange]scrollwheel[] to zoom in or out.";
			}
		},
		shoot{
			{
				desktopOnly = true;
				text = "Use your mouse to aim,  hold [orange]left mouse button[] to shoot. Try practicing on the [yellow]target[].";
			}
		},
		moveAndroid{
			{
				androidOnly = true;
				text = "To pan the view, drag one finger across the screen. Pinch and drag to zoom in or out.";
			}
		},
		placeSelect{
			{
				canBack = false;
				canPlace = true;
				text = "Try selecting a [yellow]conveyor[] from the block menu in the bottom right.";
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttondistribution").fireClick();
			}
		},
		placeConveyorDesktop{
			{
				desktopOnly = true;
				canPlace = true;
				showBlock = true;
				canForward = false;
				blockRotation = 1;
				blockPlaceX = 0;
				blockPlaceY = -1;
				targetBlock = DistributionBlocks.conveyor;
				text = "Use [orange][[R][] to rotate the conveyor to face [orange]forwards[], then place it in the [yellow]marked location[]  using the [orange][[left mouse button][].";
			}
		},
		placeConveyorAndroid{
			{
				androidOnly = true;
				canPlace = true;
				showBlock = true;
				canForward = false;
				blockRotation = 1;
				blockPlaceX = 0;
				blockPlaceY = -1;
				targetBlock = DistributionBlocks.conveyor;
				text = "Use the [orange][[rotate button][] to rotate the conveyor to face [orange]forwards[], drag it into position with one finger, then place it in the [yellow]marked location[] using the [orange][[checkmark][].";
			}
		},
		placeConveyorAndroidInfo{
			{
				androidOnly = true;
				canBack = false;
				text = "Alternatively, you can press the crosshair icon in the bottom left to switch to [orange][[touch mode][], and "
						+ "place blocks by tapping on the screen. In touch mode, blocks can be rotated with the arrow at the bottom left. "
						+ "Press [yellow]next[] to try it out.";
			}
			
			void onSwitch(){
				//Vars.player.recipe = null;
			}
		},
		placeDrill{
			{
				canPlace = true;
				canBack = false;
				showBlock = true;
				canForward = false;
				blockPlaceX = 0;
				blockPlaceY = -2;
				targetBlock = ProductionBlocks.stonedrill;
				text = "Now, select and place a [yellow]stone drill[] at the marked location.";
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttonproduction").fireClick();
			}
		},
		deselectDesktop{
			{
				desktopOnly = true;
				canBack = false;
				text = "You can de-select a block using the [orange][[right mouse button][].";
			}
		},
		deselectAndroid{
			{
				androidOnly = true;
				canBack = false;
				text = "You can deselect a block by pressing the [orange]X[] button.";
			}
		},
		drillPlaced{
			{
				canBack = false;
				text = "The drill will now produce [yellow]stone,[] output it onto the conveyor, then move it into the [yellow]core[].";
			}
			
			void onSwitch(){
				Vars.player.recipe = null;
			}
		},
		drillInfo{
			{
				text = "Different ores need different drills. Stone requires stone drills, iron requires iron drills, etc.";
			}
		},
		drillPlaced2{
			{
				text = "Moving items into the core puts them in your [yellow]item inventory[], in the top left. Placing blocks uses items from your inventory.";
			}
		},
		moreDrills{
			{
				canBack = false;
				text = "You can link many drills and conveyors up together, like so.";
			}
			
			void onSwitch(){
				for(int flip : new int[]{1, -1}){
					World.tile(World.core.x + flip, World.core.y - 1).setBlock(DistributionBlocks.conveyor, 2 * flip);
					World.tile(World.core.x + flip*2, World.core.y - 1).setBlock(DistributionBlocks.conveyor, 2 * flip);
					World.tile(World.core.x + flip*2, World.core.y - 2).setBlock(DistributionBlocks.conveyor, 1);
					World.tile(World.core.x + flip*2, World.core.y - 3).setFloor(Blocks.stone);
					World.tile(World.core.x + flip*2, World.core.y - 3).setBlock(ProductionBlocks.stonedrill);
					
				}
			}
		},
		deleteBlock{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				targetBlock = Blocks.air;
				blockPlaceX = 2;
				blockPlaceY = -1;
				text = !Vars.android ? 
					"You can delete blocks by holding the  [orange]right mouse button[] on the block you want to delete. Try deleting this conveyor.":
					"You can delete blocks by [orange]tapping and holding[]  on the block you want to delete. Try deleting this conveyor.";
			}
		},
		deleteBlock2{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				targetBlock = Blocks.air;
				blockPlaceX = 1;
				blockPlaceY = -1;
				text = "Try deleting this other conveyor too.";
			}
		},
		placeTurret{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				targetBlock = WeaponBlocks.turret;
				blockPlaceX = 2;
				blockPlaceY = -1;
				text = "Now, select and place a [yellow]turret[] at the [yellow]marked location[].";
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttondefense").fireClick();
			}
		},
		placedTurretAmmo{
			{
				canBack = false;
				text = "This turret will now accept [yellow]ammo[] from the conveyor. You can see how much ammo it has by " + 
				(Vars.android ? "tapping it" : "hovering over it") + " and checking the [green]green bar[].";
			}
			
			void onSwitch(){
				Vars.player.recipe = null;
			}
		},
		turretExplanation{
			{
				canBack = false;
				text = "Turrets will automatically shoot at the nearest enemy in range, as long as they have enough ammo.";
			}
		},
		waves{
			{
				text = "Every [yellow]" + (int)(Vars.wavespace/60) + "[] seconds, a wave of [coral]enemies[] will spawn in specific locations and attempt to destroy the core.";
			}
		},
		coreDestruction{
			{
				text = "Your objective is to [yellow]defend the core[]. If the core is destroyed, you [coral]lose the game[].";
			}
		},
		pausingDesktop{
			{
				desktopOnly = true;
				text = "If you ever need to take a break, press the [orange]pause button[] in the top left or [orange]space[] "
						+ "to pause the game. You can still select and place blocks while paused, but cannot move or shoot.";
			}
		},
		pausingAndroid{
			{
				androidOnly = true;
				text = "If you ever need to take a break, press the [orange]pause button[] in the top left"
						+ " to pause the game. You can still place select and place blocks while paused.";
			}
		},
		purchaseWeapons{
			{
				desktopOnly = true;
				canBack = false;
				text = "You can purchase new [yellow]weapons[] for your mech by opening the upgrade menu in the bottom left.";
			}
			
			void onSwitch(){
				Vars.control.addItem(Item.steel, 60);
				Vars.control.addItem(Item.iron, 60);
			}
		},
		switchWeapons{
			{
				canBack = false;
				desktopOnly = true;
				text = "Switch weapons by either clicking its icon in the bottom left, or using numbers [orange][[1-9][].";
			}
			
			void onSwitch(){
				if(!Vars.control.getWeapons().contains(Weapon.multigun, true)){
					Vars.control.getWeapons().add(Weapon.multigun);
					Vars.ui.updateWeapons();
				}
			}
		},
		spawnWave{
			float warmup = 0f;
			{
				canBack = false;
				canForward = false;
				text = "Here comes a wave now. Destroy them.";
			}
			
			void update(Tutorial t){
				warmup += Timers.delta();
				if(Vars.control.getEnemiesRemaining() == 0 && warmup > 60f){
					t.move(true);
				}
			}
			
			void onSwitch(){
				warmup = 0f;
				Vars.control.runWave();
			}
		},
		pumpDesc{
			{
				canBack = false;
				text = "In later waves, you might need to use [yellow]pumps[] to distribute liquids for extractors.";
			}
		}, 
		pumpPlace{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				targetBlock = ProductionBlocks.pump;
				blockPlaceX = 6;
				blockPlaceY = -2;
				text = "Pumps work similarly to drills, except that they produce liquids instead of items. Try placing a pump on the [yellow]designated water[].";
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttonproduction").fireClick();
				Vars.control.addItem(Item.steel, 60);
				Vars.control.addItem(Item.iron, 60);
			}
		},
		conduitUse{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				targetBlock = DistributionBlocks.conduit;
				blockPlaceX = 5;
				blockPlaceY = -2;
				blockRotation = 2;
				text = "Now place a [orange]conduit[] leading away from the pump.";
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttondistribution").fireClick();
				World.tile(blockPlaceX + World.core.x, blockPlaceY + World.core.y).setBlock(Blocks.air);
			}
		},
		conduitUse2{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				targetBlock = DistributionBlocks.conduit;
				blockPlaceX = 4;
				blockPlaceY = -2;
				blockRotation = 1;
				text = "And a few more...";
			}
			
			void onSwitch(){
				World.tile(blockPlaceX + World.core.x, blockPlaceY + World.core.y).setBlock(Blocks.air);
			}
		},
		conduitUse3{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				targetBlock = DistributionBlocks.conduit;
				blockPlaceX = 4;
				blockPlaceY = -1;
				blockRotation = 1;
				text = "And a few more...";
			}
			
			void onSwitch(){
				World.tile(blockPlaceX + World.core.x, blockPlaceY + World.core.y).setBlock(Blocks.air);
			}
		},
		extractor{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				targetBlock = ProductionBlocks.coalpurifier;
				blockPlaceX = 4;
				blockPlaceY = 0;
				text = "Now, place a [orange]coal extractor[] block at the end of the conduit.";
			}
			
			void onSwitch(){
				World.tile(blockPlaceX + World.core.x, blockPlaceY + World.core.y).setBlock(Blocks.air);
				Vars.ui.<ImageButton>find("sectionbuttonproduction").fireClick();
				Vars.control.addItem(Item.steel, 60);
				Vars.control.addItem(Item.iron, 60);
			}
		},
		extractorExplain{
			{
				canBack = false;
				text = "The extractor will now produce [orange]coal[] from the stone and water, then move it to the core.";
			}
			
			void onSwitch(){
				for(int i = -2; i <= 2; i ++){
					World.tile(World.core.x + i + 4, World.core.y + 2).setBlock(ProductionBlocks.stonedrill);
					World.tile(World.core.x + i + 4, World.core.y + 2).setFloor(Blocks.stone);
				}
				
				for(int i = 0; i < 3; i ++){
					World.tile(World.core.x + 4 - 1 - i, World.core.y).setBlock(DistributionBlocks.conveyor, 2);
				}
				
				World.tile(World.core.x + 2, World.core.y + 1).setBlock(DistributionBlocks.conveyor, 0);
				World.tile(World.core.x + 3, World.core.y + 1).setBlock(DistributionBlocks.conveyor, 0);
				World.tile(World.core.x + 4, World.core.y + 1).setBlock(DistributionBlocks.conveyor, 3);
				World.tile(World.core.x + 5, World.core.y + 1).setBlock(DistributionBlocks.conveyor, 2);
				World.tile(World.core.x + 6, World.core.y + 1).setBlock(DistributionBlocks.conveyor, 2);
				
			}
		},
		extractorMore{
			{
				canBack = false;
				canPlace = true;
				text = "The [orange]smeltery[] and [orange]crucible[] blocks work similarly to extractors, except they accept only items.";
			}
		},
		end{
			{
				text = "And that concludes the tutorial!";
				canBack = false;
			}
		};
		String text = "no text";
		
		boolean androidOnly;
		boolean desktopOnly;
		
		boolean canBack = true;
		boolean canForward = true;
		boolean canPlace = false;
		boolean showBlock = false;
		
		int blockPlaceX = 0;
		int blockPlaceY = 0;
		int blockRotation = -1;
		Block targetBlock = null;
		
		void update(Tutorial t){};
		void onSwitch(){}
	}
}
