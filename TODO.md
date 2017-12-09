_Keep in mind that this is just a basic outline of planned features, and will be constantly changing!_

### 3.0 Release
- New tutorial with the power blocks
- New SFX for specific blocks, especially turrets
- Block drawing layers. Refactor/remove `Block#drawOver()`, add `Layer` enum. Should fix 'glitchy' lasers and conveyor clipping
- Balance nuclear reactor, improve effectiveness as they are currently underpowered
- Make generation frame independent
- Investigate issue #5 (enemies stuck in blocks)
- Faster mech movement, possibly with a "boost" key
- Balance enemy difficulty

### 3.x Planned
- New save system: toggleable autosave, named slots, less clunky save UI
- Teleporter UI changes, more colors (?)
- New building tools: selection-delete, hold to place blocks in a line, one-tap delete mode (mobile). New 'tool' menu (desktop).
- Refactor `Renderer`, remove code for rendering platform-specific placement and move to 2 different classes
- New map format system. Each new version is a different class, convert between different versions.
- Underground conduits
- Minimap
- More indicators for when the core is damaged and/or under attack


### 4.0 Planned
- Multiplayer framework, possibly implementation
- New look to blocks, make them less 'blocky'

### Misc
- Localization support. Change all in-game strings to localized strings. Check compatibility with GWT and Android libraries.

### Possible Additions
- Mech body upgrades
- Uranium extractor / uranium->iron converter
- Laser enemies
- Flying enemies that move in formation and spawn from different locations
- Fusion reactor
- Point defense turrets that take down projectiles
- Turrets fueled by lava
- Gas transporation and use
- Better enemy effects and looks
- Homing missile enemies and turrets
- Reflective shield blocks
- Tech tree with bonuses to production or turrets
- Spawn points changed into enemy bases with hostile turrets
- Unit production

### Optmiziation
- Look into uses for `IntMap`
- Spread updating over multiple frames for large groups of specific tile entities (?)
- Optimize enemy + bullet code and check quadtree leaf parameters
- Check for unnecessary use of `Timers#get()`
- Optimize generator laser distribution, especially finding targets
- Optimize UI
- Check memory usage and GC, profile

