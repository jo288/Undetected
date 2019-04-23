/*
 * ExitModel.java
 *
 * This is a refactored version of the exit door from Lab 4.  We have made it a specialized
 * class so that we can import its properties from a JSON file.  
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * B2Lights version, 3/12/2016
 */
package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.lang.reflect.Field;
import java.util.*;

/**
 * A sensor obstacle representing the end of the level
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.
 */
public class ObjectiveModel extends BoxObstacle {
	/** Collide Bit */
	public static final String COLLIDE_BIT = "0010";
	/** Default Width of Player */
	public static final String EXCLUDE_BIT = "0000000000000000";

	/** Whether the objective is active or not */
	private boolean isActive;
	/** Whether the objective triggers alarm or not */
	private boolean hasAlarm;
	private ArrayList<Laser> lasers = new ArrayList<>();
	private ArrayList<DoorModel> doors = new ArrayList<>();

	private TextureRegion defaultCardTexture;
	private TextureRegion stolenCardTexture;

	/**
	 * Create a new ObjectiveModel with degenerate settings
	 */
	public ObjectiveModel() {
		super(0,0,1,1);
	}

	/**
	 * Create a new ObjectiveModel with alarm or not
	 */
	public ObjectiveModel(boolean hasAlarm) {
		super(0,0,1,1);
		this.hasAlarm = hasAlarm;
	}

	public boolean stealCard(){
		//change sprite
		setTexture(stolenCardTexture);
		setOrigin(origin.x,0);
//		setBoxHeld(box);
		alarmMode();
		return true;
	}

	public void addLaser(Laser laser) {
		lasers.add(laser);
	}

	public void addDoor(DoorModel door) {
		doors.add(door);
	}

	public ArrayList<Laser> getLasers() {
		return lasers;
	}

	public ArrayList<DoorModel> getDoors() {
		return doors;
	}

	public void alarmMode() {
		if (doors != null) {
			for (DoorModel door : doors) {
				door.switchState();
			}
		}
		if (lasers != null) {
			for (Laser las : lasers) {
				las.setOn(true);
			}
		}
	}

	public boolean getIsActive(){
		return isActive;
	}

	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	
	/**
	 * Initializes the exit door via the given JSON value
	 *
	 * The JSON value has been parsed and is part of a bigger level file.  However, 
	 * this JSON value is limited to the exit subtree
	 *
	 * @param json	the JSON subtree defining the dude
	 */
	public void initialize(JsonValue json) {
		setName(json.name());
		hasAlarm = json.get("hasAlarm").asBoolean();
		float[] pos  = json.get("pos").asFloatArray();
		float[] size = json.get("size").asFloatArray();
		setPosition(pos[0]+0.5f*(size[0]%2),pos[1]+0.5f*(size[1]%2));
		setDimension(size[0],size[1]);
		
		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		setBodyType(BodyDef.BodyType.StaticBody);
		setDensity(0);
		setFriction(0);
		setRestitution(0);
		
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
		    Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField("YELLOW");
		    debugColor = new Color((Color)field.get(null));
		} catch (Exception e) {
			debugColor = null; // Not defined
		}
		int opacity = 200;
		debugColor.mul(opacity/255.0f);
		setDebugColor(debugColor);

		TextureRegion texture = JsonAssetManager.getInstance().getEntry("stealcard", TextureRegion.class);
		defaultCardTexture = texture;
		texture.getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		setTexture(texture);
		setOrigin(origin.x,0);

		texture = JsonAssetManager.getInstance().getEntry("stolencard", TextureRegion.class);
		stolenCardTexture = texture;
		
		// Now get the texture from the AssetManager singleton
//		String key = json.get("texture").asString();
//		TextureRegion texture = JsonAssetManager.getInstance().getEntry(key, TextureRegion.class);
//		setTexture(texture);
//		setOrigin(origin.x,0);
	}
	/**
	 * Draws the physics object.
	 *
	 * @param canvas Drawing context
	 */
	public void draw(ObstacleCanvas canvas) {
		if (texture != null) {
			canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),1.0f,1.0f);
		}
	}
}
