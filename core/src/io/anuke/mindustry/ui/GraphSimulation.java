package io.anuke.mindustry.ui;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.SafeArray;

public class GraphSimulation<T>{
	public Array<Vertex<T>> vertices = new SafeArray<>();
	public Array<Edge<T>> edges = new SafeArray<>();
	public int frameWidth;
	public int frameHeight;
	public boolean equi = false;
	public float criterion = 1000;
	public float coolingRate = 0.065f;
	
	private static final float C = 1f;

	private int iteration = 0;
	private float k;
	private float t;
	private boolean equilibriumReached = false;

	private Vector2 deltaPos = new Vector2();

	public int startSimulation() {

		iteration = 0;
		equilibriumReached = false;

		int area = Math.min(frameWidth * frameWidth, frameHeight * frameHeight);
		k = C * Mathf.sqrt(area / vertices.size);
		t = frameWidth / 10;

		if (equi) {
			while (!equilibriumReached && iteration < 10000) {
				simulateStep();
			}
		} else {
			for (int i = 0; i < criterion; i++) {
				simulateStep();
			}
		}
		return iteration;
	}

	private void simulateStep() {
		for (Vertex<T> v : vertices) {
			v.disp.set(0, 0);
			for (Vertex<T> u : vertices) {
				if (v != u) {
					deltaPos.set(v.pos).sub(u.pos);
					float length = deltaPos.len();
					deltaPos.setLength(forceRepulsive(length, k));
					v.disp.add(deltaPos);
				}
			}
		}

		for (Edge<T> e : edges) {
			deltaPos.set(e.v.pos).sub(e.u.pos);
			float length = deltaPos.len();
			deltaPos.setLength(forceAttractive(length, k));

			e.v.disp.sub(deltaPos);
			e.u.disp.add(deltaPos);
		}

		equilibriumReached = true;

		for (Vertex<T> v : vertices) {

			deltaPos.set(v.disp);
			float length = deltaPos.len();

			if (length > criterion) {
				equilibriumReached = false;
			}

			deltaPos.setLength(Math.min(length, t));

			v.pos.add(deltaPos);
			v.pos.x = Mathf.clamp(v.pos.x, 0, frameWidth);
			v.pos.y = Mathf.clamp(v.pos.y, 0, frameHeight);
		}

		t = Math.max(t * (1 - coolingRate), 1);
		iteration++;
	}

	private float forceAttractive(float d, float k) {
		return d * d / k;
	}

	private float forceRepulsive(float d, float k) {
		return k * k /d;
	}

	public static class Vertex<T>{
		public Vector2 pos = new Vector2();
		public final T value;

		private Vector2 disp = new Vector2();

		public Vertex(T value){
			this.value = value;
		}
	}

	public static class Edge<T> {
		public final Vertex<T> v;
		public final Vertex<T> u;

		public Edge(Vertex<T> v, Vertex<T> u) {
			this.v = v;
			this.u = u;
		}
	}
}
