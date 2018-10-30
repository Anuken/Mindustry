package io.anuke.ucore.util;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Keys;

/**Note that these color codes will only work on linux or mac terminals.*/
public class ColorCodes{
	public static String FLUSH = "\033[H\033[2J";
	public static String RESET = "\u001B[0m";
	public static String BOLD = "\u001B[1m";
	public static String ITALIC = "\u001B[3m";
	public static String UNDERLINED = "\u001B[4m";
	public static String BLACK = "\u001B[30m";
	public static String RED = "\u001B[31m";
	public static String GREEN = "\u001B[32m";
	public static String YELLOW = "\u001B[33m";
	public static String BLUE = "\u001B[34m";
	public static String PURPLE = "\u001B[35m";
	public static String CYAN = "\u001B[36m";
	public static String LIGHT_RED = "\u001B[91m";
	public static String LIGHT_GREEN = "\u001B[92m";
	public static String LIGHT_YELLOW = "\u001B[93m";
	public static String LIGHT_BLUE = "\u001B[94m";
	public static String LIGHT_MAGENTA = "\u001B[95m";
	public static String LIGHT_CYAN = "\u001B[96m";
	public static String WHITE = "\u001B[37m";
	
	public static String BACK_DEFAULT = "\u001B[49m";
	public static String BACK_RED = "\u001B[41m";
	public static String BACK_GREEN = "\u001B[42m";
	public static String BACK_YELLOW = "\u001B[43m";
	public static String BACK_BLUE = "\u001B[44m";

	private static ObjectMap<String, String> codes = new ObjectMap<>();
	
	static{
		//disable color codes on windows
		
		if(OS.isWindows){
			FLUSH = RESET = BOLD = UNDERLINED = BLACK = RED = GREEN = YELLOW = BLUE = PURPLE = CYAN 
					= LIGHT_RED = LIGHT_GREEN = LIGHT_YELLOW = LIGHT_BLUE = LIGHT_MAGENTA = LIGHT_CYAN 
					= WHITE = BACK_DEFAULT = BACK_RED = BACK_YELLOW = BACK_BLUE = BACK_GREEN = ITALIC = "";
		}

		codes.put("ff", FLUSH);
		codes.put("fr", RESET);
		codes.put("fb", BOLD);
		codes.put("fi", ITALIC);
		codes.put("fu", UNDERLINED);
		codes.put("bk", BLACK);
		codes.put("r", RED);
		codes.put("g", GREEN);
		codes.put("y", YELLOW);
		codes.put("b", BLUE);
		codes.put("p", PURPLE);
		codes.put("c", CYAN);
		codes.put("lr", LIGHT_RED);
		codes.put("lg", LIGHT_GREEN);
		codes.put("ly", LIGHT_YELLOW);
		codes.put("lm", LIGHT_MAGENTA);
		codes.put("lb", LIGHT_BLUE);
		codes.put("lc", LIGHT_CYAN);
		codes.put("w", WHITE);

		codes.put("bd", BACK_DEFAULT);
		codes.put("br", BACK_RED);
		codes.put("bg", BACK_GREEN);
		codes.put("by", BACK_YELLOW);
		codes.put("bb", BACK_BLUE);
	}

	public static Iterable<String> getColorCodes(){
		return new Keys<>(codes);
	}

	public static String getColorText(String code){
		return codes.get(code);
	}
}
