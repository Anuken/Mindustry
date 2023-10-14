package mindustry.graphics;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.Nullable;

import java.util.*;

public class Voronoi{
    private final static int LE = 0;
    private final static int RE = 1;

    private final static float minDistanceBetweenSites = 1F;

    public static Seq<GraphEdge> generate(Vec2[] values, float minX, float maxX, float minY, float maxY){
        Seq<GraphEdge> allEdges = new Seq<>();

        int nsites = values.length;

        float sn = (float)nsites + 4;
        int rtsites = (int)Math.sqrt(sn);

        Site[] sites = new Site[nsites];
        Vec2 first = values[0];
        float xmin = first.x;
        float ymin = first.y;
        float xmax = first.x;
        float ymax = first.y;
        for(int i = 0; i < nsites; i++){
            sites[i] = new Site();
            sites[i].coord.set(values[i]);
            sites[i].sitenbr = i;

            if(values[i].x < xmin){
                xmin = values[i].x;
            }else if(values[i].x > xmax){
                xmax = values[i].x;
            }

            if(values[i].y < ymin){
                ymin = values[i].y;
            }else if(values[i].y > ymax){
                ymax = values[i].y;
            }
        }

        Arrays.sort(sites, (p1, p2) -> {
            Vec2 s1 = p1.coord, s2 = p2.coord;
            if(s1.y < s2.y){
                return (-1);
            }
            if(s1.y > s2.y){
                return (1);
            }
            return Float.compare(s1.x, s2.x);
        });

        float deltay = ymax - ymin;
        float deltax = xmax - xmin;

        // Check bounding box inputs - if mins are bigger than maxes, swap them
        float temp;
        if(minX > maxX){
            temp = minX;
            minX = maxX;
            maxX = temp;
        }
        if(minY > maxY){
            temp = minY;
            minY = maxY;
            maxY = temp;
        }
        BorderBounds borderBounds = new BorderBounds(minX, maxX, minY, maxY);

        int siteidx = 0;

        int PQhashsize = 4 * rtsites;
        int i1;
        int ELhashsize = 2 * rtsites;
        Halfedge[] ELhash = new Halfedge[ELhashsize];

        for(i1 = 0; i1 < ELhashsize; i1 += 1){
            ELhash[i1] = null;
        }
        Halfedge ELleftend = newHe(null, 0);
        Halfedge ELrightend = newHe(null, 0);
        ELleftend.ELleft = null;
        ELleftend.ELright = ELrightend;
        ELrightend.ELleft = ELleftend;
        ELrightend.ELright = null;
        ELhash[0] = ELleftend;
        ELhash[ELhashsize - 1] = ELrightend;

        Site bottomsite = sites[siteidx ++];
        Site newsite = siteidx < nsites ? sites[siteidx ++] : null;
        Halfedge lbnd;
        Vec2 newintstar = null;
        Edge e;
        int nvertices = 0;
        EdgesCount edgesCount = new EdgesCount();
        PqHolder pqHolder = new PqHolder(ymin, deltay, PQhashsize);
        while(true){
            Vec2 tempIntStar = pqHolder.createNewIntStar();
            if (tempIntStar != null){
                newintstar = tempIntStar;
            }

            Halfedge rbnd;
            Halfedge bisector;
            Site p;
            Site bot;

            if(newsite != null && (pqHolder.PQcount == 0 || newsite.coord.y < newintstar.y || (newsite.coord.y == newintstar.y && newsite.coord.x < newintstar.x))){
                int bucket = (int)(((newsite.coord).x - xmin) / deltax * ELhashsize);

                if(bucket < 0){
                    bucket = 0;
                }
                if(bucket >= ELhashsize){
                    bucket = ELhashsize - 1;
                }

                Halfedge he = getHash(ELhash, bucket);
                if(he == null){
                    for(int i = 1; i < ELhashsize; i += 1){
                        if((he = getHash(ELhash, bucket - i)) != null){
                            break;
                        }
                        if((he = getHash(ELhash, bucket + i)) != null){
                            break;
                        }
                    }
                }
                if(he == ELleftend || (he != ELrightend && right(he, (newsite.coord)))){
                    do{
                        he = he.ELright;
                    }while(he != ELrightend && right(he, (newsite.coord)));
                    he = he.ELleft;
                }else{
                    do{
                        he = he.ELleft;
                    }while(he != ELleftend && !right(he, (newsite.coord)));
                }

                if(bucket > 0 && bucket < ELhashsize - 1){
                    ELhash[bucket] = he;
                }
                lbnd = he;
                rbnd = lbnd.ELright;

                bot = lbnd.ELedge == null ? bottomsite : rightreg(lbnd);
                e = bisect(bot, newsite, edgesCount);

                bisector = newHe(e, LE);
                insert(lbnd, bisector);

                if((p = intersect(lbnd, bisector)) != null){
                    pqdelete(lbnd, pqHolder);
                    pqinsert(lbnd, p, p.coord.dst(newsite.coord), pqHolder);
                }
                lbnd = bisector;
                bisector = newHe(e, RE);
                insert(lbnd, bisector);

                if((p = intersect(bisector, rbnd)) != null){
                    pqinsert(bisector, p, p.coord.dst(newsite.coord), pqHolder);
                }
                newsite = siteidx < nsites ? sites[siteidx ++] : null;
            }else if(!(pqHolder.PQcount == 0)){
                Halfedge curr;

                curr = pqHolder.PQhash[pqHolder.PQmin].PQnext;
                pqHolder.PQhash[pqHolder.PQmin].PQnext = curr.PQnext;
                pqHolder.PQcount -= 1;
                lbnd = (curr);
                Halfedge llbnd = lbnd.ELleft;
                rbnd = lbnd.ELright;
                Halfedge rrbnd = (rbnd.ELright);
                bot = lbnd.ELedge == null ? bottomsite : leftReg(lbnd);
                Site top = rbnd.ELedge == null ? bottomsite : rightreg(rbnd);

                Site v = lbnd.vertex;
                v.sitenbr = nvertices;
                nvertices += 1;
                GraphEdge lEdge = endpoint(lbnd.ELedge, borderBounds, lbnd.ELpm, v);
                if (lEdge != null) {
                    allEdges.add(lEdge);
                }
                GraphEdge rEdge = endpoint(rbnd.ELedge, borderBounds, rbnd.ELpm, v);
                if (rEdge != null) {
                    allEdges.add(rEdge);
                }
                delete(lbnd);
                pqdelete(rbnd, pqHolder);
                delete(rbnd);
                int pm = LE;

                if(bot.coord.y > top.coord.y){
                    Site temp1 = bot;
                    bot = top;
                    top = temp1;
                    pm = RE;
                }

                e = bisect(bot, top, edgesCount);
                bisector = newHe(e, pm);
                insert(llbnd, bisector);
                GraphEdge reEdge = endpoint(e, borderBounds, RE - pm, v);
                if (reEdge != null) {
                    allEdges.add(reEdge);
                }

                if((p = intersect(llbnd, bisector)) != null){
                    pqdelete(llbnd, pqHolder);
                    pqinsert(llbnd, p, p.coord.dst(bot.coord), pqHolder);
                }

                if((p = intersect(bisector, rrbnd)) != null){
                    pqinsert(bisector, p, p.coord.dst(bot.coord), pqHolder);
                }
            }else{
                break;
            }
        }

        for(lbnd = (ELleftend.ELright); lbnd != ELrightend; lbnd = (lbnd.ELright)){
            e = lbnd.ELedge;
            GraphEdge clippedEdge = clipLine(e, borderBounds);
            if (clippedEdge != null) {
                allEdges.add(clippedEdge);
            }
        }

        return allEdges;
    }

