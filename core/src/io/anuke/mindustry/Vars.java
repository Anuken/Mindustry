package io.anuke.mindustry;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;

import io.anuke.mindustry.entities.Player;
import io.anuke.ucore.scene.ui.layout.Unit;

public class Vars{
	//shorthand for whether or not this is running on android
	public static final boolean android = (Gdx.app.getType() == ApplicationType.Android);
	//how far away from the player blocks can be placed
	public static final float placerange = 66;
	//respawn time in frames
	public static final float respawnduration = 60*4;
	//time between waves in frames
	public static final float wavespace = 35*60*(android ? 2 : 1);
	//how far away from spawn points the player can't place blocks
	public static final float enemyspawnspace = 65;
	//scale of the font
	public static final float fontscale = Unit.dp.inPixels(1f)/2f;
	//camera zoom displayed on startup
	public static final int baseCameraScale = Math.round(Unit.dp.inPixels(4));
	//how much the zoom changes every zoom button press
	public static final int zoomScale = Math.round(Unit.dp.inPixels(1));
	//if true, player speed will be increased, massive amounts of resources will be given on start, and other debug options will be available
	public static boolean debug = false;
	//number of save slots-- increasing may lead to layout issues
	public static final int saveSlots = 4;
	
	//turret and enemy shoot speed inverse multiplier
	public static final float multiplier = android ? 3 : 2;
	
	public static final int tilesize = 8;
	
	public static Control control;
	public static Renderer renderer;
	public static UI ui;
	
	public static Player player;
	
	public static final String[] maps = {"delta", "canyon", "pit", "maze"};
	
	public static String[] aboutText = {
		"Made by [ROYAL]Anuken[] for the" + "\nGDL Metal Monstrosity jam.",
		"",
		"Sources used:",
		"- [YELLOW]bfxr.com[] for sound effects",
		"- [RED]freemusicarchive.org[] for music",
		"- Music made by [GREEN]RoccoW[]",
	};
	
	public static String[] tutorialText = {
		"[GREEN]Default Controls:",
		"[WHITE][YELLOW][[WASD][] to move, [YELLOW][[R][] to rotate blocks.",
		"Hold [YELLOW][[R-MOUSE][] to destroy blocks, click [YELLOW][[L-MOUSE][] to place them.",
		"[YELLOW][[L-MOUSE][] to shoot.",
		"[yellow][[scrollwheel] to switch weapons.",
		"",
		"[GOLD]Every "+wavespace/60+" seconds, a new wave will appear.",
		"Build turrets to defend the core.",
		"If the core is destroyed, you lose the game.",
		"",
		"[LIME]To collect building resources, move them into the core with conveyors.",
		"[LIME]Place [ORANGE]drills[] on the right material,they will automatically mine material",
		"and dump it to nearby conveyors or turrets.",
		"",
		"[SCARLET]To produce steel, feed coal and iron into a smelter."
	};
	
	public static String[] androidTutorialText = {
		"[GREEN]Default Controls:",
		"[WHITE]Use [YELLOW]one finger[] to pan the camera, or two while placing blocks.",
		"[YELLOW]Hold and tap[] to destroy blocks.",
		"",
		"[GOLD]Every "+wavespace/60+" seconds, a new wave will appear.",
		"Build turrets to defend the core.",
		"If the core is destroyed, you lose the game.",
		"",
		"[LIME]To collect building resources, move them into the core with conveyors.",
		"[LIME]Place [ORANGE]drills[] on the right material,they will automatically mine material",
		"and dump it to nearby conveyors or turrets.",
		"",
		"[SCARLET]To produce steel, feed coal and iron into a smelter."
	};
}
