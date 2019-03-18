/*
 * GameController.java
 *
 * This combines the WorldController with the mini-game specific PlatformController
 * in the last lab.  With that said, some of the work is now offloaded to the new
 * LevelModel class, which allows us to serialize and deserialize a level. 
 * 
 * This is a refactored version of WorldController from Lab 4.  It separate the 
 * level out into a new class called LevelModel.  This model is, in turn, read
 * from a JSON file.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * B2Lights version, 3/12/2016
 */
package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.util.*;

import edu.cornell.gdiac.physics.obstacle.*;

import java.security.Guard;
import java.util.ArrayList;

/**
 * Gameplay controller for the game.
 *
 * This class does not have the Box2d world.  That is stored inside of the
 * LevelModel object, as the world settings are determined by the JSON
 * file.  However, the class does have all of the controller functionality,
 * including collision listeners for the active level.
 *
 * You will notice that asset loading is very different.  It relies on the
 * singleton asset manager to manage the various assets.
 */
public class GameController implements Screen, ContactListener {
	/** 
	 * Tracks the asset state.  Otherwise subclasses will try to load assets 
	 */
	protected enum AssetState {
		/** No assets loaded */
		EMPTY,
		/** Still loading assets */
		LOADING,
		/** Assets are complete */
		COMPLETE
	}

	private LightController lightController;
	
	/** The reader to process JSON files */
	private JsonReader jsonReader;
	 /** The JSON asset directory */
	private JsonValue  assetDirectory;
	/** The JSON defining the level model */
	private JsonValue  levelFormat;

	/** The font for giving messages to the player */
	protected BitmapFont displayFont;
	
	/** Track asset loading from all instances and subclasses */
	private AssetState assetState = AssetState.EMPTY;
	
	/**
	 * Preloads the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time.  However, we still want the assets themselves to be static.  So
	 * we have an AssetState that determines the current loading state.  If the
	 * assets are already loaded, this method will do nothing.
	 * 
	 * //@param manager Reference to global asset manager.
	 */	
	public void preLoadContent() {
		if (assetState != AssetState.EMPTY) {
			return;
		}
		
		assetState = AssetState.LOADING;

		jsonReader = new JsonReader();
		assetDirectory = jsonReader.parse(Gdx.files.internal("jsons/assets.json"));

		JsonAssetManager.getInstance().loadDirectory(assetDirectory);
	}

	/**
	 * Load the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time.  However, we still want the assets themselves to be static.  So
	 * we have an AssetState that determines the current loading state.  If the
	 * assets are already loaded, this method will do nothing.
	 * 
	 * //@param manager Reference to global asset manager.
	 */
	public void loadContent() {
		if (assetState != AssetState.LOADING) {
			return;
		}
		
		JsonAssetManager.getInstance().allocateDirectory();
		displayFont = JsonAssetManager.getInstance().getEntry("display", BitmapFont.class);
		assetState = AssetState.COMPLETE;
	}
	

	/** 
	 * Unloads the assets for this game.
	 * 
	 * This method erases the static variables.  It also deletes the associated textures 
	 * from the asset manager. If no assets are loaded, this method does nothing.
	 * 
	 * //@param manager Reference to global asset manager.
	 */
	public void unloadContent() {
		JsonAssetManager.getInstance().unloadDirectory();
		JsonAssetManager.clearInstance();
	}

	
	/** Exit code for quitting the game */
	public static final int EXIT_QUIT = 0;
    /** How many frames after winning/losing do we continue? */
	public static final int EXIT_COUNT = 120;

	/** Reference to the game canvas */
	protected ObstacleCanvas canvas;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;

	/** Reference to the game level */
	protected LevelModel level;
		
	/** Whether or not this is an active controller */
	private boolean active;
	/** Whether we have completed this level */
	private boolean complete;
	/** Whether we have failed at this world (and need a reset) */
	private boolean failed;
	/** Countdown active for winning or losing */
	private int countdown;
	/** Whether the player reached the objective or not */
	private boolean hasObjective;
	private boolean setAngle = false;


	/** Mark set to handle more sophisticated collision callbacks */
	protected ObjectSet<Fixture> sensorFixtures;
	/** Whether the player collided with a box */
	protected boolean avatarBoxCollision;
	/** Whether the player collided with a laser */
	protected boolean avatarLaserCollision;

	/**
	 * Returns true if the level is completed.
	 *
	 * If true, the level will advance after a countdown
	 *
	 * @return true if the level is completed.
	 */
	public boolean isComplete( ) {
		return complete;
	}

	/**
	 * Sets whether the level is completed.
	 *
	 * If true, the level will advance after a countdown
	 *
	 * @param value whether the level is completed.
	 */
	public void setComplete(boolean value) {
		if (value) {
			countdown = EXIT_COUNT;
		}
		complete = value;
	}