    private static Edge bisect(Site s1, Site s2, EdgesCount edgesCount){
        Edge newedge = new Edge();

        // store the sites that this edge is bisecting
        newedge.reg[0] = s1;
        newedge.reg[1] = s2;
        // to begin with, there are no endpoints on the bisector - it goes to
        // infinity
        newedge.ep[0] = null;
        newedge.ep[1] = null;

        // get the difference in x dist between the sites
        float dx = s2.coord.x - s1.coord.x;
        float dy = s2.coord.y - s1.coord.y;
        // make sure that the difference in positive
        float adx = dx > 0 ? dx : -dx;
        float ady = dy > 0 ? dy : -dy;
        newedge.c = s1.coord.x * dx + s1.coord.y * dy + (dx * dx + dy * dy) * 0.5f;// get the slope of the line

        if(adx > ady){
            newedge.a = 1.0f;
            newedge.b = dy / dx;
            newedge.c /= dx;// set formula of line, with x fixed to 1
        }else{
            newedge.b = 1.0f;
            newedge.a = dx / dy;
            newedge.c /= dy;// set formula of line, with y fixed to 1
        }

        newedge.edgenbr = edgesCount.amt;

        edgesCount.amt += 1;
        return newedge;
    }

    private static int pqbucket(Halfedge he, PqHolder pqHolder){
        int bucket;

        bucket = (int)((he.ystar - pqHolder.ymin) / pqHolder.deltay * pqHolder.PQhashsize);
        if(bucket < 0){
            bucket = 0;
        }
        if(bucket >= pqHolder.PQhashsize){
            bucket = pqHolder.PQhashsize - 1;
        }
        if(bucket < pqHolder.PQmin){
            pqHolder.PQmin = bucket;
        }
        return bucket;
    }

