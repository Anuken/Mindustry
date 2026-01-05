package mindustry.logic;

public enum LMarkerControl{
    remove,
    world("true/false"),
    minimap("true/false"),
    autoscale("true/false"),
    pos("x", "y"),
    endPos("x", "y"),
    drawLayer("layer"),
    color("color"),
    radius("radius"),
    stroke("stroke"),
    outline("outline"),
    rotation("rotation"),
    shape("sides", "fill", "outline"),
    arc("start", "end"),
    flushText("fetch"),
    fontSize("size"),
    textHeight("height"),
    textAlign("align"),
    lineAlign("align"),
    labelFlags("background", "outline"),
    texture("printFlush", "name"),
    textureSize("width", "height"),
    posi("index", "x", "y"),
    uvi("index", "x", "y"),
    colori("index", "color");

    public final String[] params;

    public static final LMarkerControl[] all = values();

    LMarkerControl(String... params){
        this.params = params;
    }
}
