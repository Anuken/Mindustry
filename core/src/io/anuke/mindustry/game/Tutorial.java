package io.anuke.mindustry.game;

import com.badlogic.gdx.math.GridPoint2;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.resource.Item;
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
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Tmp;

import static io.anuke.mindustry.Vars.*;

public class Tutorial{
	private Stage stage;
	private Label info;
	private TextButton next, prev;
	
	public Tutorial(){
		reset();
	}
	
	public boolean active(){
		return world.getMap() != null && world.getMap().name.equals("tutorial") && !state.is(State.menu);
	}
	
	public void buildUI(table table){

		table.atop();
		
		new table("pane"){{
			atop();
			margin(12);
			
			info = new label(()->stage.text).pad(10f).padBottom(5f).width(340f).colspan(2).get();
			info.setWrap(true);
			
			row();
			
			prev = new button("$text.tutorial.back", ()->{
				if(!prev.isDisabled())
					move(false);
			}).left().get();
			
			next = new button("$text.tutorial.next", ()->{
				if(!next.isDisabled())
					move(true);
			}).right().get();
			
			
		}}.end();
		
		prev.margin(16);
		next.margin(16);
		
		prev.setDisabled(()->!canMove(false) || !stage.canBack);
		next.setDisabled(()->!stage.canForward);
	}
	
