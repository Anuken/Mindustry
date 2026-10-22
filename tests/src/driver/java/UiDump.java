import arc.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.struct.*;

/**
 * Text description of the scene2d tree, so an agent can find and click things by name/label instead of guessing pixels from a screenshot.
 * All methods touch the scene, so they must run on the render thread (wrap in {@link ClientHarness#call}).
 * Coordinates are screenshot pixels (top-left origin), the same space {@link ClientInput} takes.
 */
public final class UiDump{
    private UiDump(){}

    /** A thing on screen. */
    public record Hit(Element element, String text, float x, float y, float w, float h){
        public float cx(){ return x + w / 2f; }
        public float cy(){ return y + h / 2f; }

        @Override
        public String toString(){
            return element.getClass().getSimpleName() + (text != null ? " \"" + text + "\"" : "") + " center=(" + (int)cx() + "," + (int)cy() + ") size=" + (int)w + "x" + (int)h;
        }
    }

    /** Indented tree of visible, on-screen elements that carry information. Interactive ones (buttons, fields, sliders, scroll panes) are marked '*'. */
    public static String dump(){
        StringBuilder out = new StringBuilder();
        Dialog dialog = Core.scene.getDialog();
        out.append("screen=").append(Core.graphics.getWidth()).append("x").append(Core.graphics.getHeight());
        out.append(" topDialog=").append(dialog == null ? "none" : dialog.getClass().getSimpleName() + " \"" + clean(dialog.title.getText().toString()) + "\"").append('\n');
        walk(Core.scene.root, 0, out);
        return out.toString();
    }

    private static void walk(Element e, int depth, StringBuilder out){
        if(!e.visible) return;

        boolean interactive = e instanceof Button || e instanceof TextField || e instanceof Slider || e instanceof ScrollPane;
        String text = textOf(e);
        boolean named = e.name != null && !e.name.isEmpty();

        //only print nodes that carry information, but always recurse
        if(interactive || text != null || named){
            Hit h = hit(e, text);
            if(h.w() > 0 && h.h() > 0 && onScreen(h)){
                out.append("  ".repeat(depth)).append(interactive ? "* " : "- ")
                .append(e.getClass().getSimpleName())
                .append(named ? " name=" + e.name : "")
                .append(text != null ? " \"" + text.replace("\n", "\\n") + "\"" : "")
                .append(e instanceof Slider s ? " value=" + s.getValue() : "")
                .append(" @(").append((int)h.cx()).append(",").append((int)h.cy()).append(") ")
                .append((int)h.w()).append("x").append((int)h.h())
                .append(e instanceof Button b && b.isDisabled() ? " DISABLED" : "")
                .append(e instanceof Button b && isToggled(b) ? " CHECKED" : "")
                .append('\n');
                depth++;
            }
        }

        if(e instanceof Group g){
            for(Element child : g.getChildren()) walk(child, depth, out);
        }
    }

    /** Arc flips isChecked on every click, even for plain buttons. Only report it where it is visible to the user: check boxes and styles with a checked look. */
    private static boolean isToggled(Button b){
        return b.isChecked() && (b instanceof CheckBox || (b.getStyle() != null && b.getStyle().checked != null));
    }

    private static boolean onScreen(Hit h){
        return h.x() < Core.graphics.getWidth() && h.x() + h.w() > 0 && h.y() < Core.graphics.getHeight() && h.y() + h.h() > 0;
    }

    /** Text of labels, buttons and fields, with Mindustry color tags like [accent] removed. */
    static String textOf(Element e){
        String raw =
            e instanceof Label l ? l.getText().toString() :
            e instanceof TextButton t ? t.getText().toString() :
            e instanceof TextField f ? f.getText() : null;
        return raw == null ? null : clean(raw);
    }

    private static String clean(String s){
        return s.replaceAll("\\[(?:#[0-9a-fA-F]{3,8}|[a-zA-Z_]*)\\]", "");
    }

    static Hit hit(Element e, String text){
        Vec2 a = e.localToStageCoordinates(new Vec2(0, 0)), b = e.localToStageCoordinates(new Vec2(e.getWidth(), e.getHeight()));
        //stage -> window pixels. project() is y-up (the space Arc input events use), screenshots are y-down.
        Vec2 p1 = Core.scene.getViewport().project(a), p2 = Core.scene.getViewport().project(b);
        float height = Core.graphics.getHeight();
        float left = Math.min(p1.x, p2.x), right = Math.max(p1.x, p2.x);
        float top = height - Math.max(p1.y, p2.y), bottom = height - Math.min(p1.y, p2.y);
        return new Hit(e, text, left, top, right - left, bottom - top);
    }

    /** All visible elements whose text or name contains the query (case-insensitive), in tree order. */
    public static Seq<Hit> find(String query){
        Seq<Hit> result = new Seq<>();
        collect(Core.scene.root, query.toLowerCase(), result);
        return result;
    }

    private static void collect(Element e, String q, Seq<Hit> out){
        if(!e.visible) return;
        String text = textOf(e);
        if((text != null && text.toLowerCase().contains(q)) || (e.name != null && e.name.toLowerCase().contains(q))){
            out.add(hit(e, text));
        }
        if(e instanceof Group g){
            for(Element child : g.getChildren()) collect(child, q, out);
        }
    }

    /**
     * The click target for a label: the nearest enclosing button (or the element itself), as long as clicking its center
     * would really reach it, i.e. it is not covered by another dialog. Throws with an explanation otherwise.
     */
    public static Hit findClickable(String query){
        Seq<Hit> all = find(query);
        if(all.isEmpty()) throw new IllegalArgumentException("Nothing visible matches \"" + query + "\". Run `ui` to see what is on screen.");

        for(Hit match : all){
            Element target = match.element();
            for(Element e = match.element(); e != null; e = e.parent){
                if(e instanceof Button){
                    target = e;
                    break;
                }
            }

            Hit h = hit(target, match.text());
            Vec2 stage = Core.scene.getViewport().unproject(new Vec2(h.cx(), Core.graphics.getHeight() - h.cy()));
            Element top = Core.scene.hit(stage.x, stage.y, true);

            if(top != null && (top == target || top.isDescendantOf(target))){
                return h;
            }
        }
        throw new IllegalStateException("\"" + query + "\" matches " + all.size + " element(s) but none can be clicked (covered by something else, or off screen): " + all.toString("; "));
    }
}
