package mindustry.logic;

public enum LMarkerControl{
    remove,
    visibility("true/false"),
    toggleVisibility,
    x("x"),
    y("y"),
    pos("x", "y"),
    endX("x"),
    endY("y"),
    endPos("x", "y"),
    drawLayer("layer"),
    color("color"),
    radius("radius"),
    stroke("stroke"),
    rotation("rotation"),
    shapeSides("sides"),
    shapeFill("true/false"),
    shapeOutline("true/false"),
    shape("sides", "fill", "outline"),
    text("text"),
    flushText,
    fontSize("size"),
    textHeight("height"),
    labelBackground("true/false"),
    labelOutline("true/false"),
    labelFlags("background", "outline"),
    texture("name", "-", "-"),
    textureWidth("width"),
    textureHeight("height");

    public final String[] params;

    public static final LMarkerControl[] all = values();

    LMarkerControl(String... params){
        this.params = params;
    }
}
