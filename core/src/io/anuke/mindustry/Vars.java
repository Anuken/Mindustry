package io.anuke.mindustry;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;

import io.anuke.mindustry.entities.Player;
import io.anuke.ucore.scene.ui.layout.Unit;

/**ick, global state*/
public class Vars{
	public static final boolean android = (Gdx.app.getType() == ApplicationType.Android);
	
	public static final float placerange = 66;
	public static final float respawnduration = 60*4;
	public static final float wavespace = 20*60*(android ? 2 : 1);
	public static final float enemyspawnspace = 65;
	public static final float fontscale = Unit.dp.inPixels(1f)/2f;
	public static final int baseCameraScale = Math.round(Unit.dp.inPixels(4));
	public static final int zoomScale = Math.round(Unit.dp.inPixels(1));
	
	public static boolean debug = false;
	
	//turret and enemy shoot speed inverse multiplier
	public static final int multiplier = android ? 3 : 1;
	
	public static final int tilesize = 8;
	
	public static Control control;
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
