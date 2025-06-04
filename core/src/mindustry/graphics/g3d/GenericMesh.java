package mindustry.graphics.g3d;

import arc.math.geom.*;
import arc.util.*;

public interface GenericMesh extends Disposable{
    void render(PlanetParams params, Mat3D projection, Mat3D transform);
}
