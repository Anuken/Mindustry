package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.utils.Array;
import io.anuke.arc.utils.async.AsyncExecutor;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.WorldGenerator.GenResult;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.arc.graphics.Draw;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.scene.utils.Cursors;
import io.anuke.arc.util.GridMap;
import io.anuke.arc.util.Mathf;

import static io.anuke.mindustry.Vars.sectorSize;
import static io.anuke.mindustry.Vars.world;

public class GenViewDialog extends FloatingDialog{
    Array<Item> ores = Array.with(Items.copper, Items.lead, Items.coal);

    public GenViewDialog(){
        super("generate view");

        content().add(new GenView()).grow();
    }

    public class GenView extends Element{
        GridMap<Texture> map = new GridMap<>();
        GridMap<Boolean> processing = new GridMap<>();
        float panX, panY;
        float lastX, lastY;
        int viewsize = 3;
        AsyncExecutor async = new AsyncExecutor(Mathf.sqr(viewsize*2));

        {
            addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button){
                    Cursors.setHand();
                    lastX = x;
                    lastY = y;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    panX -= x - lastX;
                    panY -= y - lastY;

                    lastX = x;
                    lastY = y;
                }

                @Override
                public void touchUp(InputEvent event, float x, float y, int pointer, int button){
                    Cursors.restoreCursor();
                }
            });
        }

        public void draw(){
            float padSectorSize = 200f;
            int tx = (int)(panX / padSectorSize);
            int ty = (int)(panY / padSectorSize);

            Draw.color();

            for(int x = -viewsize; x <= viewsize; x++){
                for(int y = -viewsize; y <= viewsize; y++){
                    int wx = tx + x, wy = ty + y;
                    if(map.get(wx, wy) == null){
                        if(processing.get(wx, wy) == Boolean.TRUE){
                            continue;
                        }
                        processing.put(wx, wy, true);
                        async.submit(() -> {
                            GenResult result = new GenResult();
                            Pixmap pixmap = new Pixmap(sectorSize, sectorSize, Format.RGBA8888);
                            for(int i = 0; i < sectorSize; i++){
                                for(int j = 0; j < sectorSize; j++){
                                    world.generator.generateTile(result, wx, wy, i, j, true, null, ores);
                                    pixmap.drawPixel(i, sectorSize - 1 - j, ColorMapper.colorFor(result.floor, result.wall, Team.none, result.elevation, (byte)0));
                                }
                            }
                            Core.app.post(() -> map.put(wx, wy, new Texture(pixmap)));
                            return pixmap;
                        });

                        continue;
                    }

                    float drawX = x + width/2f+ wx * padSectorSize - tx * padSectorSize - panX % padSectorSize;
                    float drawY = y + height/2f + wy * padSectorSize - ty * padSectorSize - panY % padSectorSize;

                    Draw.rect(map.get(wx, wy), drawX, drawY, padSectorSize, padSectorSize);
                }
            }
        }
    }
}