	public void update(){
		stage.update(this);
		//info.setText(stage.text);
		
		if(stage.showBlock){
			Tile tile = world.tile(world.getCore().x + stage.blockPlaceX, world.getCore().y + stage.blockPlaceY);
			
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
		state.set(State.menu);
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
				}else if(mobile == Stage.values()[current].androidOnly || mobile != Stage.values()[current].desktopOnly){
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
			}else if(mobile == Stage.values()[current].androidOnly || mobile != Stage.values()[current].desktopOnly){
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
		
		world.tile(corex + 1, corey - 8).setFloor(Blocks.iron);
		world.tile(corex - 1, corey - 8).setFloor(Blocks.coal);
		
		int r = 10;
		
		for(int x = -r; x <= r; x ++){
			for(int y = -r; y <= r; y ++){
				if(world.tile(corex + x, corey + y).block() == Blocks.rock){
					world.tile(corex + x, corey + y).setBlock(Blocks.air);
				}
			}
		}
	}
	
	public enum Stage{
		intro{
			{
			}
		},
		moveDesktop{
			{
				desktopOnly = true;
			}
		},
		shoot{
			{
				desktopOnly = true;
			}
		},
		moveAndroid{
			{
				androidOnly = true;
			}
		},
		placeSelect{
			{
				canBack = false;
				canPlace = true;
			}
			
			void onSwitch(){
				ui.<ImageButton>find("sectionbuttondistribution").fireClick();
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
			}
		},
		placeConveyorAndroidInfo{
			{
				androidOnly = true;
				canBack = false;
			}
			
			void onSwitch(){
				//player.recipe = null;
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
			}
			
			void onSwitch(){
				ui.<ImageButton>find("sectionbuttonproduction").fireClick();
			}
		},
		blockInfo{
			{
				canBack = true;
			}
		},
		deselectDesktop{
			{
				desktopOnly = true;
				canBack = false;
			}
		},
		deselectAndroid{
			{
				androidOnly = true;
				canBack = false;
			}
		},
		drillPlaced{
			{
				canBack = false;
			}
			
			void onSwitch(){
				control.input().recipe = null;
			}
		},
		drillInfo{
			{
			}
		},
		drillPlaced2{
			{
			}
		},
		moreDrills{
			{
				canBack = false;
			}
			
			void onSwitch(){
				for(int flip : new int[]{1, -1}){
					world.tile(world.getCore().x + flip, world.getCore().y - 2).setBlock(DistributionBlocks.conveyor, 2 * flip);
					world.tile(world.getCore().x + flip*2, world.getCore().y - 2).setBlock(DistributionBlocks.conveyor, 2 * flip);
					world.tile(world.getCore().x + flip*2, world.getCore().y - 3).setBlock(DistributionBlocks.conveyor, 2 * flip);
					world.tile(world.getCore().x + flip*2, world.getCore().y - 3).setBlock(DistributionBlocks.conveyor, 1);
					world.tile(world.getCore().x + flip*2, world.getCore().y - 4).setFloor(Blocks.stone);
					world.tile(world.getCore().x + flip*2, world.getCore().y - 4).setBlock(ProductionBlocks.stonedrill);
					
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
				desktopOnly = true;
			}
		},
		deleteBlockAndroid{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				targetBlock = Blocks.air;
				blockPlaceX = 2;
				blockPlaceY = -2;
				androidOnly = true;
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
				blockPlaceY = 2;
			}
			
			void onSwitch(){
				ui.<ImageButton>find("sectionbuttonweapon").fireClick();
			}
		},
		placedTurretAmmo{
			{
				canBack = false;
			}
			
			void onSwitch(){
				for(int i = 0; i < 4; i ++){
					world.tile(world.getCore().x + 2, world.getCore().y - 2 + i).setBlock(DistributionBlocks.conveyor, 1);
				}

				control.input().recipe = null;
			}
		},
		turretExplanation{
			{
				canBack = false;
			}
		},
		waves{
			{
			}
		},
		coreDestruction{
			{
			}
		},
		pausingDesktop{
			{
				desktopOnly = true;
			}
		},
		pausingAndroid{
			{
				androidOnly = true;
			}
		},
		//TODO re-add tutorial on weapons

		spawnWave{
			float warmup = 0f;
			{
				canBack = false;
				canForward = false;
			}
			
			void update(Tutorial t){
				warmup += Timers.delta();
				if(state.enemies == 0 && warmup > 60f){
					t.move(true);
				}
			}
			
			void onSwitch(){
				warmup = 0f;
				logic.runWave();
			}
		},
		pumpDesc{
			{
				canBack = false;
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
			}
			
			void onSwitch(){
				ui.<ImageButton>find("sectionbuttonproduction").fireClick();
				state.inventory.addItem(Item.steel, 60);
				state.inventory.addItem(Item.iron, 60);
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
			}
			
			void onSwitch(){
				ui.<ImageButton>find("sectionbuttondistribution").fireClick();
				world.tile(blockPlaceX + world.getCore().x, blockPlaceY + world.getCore().y).setBlock(Blocks.air);
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
			}
			
			void onSwitch(){
				world.tile(blockPlaceX + world.getCore().x, blockPlaceY + world.getCore().y).setBlock(Blocks.air);
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
			}
			
			void onSwitch(){
				world.tile(blockPlaceX + world.getCore().x, blockPlaceY + world.getCore().y).setBlock(Blocks.air);
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
			}
			
			void onSwitch(){
				world.tile(blockPlaceX + world.getCore().x, blockPlaceY + world.getCore().y).setBlock(Blocks.air);
				ui.<ImageButton>find("sectionbuttonpower").fireClick();
				state.inventory.addItem(Item.steel, 60);
				state.inventory.addItem(Item.iron, 60);
			}
		},
		generatorExplain{
			{
				canBack = false;
			}
		},
		lasers{
			{
				canBack = false;
				canForward = false;
				showBlock = true;
				canPlace = true;
				blockPlaceX = 4;
				blockPlaceY = 4;
				blockRotation = 2;
				targetBlock = DistributionBlocks.powerlaser;
			}
			
			void onSwitch(){
				ui.<ImageButton>find("sectionbuttonpower").fireClick();
			}
		},
		laserExplain{
			{
				canBack = false;
			}
		},
		laserMore{
			{
				canBack = false;
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
			}
			
			void onSwitch(){
				ui.<ImageButton>find("sectionbuttonpower").fireClick();
			}
		},
		healingTurretExplain{
			{
				canBack = false;
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
				blockPlaceY = -7;
				targetBlock = ProductionBlocks.smelter;
			}
			
			void onSwitch(){
				state.inventory.addItem(Item.stone, 40);
				state.inventory.addItem(Item.iron, 40);
				ui.<ImageButton>find("sectionbuttoncrafting").fireClick();
				
			}
		},
		smelterySetup{
			{
				canBack = false;
			}
			
			void onSwitch(){
				for(int i = 0; i < 5; i ++){
					world.tile(world.getCore().x, world.getCore().y - 6 + i).setBlock(DistributionBlocks.conveyor, 1);
				}

				world.tile(world.getCore().x, world.getCore().y - 6 + 1).setBlock(DistributionBlocks.tunnel, 3);
				world.tile(world.getCore().x, world.getCore().y - 6 + 2).setBlock(DefenseBlocks.stonewall, 0);
				world.tile(world.getCore().x, world.getCore().y - 6 + 3).setBlock(DistributionBlocks.tunnel, 1);

				world.tile(world.getCore().x+1, world.getCore().y - 8).setBlock(ProductionBlocks.irondrill);
				world.tile(world.getCore().x-1, world.getCore().y - 8).setBlock(ProductionBlocks.coaldrill);
				
				world.tile(world.getCore().x+1, world.getCore().y - 7).setBlock(DistributionBlocks.conveyor, 2);
				world.tile(world.getCore().x-1, world.getCore().y - 7).setBlock(DistributionBlocks.conveyor, 0);
			}
		},
		tunnelExplain{
			{
				canBack = false;
			}
		},
		end{
			{
				canBack = false;
			}
		};
		public final String text = Bundles.getNotNull("tutorial."+name()+".text");
		
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