	/**
	 * Returns true if the level is failed.
	 *
	 * If true, the level will reset after a countdown
	 *
	 * @return true if the level is failed.
	 */
	public boolean isFailure( ) {
		return failed;
	}

	/**
	 * Sets whether the level is failed.
	 *
	 * If true, the level will reset after a countdown
	 *
	 * @param value whether the level is failed.
	 */
	public void setFailure(boolean value) {
		if (value) {
			countdown = EXIT_COUNT;
		}
		failed = value;
	}
	
	/**
	 * Returns true if this is the active screen
	 *
	 * @return true if this is the active screen
	 */
	public boolean isActive( ) {
		return active;
	}

	/**
	 * Returns the canvas associated with this controller
	 *
	 * The canvas is shared across all controllers
	 *
	 * //@param the canvas associated with this controller
	 */
	public ObstacleCanvas getCanvas() {
		return canvas;
	}
	
	/**
	 * Sets the canvas associated with this controller
	 *
	 * The canvas is shared across all controllers.  Setting this value will compute
	 * the drawing scale from the canvas size.
	 *
	 * //@param value the canvas associated with this controller
	 */
	public void setCanvas(ObstacleCanvas canvas) {
		this.canvas = canvas;
	}
	
	/**
	 * Creates a new game world 
	 *
	 * The physics bounds and drawing scale are now stored in the LevelModel and
	 * defined by the appropriate JSON file.
	 */
	public GameController() {
		jsonReader = new JsonReader();
		level = new LevelModel();
		lightController = new LightController(level);
		complete = false;
		failed = false;
		active = false;
		countdown = -1;
		avatarBoxCollision = false;
		avatarLaserCollision = false;
		hasObjective = false;

		setComplete(false);
		setFailure(false);
		sensorFixtures = new ObjectSet<Fixture>();
	}
	
	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
	public void dispose() {
		level.dispose();
		level  = null;
		canvas = null;
	}
	
	/**
	 * Resets the status of the game so that we can play again.
	 *
	 * This method disposes of the level and creates a new one. It will 
	 * reread from the JSON file, allowing us to make changes on the fly.
	 */
	public void reset() {
		level.dispose();
		
		setComplete(false);
		setFailure(false);
		hasObjective = false;
		countdown = -1;
		
		// Reload the json each time
		levelFormat = jsonReader.parse(Gdx.files.internal("jsons/level1.json"));
		level.populate(levelFormat);
		level.getWorld().setContactListener(this);

		setAngle = false;
	}
	
	/**
	 * Returns whether to process the update loop
	 *
	 * At the start of the update loop, we check if it is time
	 * to switch to a new game mode.  If not, the update proceeds
	 * normally.
	 *
	 * //@param delta Number of seconds since last animation frame
	 * 
	 * @return whether to process the update loop
	 */
	public boolean preUpdate(float dt) {
		InputController input = InputController.getInstance();
		input.readInput();
		if (listener == null) {
			return true;
		}

		// Toggle debug
		if (input.didDebug()) {
			level.setDebug(!level.getDebug());
		}
		//the camera zoom and translations are not working properly ...
		/*if(input.zoomIn()){
			canvas.getCamera().zoom-=0.01;
		}
		else if(input.zoomOut()){
			canvas.getCamera().zoom+=0.01;
		}*/

		GuardModel guard = level.getGuards().get(0);
		float degree = guard.getLight().getConeDegree();
		if(input.increaseView()){
			guard.getLight().setConeDegree(degree+0.5f);
		}
		else if(input.decreaseView()){
			guard.getLight().setConeDegree(degree-0.5f);
		}
		
		// Handle resets
		if (input.didReset()) {
			reset();
		}
		
		// Now it is time to maybe switch screens.
		if (input.didExit()) {
			listener.exitScreen(this, EXIT_QUIT);
			return false;
		} else if (countdown > 0) {
			countdown--;
		} else if (countdown == 0) {
			reset();
		}
		
		return true;
	}
	