    // push the HalfEdge into the ordered linked list of vertices
    private static void pqinsert(Halfedge he, Site v, float offset, PqHolder pqHolder){
        Halfedge last, next;

        he.vertex = v;
        he.ystar = v.coord.y + offset;
        last = pqHolder.PQhash[pqbucket(he, pqHolder)];
        while((next = last.PQnext) != null
        && (he.ystar > next.ystar || (he.ystar == next.ystar && v.coord.x > next.vertex.coord.x))){
            last = next;
        }
        he.PQnext = last.PQnext;
        last.PQnext = he;
        pqHolder.PQcount += 1;
    }

    // remove the HalfEdge from the list of vertices
    private static void pqdelete(Halfedge he, PqHolder pqHolder){
        Halfedge last;

        if(he.vertex != null){
            last = pqHolder.PQhash[pqbucket(he, pqHolder)];
            while(last.PQnext != he){
                last = last.PQnext;
            }

            last.PQnext = he.PQnext;
            pqHolder.PQcount -= 1;
            he.vertex = null;
        }
    }

    private static Halfedge newHe(Edge e, int pm){
        Halfedge answer = new Halfedge();
        answer.ELedge = e;
        answer.ELpm = pm;
        answer.PQnext = null;
        answer.vertex = null;
        return answer;
    }

    private static Site leftReg(Halfedge he){
        return he.ELpm == LE ? he.ELedge.reg[LE] : he.ELedge.reg[RE];
    }

    private static void insert(Halfedge lb, Halfedge newHe){
        newHe.ELleft = lb;
        newHe.ELright = lb.ELright;
        lb.ELright.ELleft = newHe;
        lb.ELright = newHe;
    }

    /*
     * This delete routine can't reclaim node, since pointers from hash table
     * may be present.
     */
    private static void delete(Halfedge he){
        he.ELleft.ELright = he.ELright;
        he.ELright.ELleft = he.ELleft;
        he.deleted = true;
    }

    /* Get entry from hash table, pruning any deleted nodes */
    private static Halfedge getHash(Halfedge[] ELhash, int b){
        Halfedge he;

        if(b < 0 || b >= ELhash.length){
            return (null);
        }
        he = ELhash[b];
        if(he == null || !he.deleted){
            return (he);
        }

        /* Hash table points to deleted half edge. Patch as necessary. */
        ELhash[b] = null;
        return (null);
    }

