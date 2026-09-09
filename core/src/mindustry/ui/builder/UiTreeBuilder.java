package mindustry.ui.builder;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.input.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.CheckBox.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.ScrollPane.*;
import arc.scene.ui.Slider.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.mod.*;
import mindustry.ui.*;
import mindustry.ui.builder.UiBuilder.*;

/** Builds a UI into an existing Table from a NodeBuilder tree. */
public class UiTreeBuilder{

    public static BuildResult build(Table root, NodeBuilder<?> builder){
        return build(root, builder, null);
    }

    public static BuildResult build(Table root, NodeBuilder<?> builder, @Nullable Cons<MenuResult> resultListener){
        BuildContext ctx = new BuildContext(resultListener);
        buildInto(root, builder.entries, ctx);
        return new BuildResult(ctx.idElements, ctx.images);
    }

    private static void buildInto(Table table, Seq<Entry> entries, BuildContext ctx){
        // defaults{} only affects cells added later within this same block, not nested table/pane blocks
        Seq<Entry> defaults = new Seq<>();

        for(Entry entry : entries){
            if(entry.key == UiKey.row){
                table.row();

            }else if(entry.value instanceof NodeBuilder<?> node){
                if(entry.key == UiKey.defaults){
                    for(Entry e : node.entries) defaults.add(e);
                    continue;
                }

                String cond = node.str(UiKey.condition);
                if(cond != null && !evalCondition(cond)) continue; // condition false - node is not added at all

                Cell<?> cell = entry.key == UiKey.space ? table.add() : table.add(addNode(entry.key, node, ctx));
                if(cell == null) continue; // unknown/unsupported node - skip for forward compat

                for(Entry d : defaults) applyCellProp(cell, d.key, d.value);
                for(Entry e : node.entries){
                    if(!(e.value instanceof NodeBuilder)) applyCellProp(cell, e.key, e.value);
                }

                String id = node.str(UiKey.id);
                if(id != null){
                    cell.name(id); // assigns element.name via Cell.name()
                    ctx.idElements.put(id, cell.get());
                }

            }else{
                applyTableProp(table, entry.key, entry.value); // a loose prop on this table's own body (background, margin, ...)
            }
        }
    }

