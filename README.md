# undetected-final-release
Final Release for Undetected by Unsupervised Shopping Cart

Controls:
- Arrow keys:   Move the player character
- Space bar:    Interact with objects (switches and boxes)
- P:                  Pause
- R:                  Reset the current level
- M:                 Toggle minimap

Debug Controls:
- 1, 2:             Zoom in/out
- D:                Debugging outline display
- O:                Walk through walls


Interactions:
- Dropping box will snap the box to a tile position on the board.
- Walking over active laser/dropping box on active laser triggers an alarm that draws guards.
- Walking into camera lights will draw the guards.
- Guards navigate to the tile where the alarm was set off then return to the original path.
- Guards may change their patrol paths after the player takes the key.
- Turning the switches on and off toggles the connected doors, lasers, and cameras.
- Walking into the proximity of the guards will make them catch you, even if you're not in their lights.
