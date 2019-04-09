/*
 * Board.java
 * 
 * This class keeps track of all the tiles in the game. If a photon hits 
 * a ship on a Tile, then that Tile falls away.
 *
 * Because of this gameplay, there clearly has to be a lot of interaction
 * between the Board, Ships, and Photons.  However, this way leads to 
 * cyclical references.  As we will discover later in the class, cyclic
 * references are bad, because they lead to components that are too
 * tightly coupled.
 *
 * To address this problem, this project uses a philosophy of "passive"
 * models.  Models do not access the methods or fields of any other
 * Model class.  If we need for two Model objects to interact with
 * one another, this is handled in a controller class. This can get 
 * cumbersome at times (particularly in the coordinate transformation
 * methods in this class), but it makes it easier to modify our
 * code in the future.
 *
 * Author: Walker M. White, Cristian Zaloj
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 * LibGDX version, 1/24/2015
 */
package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;

/**
 * Class represents a 2D grid of tiles.
 *
 * Most of the work is done by the internal Tile class.  The outer class is
 * really just a container.
 */
public class Board {

	/**
	 * Each tile has a set of attributes associated with it.
	 */
	private static class TileState{
		/** Is this a valid tile? */
		public boolean isValid = true;
		/** Is this tile occupied? */
		public boolean isOccupied = false;
		/** Type of object at this tile
		 * 0: Nothing
		 * 1: Wall
		 * 2: Guard
		 * 3: Player Character
		 * 4: Laser
		 * 5: Box
		 * 6: Open Door
		 * 7: Closed Door
         * 8: Switch*/
		public int occupant = 0;
		/** Is this a goal tile (used for pathfinding)? */
		public boolean goal = false;
		/** Has this tile been visited (used for pathfinding)? */
		public boolean visited = false;
	}

	
	// Constants
	/** Color of a regular tile */
	private static final Color BASIC_COLOR = new Color(0.25f, 0.25f, 0.25f, 0.5f);
	/** Highlight color for power tiles */
	private static final Color POWER_COLOR = new Color( 0.0f,  1.0f,  1.0f, 0.5f);

	// Instance attributes
	/** The dimensions of a single tile */
	private int   TILE_WIDTH = 2; // MUST BE 2X VALUE IN GAMECANVAS
	/** The board width (in number of tiles) */
	private int width;
	/** The board height (in number of tiles) */
	private int height;
	/** The tile grid (with above dimensions) */
	private TileState[] tiles;
	/** Texture of valid tile */
	private TextureRegion tileTexture;

	/**
	 * Creates a new board of the given size
	 *
	 * @param width Board width in tiles
	 * @param height Board height in tiles
	 */
	public Board(int width, int height, int tileWidth) {
		this.width = width;
		this.height = height;
		this.TILE_WIDTH = tileWidth;
		tiles = new TileState[width * height];
		for (int ii = 0; ii < tiles.length; ii++) {
			tiles[ii] = new TileState();
		}
		resetTiles();
	}

	public int getOccupantAt(int x, int y){
		return getTileState(x,y).occupant;
	}

	/**
	 * Sets invalid tiles.
	 */
	public void setInvalidTiles(int[] invalidTiles) {
		for(int i=0;i<invalidTiles.length;i+=2){
			getTileState(invalidTiles[i],invalidTiles[i+1]).isValid = false;
		}
	}

	/**
	 * Sets occupant of tiles
	 */
	public void setOccupiedTiles(int x, int y, int oType) {
		if (isSafeAt(x,y)) {
			getTileState(x, y).isOccupied = true;
			getTileState(x, y).occupant = oType;
		}
	}
	