    /** Constructs the element for one node, adds it to the parent table, and returns its cell. */
    private static Element addNode(UiKey type, NodeBuilder<?> node, BuildContext ctx){
        return switch(type){
            case table -> {
                String bg = node.str(UiKey.background);
                Table t = node.bool(UiKey.wrap) ? new WrapTable() : new Table();
                if(bg != null) t.background(findDrawable(bg));
                Float margin = node.num(UiKey.margin);
                if(margin != null) t.margin(margin);

                buildInto(t, node.entries, ctx); // recurse with a fresh defaults scope
                yield t;
            }
            case stack -> {
                Stack stack = new Stack();
                stack.touchable = Touchable.childrenOnly;

                //simplified creation compared to table; no cells are involved
                for(Entry entry : node.entries){
                    if(entry.value instanceof NodeBuilder<?> child){
                        String cond = child.str(UiKey.condition);
                        if(cond != null && !evalCondition(cond)) continue;

                        var element = addNode(entry.key, child, ctx);
                        if(element == null) continue; // unknown/unsupported node - skip for forward compat
                        String id = child.str(UiKey.id);

                        if(id != null){
                            element.name = id;
                            ctx.idElements.put(id, element);
                        }
                        String colorStr = child.str(UiKey.color); // Apply color if provided
                        if(colorStr != null) element.setColor(Strings.parseColor(colorStr, Color.white));

                        stack.add(element);
                    }
                }
                yield stack;
            }
            case pane -> {
                Table inner = new Table();
                buildInto(inner, node.entries, ctx);
                ScrollPane pane = new ScrollPane(inner);
                applyStyle(node, ScrollPaneStyle.class, pane::setStyle);
                yield pane;
            }
            case label -> {
                Label label = new Label(node.str(UiKey.text, ""));
                label.setWrap(node.bool(UiKey.wrap));
                String labelAlign = node.str(UiKey.labelAlign);
                if(labelAlign != null) label.setAlignment(parseAlign(labelAlign));
                applyStyle(node, LabelStyle.class, label::setStyle);
                yield label;
            }
            case image -> {
                String region = node.str(UiKey.region, node.str(UiKey.icon, "error"));
                Image image = new Image(findDrawable(region, node.str(UiKey.placeholder)));
                // track server-streamed images so that they can be reloaded when streaming finishes
                if(region.startsWith(DataImagePacker.serverRegionPrefix)) ctx.images.get(region, Seq::new).add(image);
                String scl = node.str(UiKey.scaling);
                if(scl != null){
                    try{
                        Scaling scaling = Scaling.valueOf(scl);
                        image.setScaling(scaling);
                    }catch(Exception ignored){}
                }
                yield image;
            }
            case button -> {
                TextButton btn = new TextButton(node.str(UiKey.text, ""));
                applyStyle(node, TextButtonStyle.class, btn::setStyle);

                String icon = node.str(UiKey.icon);
                if(icon != null){
                    Image iconImage = new Image(findDrawable(icon, node.str(UiKey.placeholder))).setScaling(Scaling.fit);
                    // track server-streamed images so that they can be reloaded when streaming finishes
                    if(icon.startsWith(DataImagePacker.serverRegionPrefix)) ctx.images.get(icon, Seq::new).add(iconImage);
                    btn.add(iconImage).size(32f);
                    btn.getCells().reverse();
                }
                wireButton(btn, node, ctx);
                yield btn;
            }
            case imageButton -> {
                String icon = node.str(UiKey.icon);
                ImageButton btn = new ImageButton(icon != null ? findDrawable(icon, node.str(UiKey.placeholder)) : null);
                // track server-streamed images so that they can be reloaded when streaming finishes
                if(icon != null && icon.startsWith(DataImagePacker.serverRegionPrefix)) ctx.images.get(icon, Seq::new).add(btn.getImage());
                applyStyle(node, ImageButtonStyle.class, btn::setStyle);
                wireButton(btn, node, ctx);
                yield btn;
            }
            case field -> {
                TextField field = new TextField(node.str(UiKey.text, ""));
                String hint = node.str(UiKey.hint);
                if(hint != null) field.setMessageText(hint);
                Float maxLen = node.num(UiKey.maxLength);
                field.setMaxLength(maxLen == null ? 1000 : Math.min(maxLen.intValue(), 1000));
                String enter = node.str(UiKey.enter);
                if(enter != null) field.keyDown(KeyCode.enter, () -> fireResult(ctx, enter));
                applyStyle(node, TextFieldStyle.class, field::setStyle);
                yield field;
            }
            case check -> {
                CheckBox box = new CheckBox(node.str(UiKey.text, ""));
                box.setChecked(node.bool(UiKey.checked));
                String group = node.str(UiKey.group);
                if(group != null) ctx.group(group).add(box);
                applyStyle(node, CheckBoxStyle.class, box::setStyle);
                yield box;
            }
            case slider -> {
                Slider slider = new Slider(node.num(UiKey.min, 0f), node.num(UiKey.max, 1f), node.num(UiKey.step, 0.1f), false);
                Float def = node.num(UiKey.defaultValue);
                if(def != null) slider.setValue(def);
                applyStyle(node, SliderStyle.class, slider::setStyle);

                String text = node.str(UiKey.text);
                boolean useBundle = text != null && text.length() > 0 && text.charAt(0) == '@';
                String bundleKey = useBundle ? text.substring(1) : null;

                Label label = new Label(() -> {
                    String formatted = Strings.autoFixed(slider.getValue(), 2);
                    if(bundleKey != null){
                        return Core.bundle.format(bundleKey, formatted);
                    }else if(text != null){
                        return text + ": " + formatted;
                    }else{
                        return formatted;
                    }
                });
                label.setAlignment(Align.center);
                label.touchable = Touchable.disabled;
                label.setStyle(Styles.outlineLabel);
                yield new Stack(slider, label);
            }
            case buttonTable -> {
                Button btn = new Button();
                //can use any button style
                if(!applyStyle(node, TextButtonStyle.class, btn::setStyle)) applyStyle(node, ImageButtonStyle.class, btn::setStyle);
                Float margin = node.num(UiKey.margin);
                if(margin != null) btn.margin(margin);
                buildInto(btn, node.entries, ctx); // build the button's own contents into it, like table/pane
                wireButton(btn, node, ctx);
                yield btn;
            }
            default -> null;
        };
    }

