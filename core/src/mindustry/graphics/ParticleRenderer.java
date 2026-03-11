package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import mindustry.*;

import java.util.concurrent.*;

/** WIP experimental point-sprite based particle renderer. */
public class ParticleRenderer{
    static final boolean useAsync = true;
    static final int maxParticles = 100_000, maxParticlesPerFrame = 25_000;
    static final int particleSize =
    1 + //time
    1 + //total lifetime
    2 + //position xy
    2 + //velocity xy
    1 + //sizeFrom
    1 + //sizeTo
    1   //color
    ;  //TODO fade color?

    static final int particleVertexSize =
    2 + //xy
    1 + //size
    1 //color
    ;

    static final float globalDrag = 0.05f;

    static final float cullPadding = 8f*3f;

    static final VertexAttribute[] attributes = {VertexAttribute.position, new VertexAttribute(1, "a_size"), VertexAttribute.color};
    static Shader shader;

    float[] data = new float[maxParticles * particleSize];
    volatile int count;

    float[] addBuffer = new float[maxParticlesPerFrame * particleSize]; //TODO should be smaller than data
    int addCount;

    Mesh mesh = new Mesh(false, maxParticles, 0, attributes);
    float[] vertexBuffer = new float[maxParticles * particleVertexSize];
    volatile int vertexBufferLength;

    @Nullable Future<?> asyncTask;

    public int count(){
        return count;
    }

    public void updateAndRender(){
        if(!Vars.state.isPaused()){
            update();
        }

        render();
    }

    public void update(){
        if(useAsync && asyncTask != null){
            try{
                asyncTask.get();
            }catch(Exception e){
                Log.err(e);
            }
        }

        //append added particles to the queue
        int maxAdded = Math.min(maxParticles - count, addCount);
        //if maxAdded is less than addCount, prioritize particles at the end of the array (most recent)
        int addOffset = addCount - maxAdded;

        if(maxAdded > 0){
            System.arraycopy(addBuffer, addOffset * particleSize, data, count * particleSize, maxAdded * particleSize);
        }

        count += maxAdded;
        addCount = 0;

        //uses data calculated from previous frame
        uploadMeshData(mesh);

        if(useAsync){
            asyncTask = Vars.mainExecutor.submit(this::updateAsync);
        }else{
            updateAsync();
        }
    }

    void updateAsync(){
        count = update(data, count, Time.delta);
        buildVertices();
    }

    public void render(){
        if(shader == null) makeShader();

        Gl.enable(Gl.programPointSize);

        shader.bind();
        shader.setUniformMatrix4("u_mat", Core.camera.mat);
        shader.setUniformf("u_scaling", Core.graphics.getWidth() / Core.camera.width);

        mesh.render(shader, Gl.points);
    }

    public void add(float x, float y, float lifetime, float vx, float vy, float sizeFrom, float sizeTo, float color){
        if(addCount * particleSize >= addBuffer.length ||
            //ignore particles added not in the camera viewport
            //TODO fast-moving offscreen particles won't show up.
            !Rect.contains(
                x - Core.camera.width/2f - sizeFrom - cullPadding,
                y - Core.camera.height/2f - sizeFrom - cullPadding,
                Core.camera.width + sizeFrom*2f + cullPadding*2f,
                Core.camera.height + sizeFrom*2f + cullPadding*2f, x, y)) return;

        float[] buf = addBuffer;

        int i = addCount * particleSize;
        buf[i + 0] = 0f;
        buf[i + 1] = lifetime;
        buf[i + 2] = x;
        buf[i + 3] = y;
        buf[i + 4] = vx;
        buf[i + 5] = vy;
        buf[i + 6] = sizeFrom * 2f;
        buf[i + 7] = sizeTo * 2f;
        buf[i + 8] = color;

        addCount ++;
    }

    void buildVertices(){
        //TODO: cull based on camera viewport
        float[] data = this.data;
        float count = this.count * particleSize;

        float[] vertices = vertexBuffer;

        int bufferIndex = 0;

        for(int i = 0; i < count; i += particleSize){
            float color = data[i + 8]; //TODO: colorTo
            float size = Mathf.lerp(data[i + 6], data[i + 7], Mathf.clamp(data[i] / data[i + 1]));

            //xy
            vertices[bufferIndex + 0] = data[i + 2];
            vertices[bufferIndex + 1] = data[i + 3];
            //size
            vertices[bufferIndex + 2] = size;
            //color
            vertices[bufferIndex + 3] = color;

            bufferIndex += particleVertexSize;
        }

        vertexBufferLength = bufferIndex;
    }

    void uploadMeshData(Mesh mesh){
        var buffer = mesh.getVerticesBuffer();

        buffer.position(0);
        buffer.limit(vertexBufferLength);
        buffer.put(vertexBuffer, 0, vertexBufferLength);
        buffer.position(0);
    }

    static int update(float[] data, int count, float delta){
        int head = count * particleSize;

        float dragValue = Math.max(1f - globalDrag * delta, 0f);

        for(int i = 0; i < head; i += particleSize){
            data[i] += delta;
            if(data[i] >= data[i + 1]){
                if(head > particleSize){
                    //swap head
                    System.arraycopy(data, head - particleSize, data, i, particleSize);
                }
                head -= particleSize;
                i -= particleSize;
            }else{
                //velocity
                data[i + 2] += data[i + 4] * delta;
                data[i + 3] += data[i + 5] * delta;
                data[i + 4] *= dragValue;
                data[i + 5] *= dragValue;
            }
        }

        return head / particleSize;
    }

    static void makeShader(){
        shader = new Shader(
        """
        uniform mat4 u_mat;
        uniform float u_scaling;
        
        attribute vec4 a_position;
        attribute float a_size;
        attribute vec4 a_color;
        
        varying vec4 v_color;
        
        void main(){
            v_color = a_color;
    
            gl_Position = u_mat * a_position;
            gl_PointSize = a_size * u_scaling;
        }
        """,
        """
        varying lowp vec4 v_color;
        
        #define RAD1 0.43
        #define RAD2 0.5


        void main(){
            vec2 delta = gl_PointCoord - vec2(0.5);
            gl_FragColor = vec4(v_color.rgb, v_color.a * (1.0-smoothstep(RAD1*RAD1, RAD2*RAD2, delta.x * delta.x + delta.y * delta.y)));
        }   
        """
        );
    }
}
