package mindustry.graphics.g3d;

import arc.math.geom.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.graphics.g3d.PlanetRenderer.*;
import mindustry.type.*;

/** Parameters for rendering a solar system. */
public class PlanetParams{
    /** Camera direction relative to the planet. Length is determined by zoom. */
    public Vec3 camPos = new Vec3(0f, 0f, 4f);
    /** If not null, this is the position of the "previous" planet for smooth camera movement. */
    public @Nullable Vec3 otherCamPos;
    /** Interpolation value for otherCamPos. */
    public float otherCamAlpha = 0f;
    /** Camera up vector. */
    public Vec3 camUp = new Vec3(0f, 1f, 0f);
    /** the unit length direction vector of the camera **/
    public Vec3 camDir = new Vec3(0, 0, -1);
    /** Planet being looked at. */
    public Planet planet = Planets.serpulo;

    /** Zoom relative to planet. */
    public float zoom = 1f;
    /** If true, the camera uses freeCamPos/freeCamYaw/freeCamPitch instead of orbiting the planet. */
    public boolean freeCamera = false;
    /** Free-camera position, in scene units. */
    public Vec3 freeCamPos = new Vec3(0f, 0f, 4f);
    /** Free-camera yaw/pitch, in radians. */
    public float freeCamYaw, freeCamPitch;
    /** Optional saved camera, used for per-planet orbit/rotation overrides. */
    public @Nullable transient SpaceCamera spaceCamera;
    /** Alpha of orbit rings and other UI elements. */
    public float uiAlpha = 1f;
    /** If false, orbit and sector grid are not drawn. */
    public boolean drawUi = false;
    /** If true, a space skybox is drawn. */
    public boolean drawSkybox = true;
    /** If true, this is being rendered inside the world and shouldn't have extra AA applied. */
    public boolean disableAA = false;

    /** Handles drawing details. */
    public @Nullable transient PlanetInterfaceRenderer renderer;
    /** Viewport size. <=0 to use screen size. Do not change in rules. */
    public transient int viewW = -1, viewH = -1;
    /** If true, atmosphere will be drawn regardless of player options. */
    public transient boolean alwaysDrawAtmosphere = false;
}
