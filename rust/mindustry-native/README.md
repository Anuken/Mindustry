# mindustry-native

Rust ports of the hottest CPU paths in Mindustry, exposed to Java over JNI as a `cdylib`;
the game keeps full Java fallbacks. Both ports live in the same native library.

## What is ported

- `mindustry.ai.Pathfinder`: `PathTileStruct` bit layout and all six `costTypes` cost
  functions (`tile_cost`). `updateTargets` + `updateFrontier` relaxation (`build`),
  including the `searches` staleness rule and 4-neighbour traversal. The Java version
  time-slices the work with an 8 ms budget; this port computes the whole field in one pass
  on the pathfinding thread, so one JNI call covers hundreds of thousands of tiles.
- `mindustry.async.AvoidanceProcess`: the async per-frame unit-avoidance mask
  (`avoidance::build`). The Java version stamps a disk of up to 41×41 tiles per ground
  unit into a double-buffered `int[]`. This port does the same in one pass over owned
  copies of the request and buffer arrays, so one JNI call covers the whole frame. It
  mirrors Arc's `Point2.pack/x/y`, `Mathf.ceil`, `Mathf.clamp(1, ceil(rad), 20)` (which is
  `max(1, ceil(rad))` and has no 20-tile cap), Java int overflow, and the
  `Integer.MAX_VALUE - id` max-write. Non-finite radii or pathological `r > 256` return
  `false` so Java can fall back.

Not ported: target discovery (`getPositions`), world/tile packing, the UI, networking,
`PhysicsProcess` (QuadTree + `Rand` state), or `PowerGraph` (object-heavy graph update).
Those stay in Java.

## JNI contract

`mindustry.ai.RustPathfinder.build(int[] tiles, int[] weights, short[] searches,
int[] targets, int team, int cost, int search, int width, int height)` returns `boolean`.

`mindustry.async.RustAvoidance.build(int[] requests, int total, int[] buffer, int width,
int height, boolean clear)` returns `boolean`. `requests` contains triples of
`(Point2.pack(x, y), Float.floatToRawIntBits(radius), unit id)`; `total` is the number of
valid ints (the backing array may be longer). `clear` reproduces Java's `modified` flag:
when true, the target buffer is zeroed before stamping.

All Java arrays are copied into owned buffers, the work is computed there, and results are
written back **only on success**. A `false` return leaves the Java arrays untouched, so
`Pathfinder.rebuildNative` and `AvoidanceProcess.process` can fall back to the original
Java loops.

## Library names

Arc's `SharedLibraryLoader.load("mindustry_native")` looks for these classpath resources:

| Platform | File |
| --- | --- |
| Linux x64 | `libmindustry_native64.so` |
| Windows x64 | `mindustry_native64.dll` |

The release workflow builds both and copies them to `core/assets/`, which `desktop:dist`
puts at the root of `Mindustry.jar`. Android, iOS and macOS have no bundled library and
transparently use the Java implementations.

## Build & test

```sh
cargo test --manifest-path rust/mindustry-native/Cargo.toml
cargo build --release --manifest-path rust/mindustry-native/Cargo.toml
# Windows cross-build (needs mingw-w64):
CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER=x86_64-w64-mingw32-gcc \
  cargo build --release --target x86_64-pc-windows-gnu --manifest-path rust/mindustry-native/Cargo.toml
```

`cargo test` checks the packed-tile accessors, cost-function vectors against the Java
formulas, flat-grid distances, wall handling, parity with a Bellman-Ford reference on
pseudo-random maps, and the avoidance-mask edge cases. `.github/workflows/rust-native.yml`
also runs the Java-side `RustPathfinderTests` and `RustAvoidanceTests` against the compiled
library, and checks that both JNI symbols are exported.