    private static @Nullable GraphEdge clipLine(Edge e, BorderBounds borderBounds){
        float pxmin, pxmax, pymin, pymax;
        Site s1, s2;
        float x1, x2, y1, y2;

        x1 = e.reg[0].coord.x;
        x2 = e.reg[1].coord.x;
        y1 = e.reg[0].coord.y;
        y2 = e.reg[1].coord.y;

        // if the distance between the two points this line was created from is
        // less than the square root of 2, then ignore it
        if(Math.sqrt(((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1))) < minDistanceBetweenSites){
            return null;
        }
        pxmin = borderBounds.minX;
        pxmax = borderBounds.maxX;
        pymin = borderBounds.minY;
        pymax = borderBounds.maxY;

        if(e.a == 1.0 && e.b >= 0.0){
            s1 = e.ep[1];
            s2 = e.ep[0];
        }else{
            s1 = e.ep[0];
            s2 = e.ep[1];
        }

        if(e.a == 1.0){
            y1 = pymin;
            if(s1 != null && s1.coord.y > pymin){
                y1 = s1.coord.y;
            }
            if(y1 > pymax){
                y1 = pymax;
            }
            x1 = e.c - e.b * y1;
            y2 = pymax;
            if(s2 != null && s2.coord.y < pymax){
                y2 = s2.coord.y;
            }

            if(y2 < pymin){
                y2 = pymin;
            }
            x2 = (e.c) - (e.b) * y2;
            if(((x1 > pxmax) & (x2 > pxmax)) | ((x1 < pxmin) & (x2 < pxmin))){
                return null;
            }
            if(x1 > pxmax){
                x1 = pxmax;
                y1 = (e.c - x1) / e.b;
            }
            if(x1 < pxmin){
                x1 = pxmin;
                y1 = (e.c - x1) / e.b;
            }
            if(x2 > pxmax){
                x2 = pxmax;
                y2 = (e.c - x2) / e.b;
            }
            if(x2 < pxmin){
                x2 = pxmin;
                y2 = (e.c - x2) / e.b;
            }
        }else{
            x1 = pxmin;
            if(s1 != null && s1.coord.x > pxmin){
                x1 = s1.coord.x;
            }
            if(x1 > pxmax){
                x1 = pxmax;
            }
            y1 = e.c - e.a * x1;
            x2 = pxmax;
            if(s2 != null && s2.coord.x < pxmax){
                x2 = s2.coord.x;
            }
            if(x2 < pxmin){
                x2 = pxmin;
            }
            y2 = e.c - e.a * x2;
            if(((y1 > pymax) & (y2 > pymax)) | ((y1 < pymin) & (y2 < pymin))){
                return null;
            }
            if(y1 > pymax){
                y1 = pymax;
                x1 = (e.c - y1) / e.a;
            }
            if(y1 < pymin){
                y1 = pymin;
                x1 = (e.c - y1) / e.a;
            }
            if(y2 > pymax){
                y2 = pymax;
                x2 = (e.c - y2) / e.a;
            }
            if(y2 < pymin){
                y2 = pymin;
                x2 = (e.c - y2) / e.a;
            }
        }

        GraphEdge newEdge = new GraphEdge();
        newEdge.x1 = x1;
        newEdge.y1 = y1;
        newEdge.x2 = x2;
        newEdge.y2 = y2;

        newEdge.site1 = e.reg[0].sitenbr;
        newEdge.site2 = e.reg[1].sitenbr;
        return newEdge;
    }

    private static @Nullable GraphEdge endpoint(Edge e, BorderBounds borderBounds, int lr, Site s){
        e.ep[lr] = s;
        if(e.ep[RE - lr] == null){
            return null;
        }
        return clipLine(e, borderBounds);
    }

