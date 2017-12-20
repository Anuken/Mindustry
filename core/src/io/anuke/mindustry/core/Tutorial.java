package io.anuke.mindustry.core;

import static io.anuke.mindustry.Vars.*;

import com.badlogic.gdx.math.GridPoint2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
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
		return world.getMap().name.equals("tutorial") && !GameState.is(State.menu);
	}
	
	public void buildUI(table table){
		
		//TODO maybe align it to the bottom?
		table.atop();
		
		new table("pane"){{
			atop();
			margin(12);
			
			info = new label(()->stage.text).pad(10f).padBottom(5f).width(340f).colspan(2).get();
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
		
		prev.pad(Unit.dp.scl(16));
		next.pad(Unit.dp.scl(16));
		
		prev.setDisabled(()->!canMove(false) || !stage.canBack);
		next.setDisabled(()->!stage.canForward);
	}
	
	public void update(){
		stage.update(this);
		//info.setText(stage.text);
		
		if(stage.showBlock){
			Tile tile = world.tile(control.core.x + stage.blockPlaceX, control.core.y + stage.blockPlaceY);
			
			if(tile.block() == stage.targetBlock && (tile.getRotation() == stage.blockRotation || stage.blockRotation == -1)){
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
		world.tile(corex, corey - 2).setBlock(Blocks.air);
		world.tile(corex, corey - 3).setBlock(Blocks.air);
		world.tile(corex, corey - 3).setFloor(Blocks.stone);
		
		world.tile(corex + 1, corey - 7).setFloor(Blocks.iron);
		world.tile(corex - 1, corey - 7).setFloor(Blocks.coal);
		
		int r = 10;
		
		for(int x = -r; x <= r; x ++){
			for(int y = -r; y <= r; y ++){
				if(world.tile(corex + x, corey + y).block() == Blocks.rock){
					world.tile(corex + x, corey + y).setBlock(Blocks.air);
				}
			}
		}
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
				text = "To move, use the [orange][[WASD][] keys. Hold [orange]shift[] to boost. Hold [orange]CTRL[] while using the [orange]scrollwheel[] to zoom in or out.";
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
				blockPlaceY = -2;
				targetBlock = DistributionBlocks.conveyor;
				text = "Use the [orange][[scrollwheel][] to rotate the conveyor to face [orange]forwards[], then place it in the [yellow]marked location[]  using the [orange][[left mouse button][].";
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
				blockPlaceY = -2;
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
				blockPlaceY = -3;
				targetBlock = ProductionBlocks.stonedrill;
				text = "Now, select and place a [yellow]stone drill[] at the marked location.";
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttonproduction").fireClick();
			}
		},
		blockInfo{
			{
				canBack = true;
				text = "If you want to learn more about a block, you can tap the [orange]question mark[] in the top right to read its description.";
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
					world.tile(control.core.x + flip, control.core.y - 2).setBlock(DistributionBlocks.conveyor, 2 * flip);
					world.tile(control.core.x + flip*2, control.core.y - 2).setBlock(DistributionBlocks.conveyor, 2 * flip);
					world.tile(control.core.x + flip*2, control.core.y - 3).setBlock(DistributionBlocks.conveyor, 2 * flip);
					world.tile(control.core.x + flip*2, control.core.y - 3).setBlock(DistributionBlocks.conveyor, 1);
					world.tile(control.core.x + flip*2, control.core.y - 4).setFloor(Blocks.stone);
					world.tile(control.core.x + flip*2, control.core.y - 4).setBlock(ProductionBlocks.stonedrill);
					
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
				blockPlaceY = -2;
				text = !Vars.android ? 
					"You can delete blocks by clicking the  [orange]right mouse button[] on the block you want to delete. Try deleting this conveyor.":
					"You can delete blocks by [orange]selecting the crosshair[] in the [orange]break mode menu[] in the bottom left and tapping a block. Try deleting this conveyor.";
			}
		},
		/*
		deleteBlock2{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				targetBlock = Blocks.air;
				blockPlaceX = -2;
				blockPlaceY = -2;
				text = "Try deleting this other conveyor too.";
			}
		},*/
		placeTurret{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				targetBlock = WeaponBlocks.turret;
				blockPlaceX = 2;
				blockPlaceY = 2;
				text = "Now, select and place a [yellow]turret[] at the [yellow]marked location[].";
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttonweapon").fireClick();
			}
		},
		placedTurretAmmo{
			{
				canBack = false;
				text = "This turret will now accept [yellow]ammo[] from the conveyor. You can see how much ammo it has by " + 
				(Vars.android ? "tapping it" : "hovering over it") + " and checking the [green]green bar[].";
			}
			
			void onSwitch(){
				for(int i = 0; i < 4; i ++){
					world.tile(control.core.x + 2, control.core.y - 2 + i).setBlock(DistributionBlocks.conveyor, 1);
				}
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
				text = "In later waves, you might need to use [yellow]pumps[] to distribute liquids for generators or extractors.";
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
				text = "Pumps work similarly to drills, except that they produce liquids instead of items. Try placing a pump on the [yellow]designated oil[].";
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
				world.tile(blockPlaceX + control.core.x, blockPlaceY + control.core.y).setBlock(Blocks.air);
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
				world.tile(blockPlaceX + control.core.x, blockPlaceY + control.core.y).setBlock(Blocks.air);
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
				world.tile(blockPlaceX + control.core.x, blockPlaceY + control.core.y).setBlock(Blocks.air);
			}
		},
		generator{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				targetBlock = ProductionBlocks.combustiongenerator;
				blockPlaceX = 4;
				blockPlaceY = 0;
				text = "Now, place a [orange]combustion generator[] block at the end of the conduit.";
			}
			
			void onSwitch(){
				world.tile(blockPlaceX + control.core.x, blockPlaceY + control.core.y).setBlock(Blocks.air);
				Vars.ui.<ImageButton>find("sectionbuttonpower").fireClick();
				Vars.control.addItem(Item.steel, 60);
				Vars.control.addItem(Item.iron, 60);
			}
		},
		generatorExplain{
			{
				canBack = false;
				text = "This generator will now create [yellow]power[] from the oil.";
			}
		},
		lasers{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				text = "Power is distributed using [yellow]power lasers[]. Rotate and place one here.";
				blockPlaceX = 4;
				blockPlaceY = 4;
				blockRotation = 2;
				targetBlock = DistributionBlocks.powerlaser;
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttonpower").fireClick();
			}
		},
		laserExplain{
			{
				canBack = false;
				text = "The generator will now move power into the laser block. An [yellow]opaque[] beam means that it is currently transmitting power, "
						+ "and a [yellow]transparent[] beam means it is not.";
			}
		},
		laserMore{
			{
				canBack = false;
				text = "You can check how much power a block has by hovering over it and checking the [yellow]yellow bar[] at the top.";
			}
		},
		healingTurret{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				canBack = false;
				blockPlaceX = 1;
				blockPlaceY = 4;
				targetBlock = DefenseBlocks.repairturret;
				text = "This laser can be used to power a [lime]repair turret[]. Place one here.";
			}
			
			void onSwitch(){
				Vars.ui.<ImageButton>find("sectionbuttonpower").fireClick();
			}
		},
		healingTurretExplain{
			{
				canBack = false;
				text = "As long as it has power, this turret will [lime]repair nearby blocks.[] When playing, make sure you get one in your base as quickly as possible!";
			}
		},
		smeltery{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				canBack = false;
				blockPlaceX = 0;
				blockPlaceY = -6;
				targetBlock = ProductionBlocks.smelter;
				text = "Many blocks require [orange]steel[] to make, which requires a [orange]smelter[] to craft. Place one here.";
			}
			
			void onSwitch(){
				Vars.control.addItem(Item.stone, 40);
				Vars.control.addItem(Item.iron, 40);
				Vars.ui.<ImageButton>find("sectionbuttoncrafting").fireClick();
				
			}
		},
		smelterySetup{
			{
				canBack = false;
				text = "This smelter will now produce [orange]steel[] from the input coal and iron.";
			}
			
			void onSwitch(){
				for(int i = 0; i < 4; i ++){
					world.tile(control.core.x, control.core.y - 5 + i).setBlock(DistributionBlocks.conveyor, 1);
				}
				world.tile(control.core.x+1, control.core.y - 7).setBlock(ProductionBlocks.irondrill);
				world.tile(control.core.x-1, control.core.y - 7).setBlock(ProductionBlocks.coaldrill);
				
				world.tile(control.core.x+1, control.core.y - 6).setBlock(DistributionBlocks.conveyor, 2);
				world.tile(control.core.x-1, control.core.y - 6).setBlock(DistributionBlocks.conveyor, 0);
			}
		},
		end{
			{
				text = "And that concludes the tutorial!  Good luck!";
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