	private Vector2 angleCache = new Vector2();
	/**
	 * The core gameplay loop of this world.
	 *
	 * This method contains the specific update code for this mini-game. It does
	 * not handle collisions, as those are managed by the parent class WorldController.
	 * This method is called after input is read, but before collisions are resolved.
	 * The very last thing that it should do is apply forces to the appropriate objects.
	 *
	 * //@param delta Number of seconds since last animation frame
	 */
	public void update(float dt) {
		// Process actions in object model
		DudeModel avatar = level.getAvatar();
		ArrayList<GuardModel> guards = level.getGuards();
		InputController input = InputController.getInstance();

		// LASER CHECK

		/*if (input.didForward()) {
			level.activateNextLight();
		} else if (input.didBack()){
			level.activatePrevLight();
		}*/

		if(input.didAction()&&avatar.getHasBox()){
			level.placeBox(avatar);
		} else if(input.didAction()&&!avatar.getHasBox() && avatarBoxCollision){
			avatar.setBoxHeld(avatar.getBoxInContact());
			avatar.pickupBox();
			level.queueDisabled(avatar.getBoxInContact());
		}
		//camera follow player
		//canvas.getCamera().translate(input.getHorizontal(), input.getVertical());
		int cx = (int)canvas.getCamera().position.x;
		int cy = (int)canvas.getCamera().position.y;
		//level.rayhandler.useCustomViewport(cx,cy,800,600);
		//level.raycamera.translate(input.getHorizontal(), input.getVertical());
		//level.raycamera.update();
		// Rotate the avatar to face the direction of movement
		angleCache.set(input.getHorizontal(),input.getVertical());
		if (angleCache.len2() > 0.0f) {
			float angle = angleCache.angle();
			// Convert to radians with up as 0
			angle = (float)Math.PI*(angle-90.0f)/180.0f;
//			avatar.setAngle(angle);
			avatar.setDirection(angle);
		}
		angleCache.scl(avatar.getForce());
		avatar.setMovement(angleCache.x,angleCache.y);
		avatar.applyForce();

		//only used if we are manually controlling one guard for demo purposes
		GuardModel guard = guards.get(0);
		Vector2 guardAngle = new Vector2(input.getHorizontalG(),input.getVerticalG());
		if (guardAngle.len2() > 0.0f) {
			float angle = guardAngle.angle();
			// Convert to radians with up as 0
			angle = (float)Math.PI*(angle-90.0f)/180.0f;
			guard.setDirection(angle);
		}
		guardAngle.scl(guard.getForce());
		guard.setMovement(guardAngle.x,guardAngle.y);
		guard.applyForce();

		if (setAngle) {
		    float refAngle = (float) (Math.PI/2 + Math.atan((guard.getY()-avatar.getY())/(guard.getX()-avatar.getX())));
		    guard.setBodyType(BodyDef.BodyType.StaticBody);
		    if ((guard.getY()-avatar.getY() >= 0) && (guard.getX()-avatar.getX() >= 0)) {
		        guard.setDirection(refAngle);
            }
		    else if ((guard.getY()-avatar.getY() >= 0) && (guard.getX()-avatar.getX() <= 0)) {
                guard.setDirection((float)(Math.PI + refAngle));
            }
            else if ((guard.getY()-avatar.getY() <= 0) && (guard.getX()-avatar.getX() <= 0)) {
                guard.setDirection((float)(Math.PI + refAngle));
            }
            else {
                guard.setDirection((float)(2*Math.PI + refAngle));
            }
        }

		// Turn the physics engine crank.
		level.update(dt);
		if(lightController.detect() && !failed){
			setFailure(true);
		}
	}
	
	/**
	 * Draw the physics objects to the canvas
	 *
	 * For simple worlds, this method is enough by itself.  It will need
	 * to be overriden if the world needs fancy backgrounds or the like.
	 *
	 * The method draws all objects in the order that they were added.
	 *
	 * //@param canvas The drawing context
	 */
	public void draw(float delta) {
		canvas.clear();
		
		level.draw(canvas);

		// Final message
		if (complete && !failed) {
			displayFont.setColor(Color.YELLOW);
			canvas.begin(); // DO NOT SCALE
			canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
			canvas.end();
		} else if (failed) {
			displayFont.setColor(Color.RED);
			canvas.begin(); // DO NOT SCALE
			canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
			canvas.end();
		}
	}
	
	/**
	 * Called when the Screen is resized. 
	 *
	 * This can happen at any point during a non-paused state but will never happen 
	 * before a call to show().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		// IGNORE FOR NOW
	}

	/**
	 * Called when the Screen should render itself.
	 *
	 * We defer to the other methods update() and draw().  However, it is VERY important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta) {
		if (active) {
			if (preUpdate(delta)) {
				update(delta);
			}
			draw(delta);
		}
	}

	/**
	 * Called when the Screen is paused.
	 * 
	 * This is usually when it's not active or visible on screen. An Application is 
	 * also paused before it is destroyed.
	 */
	public void pause() {
		// TODO Auto-generated method stub
	}

	/**
	 * Called when the Screen is resumed from a paused state.
	 *
	 * This is usually when it regains focus.
	 */
	public void resume() {
		// TODO Auto-generated method stub
	}
	
	/**
	 * Called when this screen becomes the current screen for a Game.
	 */
	public void show() {
		// Useless if called in outside animation loop
		active = true;
	}

	/**
	 * Called when this screen is no longer the current screen for a Game.
	 */
	public void hide() {
		// Useless if called in outside animation loop
		active = false;
	}

