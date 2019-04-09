/*
 * DudeModel.java
 *
 * This is a refactored version of DudeModel that allows us to read its properties
 * from a JSON file.  As a result, it has a lot more getter and setter "hooks" than
 * in lab.
 *
 * While the dude can support lights, these are completely decoupled from this object.
 * The dude is not aware of any of the lights. These are attached to the associated
 * body and move with the body.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * B2Lights version, 3/12/2016
 */
package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import java.lang.reflect.*;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.physics.obstacle.*;

/**
 * Player avatar for the plaform game.
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.
 */
public class DudeModel extends CharacterModel {

	/** Default Height of Player */
	public static final float DEFAULT_HEIGHT = 0.3f;
	/** Default Width of Player */
	public static final float DEFAULT_WIDTH = 1;
	/** Collide Bit */
	public static final String COLLIDE_BIT = "1111";
	/** Default Width of Player */
	public static final String EXCLUDE_BIT = "0000";
	/** Default Density of Player */
	public static final float DEFAULT_DENSITY = 1.5f;
	/** Default Force of BOXDUDE */
	public static final float BOXDUDE_FORCE = 200;
	/** Default Force of Player */
	public static final float DEFAULT_FORCE = 250;
	/** Default Damping of Player */
	public static final float DEFAULT_DAMPING = 10;
	/** Default Damping of Player */
	public static final float DEFAULT_MAXSPEED = 1;


	// Physics constants
	/** The factor to multiply by the input */
	private float force;
	/** The amount to slow the character down */
	private float damping; 
	/** The maximum character speed */
	private float maxspeed;

	// Box interactions
	/** Whether player has a box */
	private boolean hasBox;
	private Obstacle boxContact;
	private Obstacle boxHeld;
	private Obstacle lastBoxHeld;

	// Character states
	/** Whether player is alive */
	private boolean isAlive;
	
	/** The current horizontal movement of the character */
	private Vector2 movement = new Vector2();
	/** Whether or not to animate the current frame */
	private boolean animate = false;
	
	/** How many frames until we can walk again */
	private int walkCool;
	/** The standard number of frames to wait until we can walk again */
	private int walkLimit;

	/** Texture of character */
	private TextureRegion defaultCharTexture;
	/** Texture of character with box */
	private TextureRegion boxCharTexture;
	private TextureRegion shadowTexture;

	/** FilmStrip pointer to the dude animation */
	private FilmStrip dudeanimation;
	/** FilmStrip pointer to the box dude animation */
	private FilmStrip boxdudeanimation;

	/** FilmStrip pointer to the texture region */
	private FilmStrip filmstrip;
	/** The current animation frame of the avatar */
	private int startFrame;
	
	/** Cache for internal force calculations */
	private Vector2 forceCache = new Vector2();

	/** Direction of character */
	private float direction;

	private float scale = 1.4f;

	/**
	 * Returns the directional movement of this character.
	 * 
	 * This is the result of input times dude force.
	 *
	 * @return the directional movement of this character.
	 */
	public Vector2 getMovement() {
		return movement;
	}
	
	/**
	 * Sets the directional movement of this character.
	 * 
	 * This is the result of input times dude force.
	 *
	 * @param value the directional movement of this character.
	 */
	public void setMovement(Vector2 value) {
		setMovement(value.x,value.y);
	}

	/**
	 * Sets the directional movement of this character.
	 * 
	 * This is the result of input times dude force.
	 *
	 * @param dx the horizontal movement of this character.
	 * @param dy the horizontal movement of this character.
	 */
	public void setMovement(float dx, float dy) {
		movement.set(dx,dy); 
	}

	public float getDirection(){ return direction; }

	public void setDirection(float dir){ direction = dir; }

	/**
	 * Returns how much force to apply to get the dude moving
	 *
	 * Multiply this by the input to get the movement value.
	 *
	 * @return how much force to apply to get the dude moving
	 */
	public float getForce() {
		return force;
	}
	
	/**
	 * Sets how much force to apply to get the dude moving
	 *
	 * Multiply this by the input to get the movement value.
	 *
	 * @param value	how much force to apply to get the dude moving
	 */
	public void setForce(float value) {
		force = value;
	}

	/**
	 * Returns how hard the brakes are applied to get a dude to stop moving
	 *
	 * @return how hard the brakes are applied to get a dude to stop moving
	 */
	public float getDamping() {
		return damping;
	}
	
