package mindustry.logic;

public enum LMarkerControl{
    remove,
    setVisibility("true/false"),
    toggleVisibility,
    text("text"),
    flushText,
    x("x"),
    y("y"),
    pos("x", "y"),
    endX("x"),
    endY("y"),
    endPos("x", "y"),
    fontSize("size"),
    textHeight("height"),
    labelBackground("true/false"),
    labelOutline("true/false"),
    labelFlags("background", "outline"),
    radius("radius"),
    stroke("stroke"),
    rotation("rotation"),
    shapeSides("sides"),
    shapeFill("true/false"),
    shapeOutline("true/false"),
    setShape("sides", "fill", "outline"),
    color("color");

    public final String[] params;

    public static final LMarkerControl[] all = values();

    LMarkerControl(String... params){
        this.params = params;
    }
}