    private static boolean right(Halfedge el, Vec2 p){
        Edge e = el.ELedge;
        Site topsite = e.reg[1];
        boolean rightOf = p.x > topsite.coord.x;
        if(rightOf && el.ELpm == LE){
            return true;
        }
        if(!rightOf && el.ELpm == RE){
            return false;
        }

        boolean above;
        if(e.a == 1.0){
            float dyp = p.y - topsite.coord.y;
            float dxp = p.x - topsite.coord.x;
            boolean fast = false;
            if((!rightOf & (e.b < 0.0)) | (rightOf & (e.b >= 0.0))){
                above = dyp >= e.b * dxp;
                fast = above;
            }else{
                above = p.x + p.y * e.b > e.c;
                if(e.b < 0.0){
                    above = !above;
                }
                if(!above){
                    fast = true;
                }
            }
            if(!fast){
                float dxs = topsite.coord.x - (e.reg[0]).coord.x;
                above = e.b * (dxp * dxp - dyp * dyp) < dxs * dyp
                * (1.0 + 2.0 * dxp / dxs + e.b * e.b);
                if(e.b < 0.0){
                    above = !above;
                }
            }
        }else{
            float yl = e.c - e.a * p.x;
            float t1 = p.y - yl;
            float t2 = p.x - topsite.coord.x;
            float t3 = yl - topsite.coord.y;
            above = t1 * t1 > t2 * t2 + t3 * t3;
        }
        return ((el.ELpm == LE) == above);
    }

    private static Site rightreg(Halfedge he){
        return (he.ELpm == LE ? he.ELedge.reg[RE] : he.ELedge.reg[LE]);
    }

    private static Site intersect(Halfedge el1, Halfedge el2){
        Edge e1, e2, e;
        Halfedge el;
        float d, xint, yint;
        boolean right_of_site;
        Site v;

        e1 = el1.ELedge;
        e2 = el2.ELedge;
        if(e1 == null || e2 == null){
            return null;
        }

        if(e1.reg[1] == e2.reg[1]){
            return null;
        }

        d = e1.a * e2.b - e1.b * e2.a;
        if(-1.0e-10 < d && d < 1.0e-10){
            return null;
        }

        xint = (e1.c * e2.b - e2.c * e1.b) / d;
        yint = (e2.c * e1.a - e1.c * e2.a) / d;

        if((e1.reg[1].coord.y < e2.reg[1].coord.y)
        || (e1.reg[1].coord.y == e2.reg[1].coord.y && e1.reg[1].coord.x < e2.reg[1].coord.x)){
            el = el1;
            e = e1;
        }else{
            el = el2;
            e = e2;
        }

        right_of_site = xint >= e.reg[1].coord.x;
        if((right_of_site && el.ELpm == LE)
        || (!right_of_site && el.ELpm == RE)){
            return null;
        }

        v = new Site();
        v.coord.x = xint;
        v.coord.y = yint;
        return (v);
    }

    static class Site{
        Vec2 coord = new Vec2();
        int sitenbr;
    }

    static class Halfedge{
        Halfedge ELleft, ELright;
        Edge ELedge;
        boolean deleted;
        int ELpm;
        Site vertex;
        float ystar;
        Halfedge PQnext;
    }

    public static class GraphEdge{
        public float x1, y1, x2, y2;

        public int site1, site2;
    }

    static class Edge{
        float a = 0, b = 0, c = 0;
        Site[] ep = new Site[2];
        Site[] reg = new Site[2];
        int edgenbr;
    }

    static class PqHolder{
        final float ymin;
        final float deltay;
        final int PQhashsize;
        final Halfedge[] PQhash;
        int PQmin = 0;
        int PQcount = 0;

        public PqHolder(float ymin, float deltay, int PQhashsize){
            this.ymin = ymin;
            this.deltay = deltay;
            this.PQhashsize = PQhashsize;
            this.PQhash = new Halfedge[PQhashsize];
            for(int i = 0; i < PQhashsize; i++){
                PQhash[i] = new Halfedge();
            }
        }

        protected Vec2 createNewIntStar(){
            if(PQcount != 0){
                while(PQhash[PQmin].PQnext == null){
                    PQmin += 1;
                }
                return new Vec2(
                        PQhash[PQmin].PQnext.vertex.coord.x,
                        PQhash[PQmin].PQnext.ystar
                );
            }
            return null;
        }
    }

    static class BorderBounds{
        final float minX;
        final float maxX;
        final float minY;
        final float maxY;

        public BorderBounds(float minX, float maxX, float minY, float maxY){
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
    }

    static class EdgesCount{
        int amt;
    }
}
