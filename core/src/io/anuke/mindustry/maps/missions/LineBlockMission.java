package io.anuke.mindustry.maps.missions;

import io.anuke.arc.collection.Array;
import io.anuke.arc.math.geom.Bresenham2;
import io.anuke.arc.math.geom.Point2;
import io.anuke.mindustry.world.Block;

public class LineBlockMission extends Mission{
    private Array<BlockLocMission> points = new Array<>();
    private int completeIndex;

    public LineBlockMission(Block block, int x1, int y1, int x2, int y2, int rotation){
        Array<Point2> points = new Bresenham2().line(x1, y1, x2, y2);
        for(Point2 point : points){
            this.points.add(new BlockLocMission(block, point.x, point.y, rotation));
        }
    }

    @Override
    public boolean isComplete(){
        while(completeIndex < points.size && points.get(completeIndex).isComplete()){
            completeIndex ++;
        }
        return completeIndex >= points.size;
    }

    @Override
    public void drawOverlay(){
        if(completeIndex < points.size){
            points.get(completeIndex).drawOverlay();
        }
    }

    @Override
    public void reset(){
        completeIndex = 0;
    }

    @Override
    public String displayString(){
        if(completeIndex < points.size){
            return points.get(completeIndex).displayString();
        }
        return points.first().displayString();
    }
}