	/**
	 * Resets the values of all the tiles on screen.
	 */
	public void resetTiles() {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				TileState tile = getTileState(x, y);
//				tile.isValid = true;
				tile.goal = false;
				tile.visited = false;
//				tile.isOccupied = false;
//				tile.occupant = 0;
			}
		}
	}

	public void resetOccupants(){
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				TileState tile = getTileState(x, y);
				tile.isOccupied = false;
				tile.occupant = 0;
			}
		}
	}
	
	/**
	 * Returns the tile state for the given position (INTERNAL USE ONLY)
	 *
	 * Returns null if that position is out of bounds.
	 *
	 * @return the tile state for the given position 
	 */
	private TileState getTileState(int x, int y) {
		if (!inBounds(x, y)) {
			return null;
		}
		return tiles[x * height + y];
	}

	/** 
	 * Returns the number of tiles horizontally across the board.
	 *
	 * @return the number of tiles horizontally across the board.
	 */
	public int getWidth() {
		return width;
	}

	/** 
	 * Returns the number of tiles vertically across the board.
	 *
	 * @return the number of tiles vertically across the board.
	 */
	public int getHeight() {
		return height;
	}

	/**
	 * Returns the size of the tile texture.
	 *
	 * @return the size of the tile texture.
	 */
	public int getTileSize() {
		return 1;
	}

	// Drawing information	
	/**
	 * Returns the textured mesh for each tile.
	 *
	 * We only need one mesh, as all tiles look (mostly) the same.
	 *
	 * @return the textured mesh for each tile.
	 */
	public TextureRegion getTileTexture() {
		return tileTexture;
	}

	/**
	 * Sets the textured mesh for each tile.
	 *
	 * We only need one mesh, as all tiles look (mostly) the same.
	 *
	 * @param texture the textured mesh for each tile.
	 */
	public void setTileTexture(TextureRegion texture) {
		tileTexture = texture;
	}


	// COORDINATE TRANSFORMS
	// The methods are used by the physics engine to coordinate the
	// Ships and Photons with the board. You should not need them.
	
	/**
	 * Returns true if a screen location is safe (i.e. there is a tile there)
	 *
	 * @param x The x value in screen coordinates
	 * @param y The y value in screen coordinates
	 *
	 * @return true if a screen location is safe
	 */
	public boolean isSafeAtScreen(float x, float y) {
		int bx = screenToBoard(x);
		int by = screenToBoard(y);
		return x >= 0 && y >= 0
				&& x < width * getTileSize()
				&& y < height * getTileSize()
				&& getTileState(bx, by).isValid;
	}

	/**
	 * Returns true if a tile location is safe (i.e. there is a tile there)
	 *
	 * @param x The x index for the Tile cell
	 * @param y The y index for the Tile cell
	 *
	 * @return true if a screen location is safe
	 */
	public boolean isSafeAt(int x, int y) {
		return x >= 0 && y >= 0 && x < width && y < height
				&& getTileState(x, y).isValid &&
				(getTileState(x,y).occupant == 0 || getTileState(x,y).occupant == 4 || getTileState(x,y).occupant == 3);
	}

	// GAME LOOP
	// This performs any updates local to the board (e.g. animation)

	/**
	 * Updates the state of all of the tiles.
	 *
	 * All we do is animate falling tiles.
	 */
	public void update() {
		for(int i=height-1;i>=0;i--){
			for(int j=0;j<width;j++){
				System.out.print(getTileState(j,i).occupant);
			}
			System.out.println();
		}
		System.out.println();
		System.out.println();
	}

	/**
	 * Draws the board to the given canvas.
	 *
	 * This method draws all of the tiles in this board. It should be the first drawing
	 * pass in the GameEngine.
	 *
	 * @param canvas the drawing context
	 */
	public void draw(ObstacleCanvas canvas) {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				drawTile(x, y, canvas);
			}
		}
	}
	
	/**
	 * Draws the individual tile at position (x,y). 
	 *
	 * Fallen tiles are not drawn.
	 *
	 * @param x The x index for the Tile cell
	 * @param y The y index for the Tile cell
	 */
	private void drawTile(int x, int y, ObstacleCanvas canvas) {
		TileState tile = getTileState(x, y);
		
		// Don't draw tile if it's fallen off the screen
		if (!tile.isValid) {
			return;
		}

		// Compute drawing coordinates
		float sx = boardToScreen(x);
		float sy = boardToScreen(y);

		// Draw
		//This one does the same thing as the line below
		//canvas.draw(tileTexture, Color.WHITE, tileTexture.getRegionWidth()/2, tileTexture.getRegionHeight()/2,
		//		TILE_WIDTH * (x + 0.5f), TILE_WIDTH * (y + 0.5f), 0, 1.0f, 1.0f);

		canvas.draw(tileTexture, Color.WHITE, 0, 0,
				TILE_WIDTH * x, TILE_WIDTH * y, 0, TILE_WIDTH/tileTexture.getRegionWidth(), TILE_WIDTH/tileTexture.getRegionHeight());
	}

	// CONVERSION METHODS (OPTIONAL)
	// Use these methods to convert between tile coordinates (int) and
	// world coordinates (float).

	/**
	 * Returns the board cell index for a screen position.
	 *
	 * While all positions are 2-dimensional, the dimensions to
	 * the board are symmetric. This allows us to use the same
	 * method to convert an x coordinate or a y coordinate to
	 * a cell index.
	 *
	 * @param f Screen position coordinate
	 *
	 * @return the board cell index for a screen position.
	 */
	public int physicsToBoard(float f) {
		return (int)(f - 0.5f);
	}

	/**
	 * Returns the board cell index for a screen position.
	 *
	 * While all positions are 2-dimensional, the dimensions to
 	 * the board are symmetric. This allows us to use the same
	 * method to convert an x coordinate or a y coordinate to
	 * a cell index.
	 *
	 * @param f Screen position coordinate
	 *
	 * @return the board cell index for a screen position.
	 */
	public int screenToBoard(float f) {
		return (int)(f / getTileSize());
	}

	public int tileRow(float y) {return (int)(y / getTileSize());}

	public int tileColumn(float x) {return (int)(x / getTileSize());}

	/**
	 * Returns the screen position coordinate for a board cell index.
	 *
	 * While all positions are 2-dimensional, the dimensions to
 	 * the board are symmetric. This allows us to use the same
	 * method to convert an x coordinate or a y coordinate to
	 * a cell index.
	 *
	 * @param n Tile cell index
	 *
	 * @return the screen position coordinate for a board cell index.
	 */
	public float boardToPhysics(int n) {
		return (float) (n + 0.5f);
	}

	/**
	 * Returns the screen position coordinate for a board cell index.
	 *
	 * While all positions are 2-dimensional, the dimensions to
	 * the board are symmetric. This allows us to use the same
	 * method to convert an x coordinate or a y coordinate to
	 * a cell index.
	 *
	 * @param n Tile cell index
	 *
	 * @return the screen position coordinate for a board cell index.
	 */
	public float boardToScreen(int n) {
		return (float) (n + 0.5f) * getTileSize();
	}
	
	/**
	 * Returns the distance to the tile center in screen coordinates.
	 *
	 * This method is an implicit coordinate transform. It takes a position (either 
	 * x or y, as the dimensions are symmetric) in screen coordinates, and determines
	 * the distance to the nearest tile center.
	 *
	 * @param f Screen position coordinate
	 *
	 * @return the distance to the tile center
	 */
	public float centerOffset(float f) {
		float paddedTileSize = getTileSize();
		int cell = screenToBoard(f);
		float nearestCenter = (cell + 0.5f) * paddedTileSize;
		return f - nearestCenter;
	}

	// PATHFINDING METHODS (REQUIRED)	
	// Use these methods to implement pathfinding on the board.
	
	/**
	 * Returns true if the given position is a valid tile
	 *
	 * It does not check whether the tile is live or not.  Dead tiles are still valid.
	 *
	 * @param x The x index for the Tile cell
	 * @param y The y index for the Tile cell
	 *
	 * @return true if the given position is a valid tile
	 */
	public boolean inBounds(int x, int y) {
		return x >= 0 && y >= 0 && x < width && y < height;
	}

	/**
	 * Returns true if the tile has been visited.
	 *
	 * A tile position that is not on the board will always evaluate to false.
	 *
	 * @param x The x index for the Tile cell
	 * @param y The y index for the Tile cell
	 *
	 * @return true if the tile has been visited.
	 */
	public boolean isVisited(int x, int y) {
		if (!inBounds(x, y)) {
			return false;
		}

		return getTileState(x, y).visited;
	}
	
	/**
	 * Marks a tile as visited.
	 *
	 * A marked tile will return true for isVisited(), until a call to clearMarks().
	 *
	 * @param x The x index for the Tile cell
	 * @param y The y index for the Tile cell
	 */
	public void setVisited(int x, int y) {
		if (!inBounds(x,y)) {
			Gdx.app.error("Board", "Illegal tile "+x+","+y, new IndexOutOfBoundsException());
			return;
		}
		getTileState(x, y).visited = true;
	}

	/**
	Returns the occupant of the tile
	 */
	public int getOccupant(int x, int y) {
		TileState tile = getTileState(x,y);
		return tile.occupant;
	}

	/**
	 * Returns true if the tile is a goal.
	 *
	 * A tile position that is not on the board will always evaluate to false.
	 *
	 * @param x The x index for the Tile cell
	 * @param y The y index for the Tile cell
	 *
	 * @return true if the tile is a goal.
	 */
	public boolean isGoal(int x, int y) {
		if (!inBounds(x, y)) {
			return false;
		}

		return getTileState(x, y).goal;
	}

	/**
	 * Marks a tile as a goal.
	 *
	 * A marked tile will return true for isGoal(), until a call to clearMarks().
	 *
	 * @param x The x index for the Tile cell
	 * @param y The y index for the Tile cell
	 */
	public void setGoal(int x, int y) {
		if (!inBounds(x,y)) {
			Gdx.app.error("Board", "Illegal tile "+x+","+y, new IndexOutOfBoundsException());
			return;
		}
		getTileState(x, y).goal = true;
	}

	/**
	 * Clears all marks on the board.
	 *
	 * This method should be done at the beginning of any pathfinding round.
	 */
	public void clearMarks() {
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				TileState state = getTileState(x, y);
				state.visited = false;
				state.goal = false;
			}
		}
	}
}