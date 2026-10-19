package mindustry.editor;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class EditorClipboardRenderer implements Disposable{
    private static final float pad = tilesize * 10f;
    //limited by the shared quad index buffer (SpriteIndices), big clipboards are split over multiple meshes
    private static final int maxSprites = 16000;

    private final Seq<EditorSpriteCache> caches = new Seq<>();
    private @Nullable EditorSpriteCache current;
    private float packW, packH;
    private final float[] verts = new float[maxSprites * EditorSpriteCache.vertexSize * 4];

    /** @return a cache with room for at least {@code needed} more sprites, flushing the current one if it is full. */
    private EditorSpriteCache cache(int needed){
        if(current != null && current.sprites() + needed > maxSprites) flush();
        if(current == null) current = new EditorSpriteCache(verts, pad, pad, packW, packH);
        return current;
    }

    /** Builds the current cache into a mesh and starts a new one on the next draw. */
    private void flush(){
        if(current == null) return;

        if(current.isEmpty()){
            current.dispose();
        }else{
            current.build(SpriteIndices.get());
            caches.add(current);
        }
        current = null;
    }

    public void rebuild(EditorClipboard c){
        dispose();
        c.dirty = false;

        packW = c.width * tilesize + 2 * pad;
        packH = c.height * tilesize + 2 * pad;

        float col = Tmp.c1.set(Color.white).a(0.75f).toFloatBits();

        for(int x = 0; x < c.width; x++){
            
            for(int y = 0; y < c.height; y++){
                int i = x + y * c.width;
                float wx = x * tilesize, wy = y * tilesize;

                if(content.block(c.floor[i]) instanceof Floor f){
                    TextureRegion r = f.variantRegions.length > 0 ? f.variantRegions[f.variant(x + c.srcX, y + c.srcY)] : f.fullIcon;
                    cache(1).draw(r, wx - tilesize / 2f, wy - tilesize / 2f, 0f, 0f, tilesize, tilesize, 0f, col);
                }
            }
        }

        for(int x = 0; x < c.width; x++){
            for(int y = 0; y < c.height; y++){
                int i = x + y * c.width;
                if(c.overlay[i] == 0) continue;

                float wx = x * tilesize, wy = y * tilesize;
                Block o = content.block(c.overlay[i]);
                TextureRegion r;
                float rot = 0f;

                if(o instanceof CharacterOverlay co){
                    r = co.letterRegions[CharOverlayData.character(c.overlayData[i])];
                    rot = CharOverlayData.rotation(c.overlayData[i]) * 90f;
                }else if(o instanceof RuneOverlay ro){
                    r = ro.letterRegions[Math.min(c.overlayData[i] & 0xff, RuneOverlay.characters - 1)];
                }else if(o instanceof Floor f && f.variantRegions.length > 0){
                    r = f.variantRegions[f.variant(x + c.srcX, y + c.srcY)];
                }else{
                    r = o.fullIcon;
                }

                cache(1).draw(r, wx - tilesize / 2f, wy - tilesize / 2f, tilesize / 2f, tilesize / 2f, tilesize, tilesize, rot, col);
            }
        }

        for(int x = 0; x < c.width; x++){
            for(int y = 0; y < c.height; y++){
                int i = x + y * c.width;
                if(c.block[i] == 0) continue;

                Block b = content.block(c.block[i]);
                TextureRegion r = b instanceof Cliff cl && cl.cliffs != null ? cl.cliffs[c.data[i] & 0xff] : b.fullIcon;
                float w = r.width * r.scl(), h = r.height * r.scl();

                cache(1).draw(r, x * tilesize + b.offset - w / 2f, y * tilesize + b.offset - h / 2f, w / 2f, h / 2f, w, h, 0f, col);
            }
        }

        TextureRegion team = Core.atlas.find("block-border");

        for(EditorClipboard.ClipBuild b : c.builds){
            TextureRegion r = b.block.fullIcon;
            float w = r.width * r.scl(), h = r.height * r.scl();
            float px = b.x * tilesize + b.block.offset - w / 2f, py = b.y * tilesize + b.block.offset - h / 2f;

            cache(2).draw(r, px, py, w / 2f, h / 2f, w, h, b.block.rotate && b.block.rotateDrawEditor ? b.rotation * 90f : 0f, col);
            cache(2).draw(team, px, py, 0f, 0f, team.width * team.scl(), team.height * team.scl(), 0f, Tmp.c2.set(mindustry.game.Team.get(b.team).color).a(0.75f).toFloatBits());
        }

        flush();
    }

    public void render(EditorClipboard c, Shader shader, Mat camMat, float worldX, float worldY){
        if(c.dirty) rebuild(c);
        if(caches.isEmpty()) return;

        shader.bind();
        shader.setUniformMatrix4("u_projTrans", Tmp.m1.set(camMat).translate(worldX - pad, worldY - pad).scale(packW, packH));
        for(EditorSpriteCache cache : caches){
            cache.render(shader);
        }
    }

    @Override
    public void dispose(){
        for(EditorSpriteCache cache : caches){
            cache.dispose();
        }
        caches.clear();

        if(current != null){
            current.dispose();
            current = null;
        }
    }
}