	/**
	 * Sets how hard the brakes are applied to get a dude to stop moving
	 *
	 * @param value	how hard the brakes are applied to get a dude to stop moving
	 */
	public void setDamping(float value) {
		damping = value;
	}
	
	/**
	 * Returns the upper limit on dude left-right movement.  
	 *
	 * This does NOT apply to vertical movement.
	 *
	 * @return the upper limit on dude left-right movement.  
	 */
	public float getMaxSpeed() {
		return maxspeed;
	}
	
	/**
	 * Sets the upper limit on dude left-right movement.  
	 *
	 * This does NOT apply to vertical movement.
	 *
	 * @param value	the upper limit on dude left-right movement.  
	 */
	public void setMaxSpeed(float value) {
		maxspeed = value;
	}
	
	/**
	 * Returns the current animation frame of this dude.  
	 *
	 * @return the current animation frame of this dude. 
	 */
	public float getStartFrame() {
		return startFrame;
	}
	
	/**
	 * Sets the animation frame of this dude.  
	 *
	 * @param value	animation frame of this dude.  
	 */
	public void setStartFrame(int value) {
		startFrame = value;
	}
	
	/**
	 * Returns the cooldown limit between walk animations
	 *
	 * @return the cooldown limit between walk animations  
	 */
	public int getWalkLimit() {
		return walkLimit;
	}

	/**
	 * Sets the cooldown limit between walk animations
	 *
	 * @param value	the cooldown limit between walk animations  
	 */
	public void setWalkLimit(int value) {
		walkLimit = value;
	}

	/**
	 * Returns whether player has a box
	 *
	 * @return true if player has a box
	 */
	public boolean getHasBox(){
		return hasBox;
	}

	/**
	 * Returns whether player is alive
	 *
	 * @return true if player is alive
	 */
	public boolean getIsAlive(){
		return isAlive;
	}

	/**
	 * Sets whether player is alive
	 *
	 * @param isAlive true if player is alive
	 */
	public void setIsAlive(boolean isAlive){
		this.isAlive = isAlive;
	}
	
	/**
	 * Creates a new dude with degenerate settings
	 *
	 * The main purpose of this constructor is to set the initial capsule orientation.
	 */
	public DudeModel() {
		super(1,1,1,1);
		setFixedRotation(false);
	}

	/**
	 * Creates a new dude with degenerate settings
	 *
	 * The main purpose of this constructor is to set the initial capsule orientation.
	 */
	public DudeModel(float x, float y,float width, float height) {
		super(x,y,width, height);
		setFixedRotation(false);
	}

	/**
	 * Pick up a box if the player doesn't have a box right now
	 *
	 * @return true if player picked up a box, false if not
	 */
	public boolean pickupBox(){
		if (hasBox)
			return false;
		hasBox = true;
		//change sprite
		filmstrip = boxdudeanimation;
		setTexture(filmstrip);
		setOrigin(origin.x,0);
		setForce(BOXDUDE_FORCE);
//		setBoxHeld(box);
		return true;
	}

	/**
	 * Drop a box if the player is holding a box
	 *
	 * @return true if player dropped a box, false if not
	 */
	public boolean dropBox(){
		if (!hasBox)
			return false;
		hasBox = false;
		//change sprite
		filmstrip = dudeanimation;
		setTexture(filmstrip);
		setOrigin(origin.x,0);
		setForce(DEFAULT_FORCE);
		lastBoxHeld = boxHeld;
		boxHeld = null;
		return true;
	}

	public void setBoxInContact(Obstacle box) {
		boxContact = box;
	}

	public Obstacle getBoxInContact() {
		return boxContact;
	}

	public Obstacle getLastBoxHeld() {
		return lastBoxHeld;
	}

	public void setBoxHeld(Obstacle box) {
		if (!hasBox) {
			boxHeld = box;
		}
	}

	public Obstacle getBoxHeld() { return boxHeld; }

