package mindustry.logic;

public enum LMarkerControl{
    create("type", "x", "y"),
    remove,
    hide,
    show,
    toggleVisibility,
    setVisibility("true/false"),
    setText("text"),
    flushText,
    setX("x"),
    setY("y"),
    setPos("x", "y"),
    setFontSize("size"),
    setTextHeight("height"),
    setLabelBackground("true/false"),
    setLabelOutline("true/false"),
    setLabelFlags("background", "outline"),
    setRadius("radius"),
    setStroke("stroke"),
    setRotation("rotation"),
    setShapeSides("sides"),
    setShapeFill("true/false"),
    setShapeOutline("true/false"),
    setShape("sides", "fill", "outline"),
    setColor("color");

    public final String[] params;

    public static final LMarkerControl[] all = values();

    LMarkerControl(String... params){
        this.params = params;
    }
}
