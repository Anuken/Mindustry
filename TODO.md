_Keep in mind that this is just a basic outline of planned features, and will be constantly changing!_

### Won't Add
_(These are not planned in the near future at all, and have been suggested before **many** times.)_
- Texture packs
- Online player profiles
- Player mech on Android
- Modding support
- Speed increase (fast forward)

### Already Suggested
_(not necessarily planned!)_
- "more blocks" "more turrets" "more content" "more X/Y/Z"
- Multiplayer
- Building of units (tanks, drones, _soldiers_, doesn't matter)
- Enemy bases, fighting against AI, capture points
- Co-op of any sort
- Campaign, challenge mode
- Multiple cores
- Movable turrets
- Batteries or storage for anything
- Destroy map indestructible blocks
- Customizable world ore generation + seed
- Steam release
- Research system, tech tree, persistent upgrades, upgrades at all
- Missile enemies/turrets/weapons (both homing and non-homing)
- Better graphics
- Enemies dropping resources
- Final objectives/non-endless mode
- Fusion reactor
- Dams, flowing water
- Flying enemies
- Day/night cycle
- Solar panels
- Deflector shields
- Autosave

### Balance
- Slow down progression slightly
- Better endgame turrets (?)
- Nerf RTG, buff nuclear reactor
- Faster power

### Misc. QoL
- Minimap
- Underground conduits
- More indicators for core damaged/attacked
- Delete saves, export saves, import saves
- Display playtime in saves
- Edit descriptions for conveyor tunnels to be more clear about how to use them
- New map format system to display
- Better placement controls, break while placing
- Hide UI elements
- New liquid conduit system

### Major Bugs
- Black screen when tabbing out on Android
- Infinite explosions that destroy blocks
- Random map reload when playing, leading to a crash (UI cause?)
- Google Payments verify crash
- Google Payments IllegalArgument crash

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

