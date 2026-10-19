package math;

import arc.struct.Seq;
import arc.math.geom.Bezier;
import arc.math.geom.Vec2;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;

@RunWith(Parameterized.class)
public class BezierTest{

    private static float epsilon = Float.MIN_NORMAL;
    private static float epsilonApprimations = 1e-6f;
    @Parameter(0)
    public ImportType type;
    /** use constructor or setter */
    @Parameter(1)
    public boolean useSetter;
    private Bezier<Vec2> bezier;

    @Parameters(name = "use setter {0} imported type {1}")
    public static Collection<Object[]> parameters(){
        Collection<Object[]> parameters = new ArrayList<>();
        for(ImportType type : ImportType.values()){
            parameters.add(new Object[]{type, true});
            parameters.add(new Object[]{type, false});
        }
        return parameters;
    }

    @Before
    public void setup(){
        bezier = null;
    }

    protected Vec2[] create(Vec2[] points){
        if(useSetter){
            bezier = new Bezier<>();
            if(type == ImportType.ArcArrays){
                bezier.set(new Seq<>(points), 0, points.length);
            }else if(type == ImportType.JavaArrays){
                bezier.set(points, 0, points.length);
            }else{
                bezier.set(points);
            }
        }else{
            if(type == ImportType.ArcArrays){
                bezier = new Bezier<>(new Seq<>(points), 0, points.length);
            }else if(type == ImportType.JavaArrays){
                bezier = new Bezier<>(points, 0, points.length);
            }else{
                bezier = new Bezier<>(points);
            }

        }
        return points;
    }

    @Test
    public void testLinear2D(){
        Vec2[] points = create(new Vec2[]{new Vec2(0, 0), new Vec2(1, 1)});

        float len = bezier.approxLength(2);
        Assert.assertEquals(Math.sqrt(2), len, epsilonApprimations);

        Vec2 d = bezier.derivativeAt(new Vec2(), 0.5f);
        Assert.assertEquals(1, d.x, epsilon);
        Assert.assertEquals(1, d.y, epsilon);

        Vec2 v = bezier.valueAt(new Vec2(), 0.5f);
        Assert.assertEquals(0.5f, v.x, epsilon);
        Assert.assertEquals(0.5f, v.y, epsilon);

        float t = bezier.approximate(new Vec2(.5f, .5f));
        Assert.assertEquals(.5f, t, epsilonApprimations);

        float l = bezier.locate(new Vec2(.5f, .5f));
        Assert.assertEquals(.5f, t, epsilon);
    }

    private enum ImportType{
        ArcArrays, JavaArrays, JavaVarArgs
    }
}
