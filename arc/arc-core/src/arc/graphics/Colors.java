package arc.graphics;

import arc.struct.*;

import java.util.*;

/**
 * A general purpose class containing named colors that can be changed at will. For example, the markup language defined by the
 * {@code BitmapFontCache} class uses this class to retrieve colors. Custom colors can be defined here.
 * @author davebaol
 */
public final class Colors{

    private static final OrderedMap<String, Color> map = new OrderedMap<>();

    static{
        reset();
    }

    private Colors(){

    }

    /** Returns the color map. */
    public static OrderedMap<String, Color> getColors(){
        return map;
    }

    /**
     * Convenience method to lookup a color by {@code name}. The invocation of this method is equivalent to the expression
     * {@code Colors.getColors().get(name)}
     * @param name the name of the color
     * @return the color to which the specified {@code name} is mapped, or {@code null} if there was no mapping for {@code name}
     * .
     */
    public static Color get(String name){
        return map.get(name);
    }

    /**
     * Convenience method to add a {@code color} with its {@code name}. The invocation of this method is equivalent to the
     * expression {@code Colors.getColors().put(name, color)}
     * @param name the name of the color
     * @param color the color
     * @return the previous {@code color} associated with {@code name}, or {@code null} if there was no mapping for {@code name}
     * .
     */
    public static Color put(String name, Color color){
        return map.put(name, color);
    }

    /** Resets the color map to the predefined colors. */
    public static void reset(){
        map.clear();
        map.put("CLEAR", Color.clear);
        map.put("BLACK", Color.black);

        map.put("WHITE", Color.white);
        map.put("LIGHT_GRAY", Color.lightGray);
        map.put("GRAY", Color.gray);
        map.put("DARK_GRAY", Color.darkGray);
        map.put("LIGHT_GREY", Color.lightGray);
        map.put("GREY", Color.gray);
        map.put("DARK_GREY", Color.darkGray);

        map.put("BLUE", Color.royal); //overridden for better visuals
        map.put("NAVY", Color.navy);
        map.put("ROYAL", Color.royal);
        map.put("SLATE", Color.slate);
        map.put("SKY", Color.sky);
        map.put("CYAN", Color.cyan);
        map.put("TEAL", Color.teal);

        map.put("GREEN", Color.valueOf("38d667")); //overridden for better visuals
        map.put("ACID", Color.acid);
        map.put("LIME", Color.lime);
        map.put("FOREST", Color.forest);
        map.put("OLIVE", Color.olive);

        map.put("YELLOW", Color.yellow);
        map.put("GOLD", Color.gold);
        map.put("GOLDENROD", Color.goldenrod);
        map.put("ORANGE", Color.orange);

        map.put("BROWN", Color.brown);
        map.put("TAN", Color.tan);
        map.put("BRICK", Color.brick);

        map.put("RED", Color.valueOf("e55454")); //overridden for better visuals
        map.put("SCARLET", Color.scarlet);
        map.put("CRIMSON", Color.crimson);
        map.put("CORAL", Color.coral);
        map.put("SALMON", Color.salmon);
        map.put("PINK", Color.pink);
        map.put("MAGENTA", Color.magenta);

        map.put("PURPLE", Color.purple);
        map.put("VIOLET", Color.violet);
        map.put("MAROON", Color.maroon);

        //lowercase versions

        map.copy().each((key, val) -> map.put(key.toLowerCase(Locale.ROOT).replace("_", ""), val));
    }

}
