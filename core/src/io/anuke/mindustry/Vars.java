package io.anuke.mindustry;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.I18NBundle;

import io.anuke.mindustry.core.*;
import io.anuke.mindustry.entities.Player;
import io.anuke.ucore.scene.ui.layout.Unit;

public class Vars{
	public static final boolean testAndroid = false;
	//shorthand for whether or not this is running on android
	public static final boolean android = (Gdx.app.getType() == ApplicationType.Android) || testAndroid;
	//shorthand for whether or not this is running on GWT
	public static final boolean gwt = (Gdx.app.getType() == ApplicationType.WebGL);
	//how far away from the player blocks can be placed
	public static final float placerange = 66;
	//respawn time in frames
	public static final float respawnduration = 60*4;
	//time between waves in frames (on normal mode)
	public static final float wavespace = 60*60*(android ? 1 : 1);
	//waves can last no longer than 5 minutes, otherwise the next one spawns
	public static final float maxwavespace = 60*60*5;
	//advance time the pathfinding starts at
	public static final float aheadPathfinding = 60*20;
	//how far away from spawn points the player can't place blocks
	public static final float enemyspawnspace = 65;
	//scale of the font
	public static float fontscale = Unit.dp.inPixels(1f)/2f;
	//camera zoom displayed on startup
	public static final int baseCameraScale = Math.round(Unit.dp.inPixels(4));
	//how much the zoom changes every zoom button press
	public static final int zoomScale = Math.round(Unit.dp.inPixels(1));
	//if true, player speed will be increased, massive amounts of resources will be given on start, and other debug options will be available
	public static boolean debug = false;
	//whether the player can clip through walls
	public static boolean noclip = false;
	//whether to draw chunk borders
	public static boolean debugChunks = false;
	//whether turrets have infinite ammo (only with debug)
	public static boolean infiniteAmmo = true;
	//whether to show paths of enemies
	public static boolean showPaths = false;
	//if false, player is always hidden
	public static boolean showPlayer = true;
	//whether to hide ui, only on debug
	public static boolean showUI = true;
	//number of save slots-- increasing may lead to layout issues
	//TODO named save slots
	public static final int saveSlots = 8;
	//amount of drops that are left when breaking a block
	public static final float breakDropAmount = 0.5f;
	
	//only if smoothCamera
	public static boolean snapCamera = true;
	
	//turret and enemy shoot speed inverse multiplier
	public static final float multiplier = android ? 3 : 2;
	
	public static final int tilesize = 8;
	
	public static I18NBundle bundle;
	
	public static Control control;
	public static Renderer renderer;
	public static UI ui;
	public static World world;
	
	public static Player player;
	
	public static String[] aboutText = {
		"Created by [ROYAL]Anuken.[]",
		"Originally an entry in the [orange]GDL[] MM Jam.",
		"",
		"Credits:",
		"- SFX made with [YELLOW]bfxr[]",
		"- Music made by [GREEN]RoccoW[] / found on [lime]FreeMusicArchive.org[]",
		"",
		"Special thanks to:",
		"- [coral]MitchellFJN[]: extensive playtesting and feedback",
		"- [sky]Luxray5474[]: wiki work, code contributions",
		"- All the beta testers on itch.io and Google Play"
	};
}
