package mindustry.ui.builder;

/** Enum used for encoding properties as single bytes instead of passing strings around. */
public enum UiKey{
    // node types
    table, pane, stack, label, image, button, imageButton, field, check, slider, space, defaults, buttonTable, row, //NOTE: all node types must be before 'row'

    // node-specific properties
    text, wrap, region, icon, placeholder, scaling, background, margin, id, hint, maxLength, checked, min, max, step,
    defaultValue, clicked, enter, style, group, condition, color, disabled,

    // cell properties
    grow, growX, growY, fill, fillX, fillY, expand, expandX, expandY,
    width, height, size, minWidth, maxWidth, minHeight, maxHeight,
    pad, padTop, padLeft, padBottom, padRight,
    align, labelAlign, colspan, uniform, uniformX, uniformY;

    public static final UiKey[] all = values();
}