    /** Looks up a style by the node's "style" value (if set) and applies it via the given setter; no-op if absent/unknown. */
    private static <S> boolean applyStyle(NodeBuilder<?> node, Class<S> styleType, Cons<S> setter){
        String name = node.str(UiKey.style);
        if(name == null) return false;
        S style = UiStyleLookup.get(styleType, name);
        if(style != null){
            setter.get(style);
            return true;
        }
        return false;
    }

    /** Sets up the click callback and button group, if present.*/
    private static void wireButton(Button element, NodeBuilder<?> node, BuildContext ctx){
        String group = node.str(UiKey.group);
        if(group != null){
            ctx.group(group).add(element);
        }

        element.setChecked(node.bool(UiKey.checked));

        String result = node.str(UiKey.clicked);
        if(result != null && ctx != null && ctx.resultListener != null){
            element.clicked(() -> fireResult(ctx, result));
        }

    }

    private static void fireResult(BuildContext ctx, String result){
        MenuResult res = new MenuResult(result);
        for(var entry : ctx.idElements){
            Element el = entry.value;
            if(el instanceof Slider s) res.values.put(entry.key, s.getValue());
            else if(el instanceof Stack stack && stack.getChildren().size > 0 && stack.getChildren().get(0) instanceof Slider slider) res.values.put(entry.key, slider.getValue());
            else if(el instanceof TextField f) res.values.put(entry.key, f.getText());
            else if(el instanceof CheckBox c) res.values.put(entry.key, c.isChecked());
            //the only thing distinguishing buttons that can be checked is that they have a style for it; it's just not visible otherwise.
            else if(el instanceof Button b && b.getStyle().checked != null) res.values.put(entry.key, b.isChecked());
        }
        ctx.resultListener.get(res);
    }

    /** Properties that apply to the table itself rather than to a cell (found loose in a table's body). */
    private static void applyTableProp(Table table, UiKey key, Object value){
        switch(key){
            case background -> table.setBackground(findDrawable((String)value));
            case margin -> table.margin((Float)value);
            case align -> table.align(parseAlign((String)value));
            default -> {}
        }
    }

    private static void applyCellProp(Cell<?> cell, UiKey key, Object value){
        if(value == Boolean.TRUE){
            switch(key){
                case grow -> cell.grow();
                case growX -> cell.growX();
                case growY -> cell.growY();
                case fill -> cell.fill();
                case fillX -> cell.fillX();
                case fillY -> cell.fillY();
                case expand -> cell.expand();
                case expandX -> cell.expandX();
                case expandY -> cell.expandY();
                case uniform -> cell.uniform();
                case uniformX -> cell.uniformX();
                case uniformY -> cell.uniformY();
                //not a cell property, technically, only applies to buttons/sliders/text fields
                case disabled -> { if(cell.get() instanceof Disableable d) d.setDisabled(true); }
                default -> {}
            }
        }else{
            switch(key){
                case width -> cell.width((Float)value);
                case height -> cell.height((Float)value);
                case size -> cell.size((Float)value);
                case minWidth -> cell.minWidth((Float)value);
                case maxWidth -> cell.maxWidth((Float)value);
                case minHeight -> cell.minHeight((Float)value);
                case maxHeight -> cell.maxHeight((Float)value);
                case pad -> cell.pad((Float)value);
                case padTop -> cell.padTop((Float)value);
                case padLeft -> cell.padLeft((Float)value);
                case padBottom -> cell.padBottom((Float)value);
                case padRight -> cell.padRight((Float)value);
                case align -> cell.align(parseAlign((String)value));
                case colspan -> cell.colspan(((Float)value).intValue());
                case color -> { //technically not a layout property, but all elements have it, so it's applied here
                    if(value instanceof String s) cell.color(Strings.parseColor(s, Color.white));
                }
                default -> {}
            }
        }
    }

