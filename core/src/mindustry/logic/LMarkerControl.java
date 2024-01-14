package mindustry.logic;

public enum LMarkerControl{
    remove,
    visibility("true/false"),
    minimap("true/false"),
    autoscale("true/false"),
    pos("x", "y"),
    endPos("x", "y"),
    drawLayer("layer"),
    color("color"),
    radius("radius"),
    stroke("stroke"),
    rotation("rotation"),
    shape("sides", "fill", "outline"),
    flushText("fetch"),
    fontSize("size"),
    textHeight("height"),
    labelFlags("background", "outline"),
    texture("printFlush", "name"),
    textureSize("width", "height");

    public final String[] params;

    public static final LMarkerControl[] all = values();

    LMarkerControl(String... params){
        this.params = params;
    }
}
