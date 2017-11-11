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
	public static final float wavespace = 40*60*(android ? 1 : 1);
	//waves can last no longer than 6 minutes, otherwise the next one spawns
	public static final float maxwavespace = 60*60*6;
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
	//whether to debug openGL info
	public static boolean debugGL = false;
	//number of save slots-- increasing may lead to layout issues
	//TODO named save slots, possibly with a scroll dialog
	public static final int saveSlots = 4;
	
	//only if smoothCamera
	public static boolean snapCamera = true;
	
	//turret and enemy shoot speed inverse multiplier
	public static final float multiplier = android ? 3 : 2;
	
	public static final int tilesize = 8;
	
	public static Control control;
	public static Renderer renderer;
	public static UI ui;
	
	public static Player player;
	
	public static String[] aboutText = {
		"Created by [ROYAL]Anuken.[]",
		"Originally an entry in the [orange]GDL[] MM Jam.",
		"",
		"Credits:",
		"- SFX made with [YELLOW]bfxr.com[]",
		"- Music made by [GREEN]RoccoW[] / found on [lime]FreeMusicArchive.org[]",
	};
}