    private static Drawable findDrawable(String name){
        return findDrawable(name, "error");
    }

    /** @param placeholder shown instead of the error sprite if {@code name} isn't streamed from the server yet */
    private static Drawable findDrawable(String name, @Nullable String placeholder){
        if(Core.atlas.has(name)){
            return Core.atlas.drawable(name);
        }
        //icons are a fallback (TODO: bad idea?)
        var result = Icon.icons.get(name);
        if(result != null) return result;

        if(placeholder != null) return findDrawable(placeholder, null);
        return Core.atlas.drawable("nomap"); // no placeholder provided, use the default of the map preview texture
    }

    /** Evaluates a condition string: "portrait", "landscape", or "width|height >=|>|<|<= number". */
    private static boolean evalCondition(String cond){
        cond = cond.trim();
        if(cond.equals("portrait")) return Core.graphics.isPortrait();
        if(cond.equals("landscape")) return !Core.graphics.isPortrait();

        String[] parts = cond.split("\\s+");
        if(parts.length != 3) return true; // malformed - don't block layout

        float dim = switch(parts[0]){
            case "width" -> Core.scene.getWidth() / Scl.scl(1f);
            case "height" -> Core.scene.getHeight() / Scl.scl(1f);
            default -> 0f;
        };
        float num = Strings.parseFloat(parts[2], Float.NaN);

        if(Float.isNaN(num)) return true;

        return switch(parts[1]){
            case ">=" -> dim >= num;
            case ">" -> dim > num;
            case "<=" -> dim <= num;
            case "<" -> dim < num;
            default -> true;
        };
    }

    private static int parseAlign(String value){
        return switch(value){
            case "center" -> Align.center;
            case "top" -> Align.top;
            case "bottom" -> Align.bottom;
            case "left" -> Align.left;
            case "right" -> Align.right;
            case "topLeft" -> Align.topLeft;
            case "topRight" -> Align.topRight;
            case "bottomLeft", "botLeft" -> Align.bottomLeft;
            case "bottomRight", "botRight" -> Align.bottomRight;
            default -> Align.center;
        };
    }

    private static class BuildContext{
        final @Nullable Cons<MenuResult> resultListener;
        final ObjectMap<String, Element> idElements = new ObjectMap<>();
        final ObjectMap<String, ButtonGroup<Button>> buttonGroups = new ObjectMap<>();
        final ObjectMap<String, Seq<Image>> images = new ObjectMap<>();

        BuildContext(@Nullable Cons<MenuResult> resultListener){
            this.resultListener = resultListener;
        }

        ButtonGroup<Button> group(String name){
            return buttonGroups.get(name, ButtonGroup::new);
        }
    }

    /** Result of building a NodeBuilder: named elements (those with an explicit id), plus any server-streamed images, keyed by region name */
    public static class BuildResult{
        public final ObjectMap<String, Element> idElements;
        public final ObjectMap<String, Seq<Image>> images;

        BuildResult(ObjectMap<String, Element> idElements, ObjectMap<String, Seq<Image>> images){
            this.idElements = idElements;
            this.images = images;
        }
    }
}