	/**
	 * Initializes the dude via the given JSON value
	 *
	 * The JSON value has been parsed and is part of a bigger level file.  However, 
	 * this JSON value is limited to the dude subtree
	 *
	 * @param json	the JSON subtree defining the dude
	 */
	public void initialize(JsonValue json) {
		setName(json.name());
		float[] pos = json.get("pos").asFloatArray();
		setPosition(pos[0]+0.5f,pos[1]+0.5f);
		setWidth(DEFAULT_WIDTH*1.25f);
		setHeight(DEFAULT_HEIGHT*1.25f);
		setFixedRotation(true);
		
		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		setBodyType(BodyDef.BodyType.DynamicBody);
		setDensity(DEFAULT_DENSITY);
		setFriction(0);
		setRestitution(0);
		setForce(DEFAULT_FORCE);
		setDamping(DEFAULT_DAMPING);
		setMaxSpeed(DEFAULT_MAXSPEED);

		//Animation
		setStartFrame(0);
		setWalkLimit(8);
		
		// Create the collision filter (used for light penetration)
      	short collideBits = LevelModel.bitStringToShort(COLLIDE_BIT);
      	short excludeBits = LevelModel.bitStringToComplement(EXCLUDE_BIT);
      	Filter filter = new Filter();
      	filter.categoryBits = collideBits;
      	filter.maskBits = excludeBits;
      	setFilterData(filter);
      	
		// Reflection is best way to convert name to color
		Color debugColor;
		try {
		    Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField("GREEN");
		    debugColor = new Color((Color)field.get(null));
		} catch (Exception e) {
			debugColor = null; // Not defined
		}
		int opacity = 192;
		debugColor.mul(opacity/255.0f);
		setDebugColor(debugColor);


		// Now get the texture from the AssetManager singleton
		TextureRegion texture = JsonAssetManager.getInstance().getEntry("defaultDude", TextureRegion.class);
		defaultCharTexture = texture;
		texture.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
//		setTexture(texture);
//		setOrigin(origin.x,0);

		texture = JsonAssetManager.getInstance().getEntry("boxDude", TextureRegion.class);
		boxCharTexture = texture;

		texture = JsonAssetManager.getInstance().getEntry("shadow", TextureRegion.class);
		shadowTexture = texture;

		texture = JsonAssetManager.getInstance().getEntry("boxdude", TextureRegion.class);
		try {
			filmstrip = (FilmStrip)texture;
			boxdudeanimation = filmstrip;
		} catch (Exception e) {
			filmstrip = null;
		}

		texture = JsonAssetManager.getInstance().getEntry("dude", TextureRegion.class);
		try {
			filmstrip = (FilmStrip)texture;
			dudeanimation = filmstrip;
		} catch (Exception e) {
			filmstrip = null;
		}
		setTexture(texture);
		setOrigin(origin.x,0);

	}

	/**
	 * Applies the force to the body of this dude
	 *
	 * This method should be called after the force attribute is set.
	 */
	public void applyForce() {
		if (!isActive()) {
			return;
		}
		
		// Only walk or spin if we allow it
		setLinearVelocity(Vector2.Zero);
		setAngularVelocity(0.0f);
		
		// Apply force for movement
		if (getMovement().len2() > 0f) {
			forceCache.set(getMovement());
			if (body.getLinearVelocity().len()>maxspeed)
				body.setLinearVelocity(body.getLinearVelocity().scl(body.getLinearVelocity().len()/maxspeed));
			else
				body.applyForce(forceCache,getPosition(),true);
			animate = true;
		} else {
			animate = false;
		}
	}
	
	/**
	 * Updates the object's physics state (NOT GAME LOGIC).
	 *
	 * We use this method to reset cooldowns.
	 *
	 * //@param delta Number of seconds since last animation frame
	 */
	public void update(float dt) {
		// Animate if necessary
		if (animate && walkCool == 0) {
			if (filmstrip != null) {
				int next = (filmstrip.getFrame()+1) % filmstrip.getSize();
				filmstrip.setFrame(next);
			}
			walkCool = walkLimit;
		} else if (walkCool > 0) {
			walkCool--;
		} else if (!animate) {
			if (filmstrip != null) {
				filmstrip.setFrame(startFrame);
			}
			walkCool = 0;
		}
		
		super.update(dt);
	}

	/**
	 * Draws the physics object.
	 *
	 * @param canvas Drawing context
	 */
	public void draw(ObstacleCanvas canvas) {
		if (texture != null) {
			canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/2f*drawScale.y,getAngle(),scale,scale);
		}
		canvas.draw(shadowTexture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x+4,getY()*drawScale.y-getHeight()/2f*drawScale.y,getAngle(),1.5f,1.5f);
	}
}