	/**
	 * Sets the ScreenListener for this mode
	 *
	 * The ScreenListener will respond to requests to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}

	/**
	 * Callback method for the start of a collision
	 *
	 * This method is called when we first get a collision between two objects.  We use 
	 * this method to test if it is the "right" kind of collision.  In particular, we
	 * use it to test if we made it to the win door.
	 *
	 * @param contact The two bodies that collided
	 */
	public void beginContact(Contact contact) {
		Fixture fix1 = contact.getFixtureA();
		Fixture fix2 = contact.getFixtureB();

		Body body1 = fix1.getBody();
		Body body2 = fix2.getBody();

		Object fd1 = fix1.getUserData();
		Object fd2 = fix2.getUserData();
		
		try {
			Obstacle bd1 = (Obstacle)body1.getUserData();
			Obstacle bd2 = (Obstacle)body2.getUserData();

			DudeModel avatar = level.getAvatar();
			ExitModel door   = level.getExit();
			ArrayList<GuardModel> guards = level.getGuards();
			ObjectiveModel objective = level.getObjective();

            if((guards.contains(bd1) && bd2==avatar ) || (bd1 == avatar && guards.contains(bd2))){
                setAngle = true;
                setFailure(true);
            }

			if((bd1==avatar && bd2 instanceof MoveableBox ) || (bd1 instanceof MoveableBox && bd2==avatar)){
				avatarBoxCollision = true;
//				System.out.println("Box in contact");
				if (bd1 instanceof  MoveableBox) {
				    avatar.setBoxInContact(bd1);
                } else if (bd2 instanceof  MoveableBox) {
				    avatar.setBoxInContact(bd2);
                }
			}

			// Check for objective
			if ((bd1 == avatar && bd2 == objective) || (bd1 == objective && bd2== avatar)){
				hasObjective = true;
				level.queueDestroyed(objective);
			}

			if((bd1 instanceof MoveableBox && bd2 instanceof Laser) || (bd1 instanceof Laser && bd2 instanceof MoveableBox)){
				System.out.println("laserbox");
			}

			// Check for win condition
			if ((bd1 == avatar && bd2 == door && hasObjective ) ||
				(bd1 == door   && bd2 == avatar && hasObjective)) {
				setComplete(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** Unused ContactListener method */
	public void endContact(Contact contact) {
		Fixture fix1 = contact.getFixtureA();
		Fixture fix2 = contact.getFixtureB();

		Body body1 = fix1.getBody();
		Body body2 = fix2.getBody();

		Object fd1 = fix1.getUserData();
		Object fd2 = fix2.getUserData();

		Obstacle bd1 = (Obstacle)body1.getUserData();
		Obstacle bd2 = (Obstacle)body2.getUserData();

		DudeModel avatar = level.getAvatar();

		if((bd1 == avatar && bd2 instanceof MoveableBox) || (bd1 instanceof MoveableBox && bd2==avatar)){
			avatarBoxCollision = false;
            avatar.setBoxInContact(null);
		}
		if((bd1 == avatar && bd2 instanceof Laser) || (bd1 instanceof Laser && bd2==avatar)){
			avatarLaserCollision = false;
		}
	}

	/** Unused ContactListener method */
	public void postSolve(Contact contact, ContactImpulse impulse) {

	}
	/** Unused ContactListener method */
	public void preSolve(Contact contact, Manifold oldManifold) {
		Fixture fix1 = contact.getFixtureA();
		Fixture fix2 = contact.getFixtureB();

		Body body1 = fix1.getBody();
		Body body2 = fix2.getBody();

		Object fd1 = fix1.getUserData();
		Object fd2 = fix2.getUserData();

		Obstacle bd1 = (Obstacle)body1.getUserData();
		Obstacle bd2 = (Obstacle)body2.getUserData();
		DudeModel avatar = level.getAvatar();

		if (((bd1 == avatar && bd2 instanceof Laser) || (bd1 instanceof Laser && bd2==avatar)) ||
				((bd1 instanceof MoveableBox && bd2 instanceof Laser) ||
						(bd1 instanceof Laser && bd2 instanceof MoveableBox))){
			contact.setEnabled(false);
			if (bd1 instanceof Laser) {
				if (((Laser) bd1).isTurnedOn()) {
					avatarLaserCollision = true;
//					if(!failed){
//						setFailure(true);
//					}
					for (AIController ai : level.getControl()) {
						ai.setAlarmed();
						ai.setProtect(bd2);
					}
				}
				else{
					avatarLaserCollision =false;
				}
			}
			else{
				if (((Laser) bd2).isTurnedOn()) {
					avatarLaserCollision = true;
//					if(!failed){
//						setFailure(true);
//					}
					for (AIController ai : level.getControl()) {
						ai.setAlarmed();
						ai.setProtect(bd1);
					}
				}
				else{
					avatarLaserCollision = false;
				}
			}
		}
	}
}