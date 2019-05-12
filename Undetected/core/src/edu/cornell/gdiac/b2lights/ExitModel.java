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

import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import java.lang.reflect.*;

import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.physics.obstacle.*;

/**
 * A sensor obstacle representing the end of the level
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.
 */
public class ExitModel extends BoxObstacle {
	/** Collide Bit */
	public static final String COLLIDE_BIT = "0010";
	/** Default Width of Player */
	public static final String EXCLUDE_BIT = "0000000000000000";
	private TextureRegion defaultExitTexture;
	private boolean animationOn = false;
	private TextureRegion circleTexture; //for minimap
	private float alpha = 1; //for drawing purposes
	private int maxframe;
	private FilmStrip filmstrip;
	private FilmStrip arrowFilmstrip;
	private int arrowCoolTime = 8;
	/** 0:x-axis  */
	private float direction;

	/**
	 * Create a new ExitModel with degenerate settings
	 */	
	public ExitModel() {
		super(0,0,1,1);
		setSensor(false);
	}

	public void setExitDirection(float dir){
		direction = (Math.round(dir*100))/100f;
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
		float[] pos  = json.get("pos").asFloatArray();
		float[] size = json.get("size").asFloatArray();
		setPosition(pos[0]+0.5f*(size[0]%2),pos[1]+0.5f*(size[1]%2));
		setDimension(size[0],size[1]);
		
		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		setBodyType(BodyDef.BodyType.StaticBody );
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
//			String cname = json.get("debugcolor").asString().toUpperCase();
		    Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField("YELLOW");
		    debugColor = new Color((Color)field.get(null));
		} catch (Exception e) {
			debugColor = null; // Not defined
		}
		int opacity = 200;
		debugColor.mul(opacity/255.0f);
		setDebugColor(debugColor);
		
		// Now get the texture from the AssetManager singleton
//		String key = json.get("texture").asString();
		TextureRegion texture = JsonAssetManager.getInstance().getEntry("goal", TextureRegion.class);
		defaultExitTexture = texture;
		setTexture(texture);
		setOrigin(origin.x,0);

		texture = JsonAssetManager.getInstance().getEntry("exitIndicator", TextureRegion.class);
		circleTexture = texture;

		texture = JsonAssetManager.getInstance().getEntry("bluedoor", TextureRegion.class);
		maxframe = 13;
		try {
			filmstrip = new FilmStrip(texture.getTexture(), 1, maxframe);
		} catch (Exception e) {
			filmstrip = null;
		}
		filmstrip.setFrame(0);
		defaultExitTexture = filmstrip;

		texture = JsonAssetManager.getInstance().getEntry("exitArrows", TextureRegion.class);
		try {
			arrowFilmstrip = new FilmStrip(texture.getTexture(), 1, 4);
		} catch (Exception e) {
			arrowFilmstrip = null;
		}
		arrowFilmstrip.setFrame(0);

		setTexture(filmstrip);
		setOrigin(origin.x,0);
	}

	public boolean isAnimating(){return animationOn;}

	public void open() {
		animationOn = true;
		setSensor(true);

	}

	public void animate(float dt){
		if(animationOn) {
			alpha -= dt * 0.6;
			if (alpha < 0) {
				animationOn = false;
			}
		}
	}
	public void drawMiniMap(ObstacleCanvas canvas, float alpha){
		setTexture(circleTexture);
		setOrigin(origin.x,0);
		canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),1.0f,1.0f, alpha);
		setTexture(defaultExitTexture);
		setOrigin(origin.x,0);

	}

	@Override
	public void update(float delta) {
		super.update(delta);
		if (arrowCoolTime!=0){
			arrowCoolTime--;
			return;
		}
		arrowFilmstrip.setFrame((arrowFilmstrip.getFrame()+1)%4);
		arrowCoolTime=9;
	}

	/**
	 * Draws the physics object.
	 *
	 * @param canvas Drawing context
	 */
	public void draw(ObstacleCanvas canvas) {
		if (texture != null) {
			canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),1.0f,1.0f, alpha);
		}
		if(direction==3.14f) {
			canvas.draw(arrowFilmstrip, Color.WHITE, arrowFilmstrip.getRegionWidth() / 2, 0, getX() * drawScale.x, getY() * drawScale.y - getHeight() / 2 * drawScale.y - 5,
					direction, 1.0f, 1.0f, 1);
		}else if(direction==0){
			canvas.draw(arrowFilmstrip, Color.WHITE, arrowFilmstrip.getRegionWidth() / 2, 0, getX() * drawScale.x, getY() * drawScale.y + getHeight()*3/2 * drawScale.y + 5,
					direction, 1.0f, 1.0f, 1);
		}
	}
}
