# mindustry-native

Rust port of the hottest CPU path in `mindustry.ai.Pathfinder`: flow-field (Dijkstra-style)
computation. Exposed to Java over JNI as a `cdylib`; the game keeps a full Java fallback.

## What is ported

- `PathTileStruct` bit layout and all six `costTypes` cost functions (`tile_cost`).
- `updateTargets` + `updateFrontier` relaxation (`build`), including the `searches`
  staleness rule and 4-neighbour traversal. The Java version time-slices the work with an
  8 ms budget; this port computes the whole field in one pass on the pathfinding thread,
  so one JNI call covers hundreds of thousands of tiles.

Not ported: target discovery (`getPositions`), world/tile packing, the UI and networking.
Those stay in Java.

## JNI contract

`mindustry.ai.RustPathfinder.build(int[] tiles, int[] weights, short[] searches,
int[] targets, int team, int cost, int search, int width, int height)` returns `boolean`.

Arrays are copied into owned buffers, the field is computed there, and results are written
back **only on success**. A `false` return leaves the Java arrays untouched, so
`Pathfinder.rebuildNative` can fall back to the incremental Java implementation.

## Library names

Arc's `SharedLibraryLoader.load("mindustry_native")` looks for these classpath resources:

| Platform | File |
| --- | --- |
| Linux x64 | `libmindustry_native64.so` |
| Windows x64 | `mindustry_native64.dll` |

The release workflow builds both and copies them to `core/assets/`, which `desktop:dist`
puts at the root of `Mindustry.jar`. Android, iOS and macOS have no bundled library and
transparently use the Java pathfinder.

## Build & test

```sh
cargo test --manifest-path rust/mindustry-native/Cargo.toml
cargo build --release --manifest-path rust/mindustry-native/Cargo.toml
# Windows cross-build (needs mingw-w64):
CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER=x86_64-w64-mingw32-gcc \
  cargo build --release --target x86_64-pc-windows-gnu --manifest-path rust/mindustry-native/Cargo.toml
```

`cargo test` checks the packed-tile accessors, cost-function vectors against the Java
formulas, flat-grid distances, wall handling, and parity with a Bellman-Ford reference on
pseudo-random maps. `.github/workflows/rust-native.yml` also runs the Java-side
`RustPathfinderTests` against the compiled library.
