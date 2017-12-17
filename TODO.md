_Keep in mind that this is just a basic outline of planned features, and will be constantly changing!_

### 3.x Planned
- New save system: toggleable autosave, named slots, less clunky save UI
- [DONE?] Optimize for tablets
- Teleporter UI changes, more colors (?)
- [DONE] New building tools: selection-delete, hold to place blocks in a line, one-tap delete mode (mobile). New 'tool' menu (desktop).
- [DONE] Refactor `Renderer`, remove code for rendering platform-specific placement and move to 2 different classes
- New map format system. Each new version is a different class, convert between different versions.
- Underground conduits
- Minimap
- More indicators for when the core is damaged and/or under attack
- Fix bugs with junction not accepting blocks (low FPS)
- Fix bugs with tunnel merging and/or removing items (low FPS)
- Edit descriptions for conveyor tunnels to be more clear about how to use them
- [DONE] Add link to Mindustry discord everywhere
- Balancing to slow down progression
- Map editor

### Major Bugs
- Black screen when tabbing out on Android
- Infinite explosions that destroy blocks
- Random map reload when playing, leading to a crash (UI cause?)
- Google Payments verify crash
- Google Payments IllegalArgument crash

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
- Optimize health bars and enemies in general
- Make drawing of enemies more efficient (don't call `flush()`?)
- Look into `NodeRecord` storage for pathfinder, since it's taking 2MB+ of memory!

