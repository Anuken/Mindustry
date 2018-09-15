package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.ui.GraphSimulation;
import io.anuke.mindustry.ui.GraphSimulation.Edge;
import io.anuke.mindustry.ui.GraphSimulation.Vertex;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Mathf;

public class UnlockGraphDialog extends FloatingDialog{
    int frameSize = 1000;
    ObjectMap<UnlockableContent, Vertex<UnlockableContent>> map = new ObjectMap<>();
    Array<Vertex<UnlockableContent>> vertices;
    Array<Edge<UnlockableContent>> edges;

    public UnlockGraphDialog(){
        super("$text.unlocks");

        rebuild();
    }

    public void rebuild(){
        content().clear();

        GraphSimulation<UnlockableContent> sim = new GraphSimulation<>();
        sim.frameWidth = frameSize;
        sim.frameHeight = frameSize;
        vertices = sim.vertices;
        edges = sim.edges;

        for(Item item : Vars.content.items()){
            if(item.type != ItemType.material) continue;
            put(item);
        }

        for(Recipe recipe : Vars.content.recipes()){
            if(recipe.requirements.length == 0) continue;
            put(recipe);

            for(Item item : Vars.content.items()){
                for(ItemStack stack : recipe.requirements){
                    if(stack.item == item){
                        link(item, recipe);
                        break;
                    }
                }
            }
        }

        sim.startSimulation();

        content().addRect((x, y, w, h) -> {
            float cx = x + w/2f, cy = y + h/2f;
            float ox = cx - frameSize/2f, oy = cy - frameSize/2f;

            Draw.color(Color.DARK_GRAY);
            Lines.stroke(4f);

            for(Edge<UnlockableContent> e : edges){
                if(e.v.value instanceof Item){
                    Draw.color(((Item) e.v.value).color);
                }
                Lines.line(e.u.pos.x + ox, e.u.pos.y + oy, e.v.pos.x + ox, e.v.pos.y + oy);
            }

            Draw.color();

            for(Vertex<UnlockableContent> v : vertices){
                Draw.rect(v.value.getContentIcon(), v.pos.x + ox, v.pos.y + oy, 16*2f, 16*2f);
            }
        }).grow();
    }

    private Vertex<UnlockableContent> get(UnlockableContent c){
        return map.get(c);
    }

    private void put(UnlockableContent c){
        Vertex<UnlockableContent> v = new Vertex<>(c);
        v.pos.set(frameSize/2f + Mathf.range(frameSize/2f), frameSize/2f + Mathf.range(frameSize/2f));
        map.put(c, v);
        vertices.add(v);
    }

    private void link(UnlockableContent a, UnlockableContent b){
        edges.add(new Edge<>(get(a), get(b)));
    }
}
