/*
 * InteriorModel.java
 *
 * This is a refactored version of a platform from the JSON Demo. Now, instead of platforms,
 * they are interior walls. We want to specify them as a rectangle, but uniformly tile them 
 * with a texture.  The BoxObstacle stretches the texture to fit, it does not tile it.  
 * Therefore, we have  modified BoxObstacle to provide tile support.
 * 
 * A further change from the JSON version is the notion of "padding".  Padding is the
 * the amount that the visible sprite is larger than the physics object itself (measured
 * in physics coordinates, not Sprite coordinates). This keeps the shadow from completely 
 * obstructing the wall sprite.
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
 * A rectangle shape representing an interior wall.
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.  In addition, this class overrides the drawing and
 * positioning functions to provide a tiled texture.
 */
public class InteriorModel extends BoxObstacle {
	/** Texture information for this object */
	protected PolygonRegion region;
	/** The texture anchor upon region initialization */
	protected Vector2 anchor;
	/** The padding (in physics coordinates) to increase the sprite beyond the physics body */
	protected Vector2 padding;
	
	/**
	 * Create a new InteriorModel with degenerate settings
	 */	
	public InteriorModel() {
		super(0,0,1,1);
		region = null;
		padding = new Vector2();
	}
	
	/**
	 * Initializes a PolygonRegion to support a tiled texture
	 * 
	 * In order to keep the texture uniform, the drawing polygon position needs to be 
	 * absolute.  However, this can cause a problem when we want to move the platform 
	 * (e.g. a dynamic platform).  The purpose of the texture anchor is to ensure that 
	 * the texture does not move as the object moves.
	 *
	 * Not the use of padding to compute the texture vertices.  This makes the image 
	 * larger than the physics body, so that it is not completely obstructed by shadows.
	 */	
	private void initRegion() {
		if (texture == null) {
			return;
		}
		float[] scaled = new float[vertices.length];
		for(int ii = 0; ii < scaled.length; ii++) {
			if (ii % 2 == 0) {
				scaled[ii] = (vertices[ii]+Math.signum(vertices[ii])*padding.x+getX())*drawScale.x;
			} else {
				scaled[ii] = (vertices[ii]+Math.signum(vertices[ii])*padding.y+getY())*drawScale.y;				
			}
		}
		short[] tris = {0,1,3,3,2,1};
		anchor = new Vector2(getX(),getY());
		region = new PolygonRegion(texture,scaled,tris);
	}
	
	/**
	 * Reset the polygon vertices in the shape to match the dimension.
	 */
	protected void resize(float width, float height) {
		super.resize(width,height);
		initRegion();
	}
	
	/**
	 * Sets the current position for this physics body
	 *
	 * This method does not keep a reference to the parameter.
	 *
	 * @param value  the current position for this physics body
	 */
	public void setPosition(Vector2 value) {
		super.setPosition(value.x,value.y);
		initRegion();
	}

	/**
	 * Sets the current position for this physics body
	 *
	 * @param x  the x-coordinate for this physics body
	 * @param y  the y-coordinate for this physics body
	 */
	public void setPosition(float x, float y) {
		super.setPosition(x,y);
		initRegion();
	}
	
	/**
	 * Sets the x-coordinate for this physics body
	 *
	 * @param value  the x-coordinate for this physics body
	 */
	public void setX(float value) {
		super.setX(value);
		initRegion();
	}
	
	/**
	 * Sets the y-coordinate for this physics body
	 *
	 * @param value  the y-coordinate for this physics body
	 */
	public void setY(float value) {
		super.setY(value);
		initRegion();
	}
	
	/**
	 * Sets the angle of rotation for this body (about the center).
	 *
	 * @param value  the angle of rotation for this body (in radians)
	 */
	public void setAngle(float value) {
		throw new UnsupportedOperationException("Cannot rotate platforms");
	}
	
	/**
	 * Sets the object texture for drawing purposes.
	 *
	 * In order for drawing to work properly, you MUST set the drawScale.
	 * The drawScale converts the physics units to pixels.
	 * 
	 * @param value  the object texture for drawing purposes.
	 */
	public void setTexture(TextureRegion value) {
		super.setTexture(value);
		initRegion();
	}
	
    /**
     * Sets the drawing scale for this physics object
     *
     * The drawing scale is the number of pixels to draw before Box2D unit. Because
     * mass is a function of area in Box2D, we typically want the physics objects
     * to be small.  So we decouple that scale from the physics object.  However,
     * we must track the scale difference to communicate with the scene graph.
     *
     * We allow for the scaling factor to be non-uniform.
     *
     * @param x  the x-axis scale for this physics object
     * @param y  the y-axis scale for this physics object
     */
    public void setDrawScale(float x, float y) {
    	super.setDrawScale(x,y);
    	initRegion();
    }
    
    /**
     * Returns the sprite padding for this interior wall
     * 
     * The sprite padding is a vector indicating the additional width and height of
     * the sprite, beyond the bounds of the physics body. The dimensions are measured
     * in physics scale, not texture scale.  So if the padding is (0.1,0.2), the width
     * is 2*0.1 units larger, and the height is 2*0.2 units higher.
     *
     * While this method returns a reference to the padding vector, modifying this
     * vector will have no affect.  If you wish to change the padding, use setPadding.
     *
     * @return the sprite padding for this interior wall
     */
    public Vector2 getPadding() {
    	return padding;
    }
    
    /**
     * Sets the sprite padding for this interior wall
     * 
     * The sprite padding is a vector indicating the additional width and height of
     * the sprite, beyond the bounds of the physics body. The dimensions are measured
     * in physics scale, not texture scale.  So if the padding is (0.1,0.2), the width
     * is 2*0.1 units larger, and the height is 2*0.2 units higher.
     *
     * This method does not retain a reference to the parameter.
     *
     * @param pad the sprite padding for this interior wall
     */
    public void setPadding(Vector2 pad) {
    	padding.set(pad);
    	initRegion();
    }
    
    /**
     * Sets the sprite padding for this interior wall
     * 
     * The sprite padding is a vector indicating the additional width and height of
     * the sprite, beyond the bounds of the physics body. The dimensions are measured
     * in physics scale, not texture scale.  So if the padding is (0.1,0.2), the width
     * is 2*0.1 units larger, and the height is 2*0.2 units higher.
     *
     * @param w the horizontal padding for this interior wall
     * @param h the horizontal padding for this interior wall
     */
    public void setPadding(float w, float h) {
    	padding.set(w,h);
    	initRegion();
    }
    
	
	/**
	 * Initializes the platform via the given JSON value
	 *
	 * The JSON value has been parsed and is part of a bigger level file.  However, 
	 * this JSON value is limited to the platform subtree
	 *
	 * @param json	the JSON subtree defining the dude
	 */
	public void initialize(JsonValue json) {
		setName(json.name());
		float[] pos  = json.get("pos").asFloatArray();
		float[] size = json.get("size").asFloatArray();
		float[] pad  = json.get("pad").asFloatArray();
		setPosition(pos[0],pos[1]);
		setDimension(size[0],size[1]);
		setPadding(pad[0],pad[1]);
		
		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
		setDensity(json.get("density").asFloat());
		setFriction(json.get("friction").asFloat());
		setRestitution(json.get("restitution").asFloat());
		
		// Create the collision filter (used for light penetration)
      	short collideBits = LevelModel.bitStringToShort(json.get("collideBits").asString());
      	short excludeBits = LevelModel.bitStringToComplement(json.get("excludeBits").asString());
      	Filter filter = new Filter();
      	filter.categoryBits = collideBits;
      	filter.maskBits = excludeBits;
      	setFilterData(filter);

		// Reflection is best way to convert name to color
		Color debugColor;
		try {
			String cname = json.get("debugcolor").asString().toUpperCase();
		    Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
		    debugColor = new Color((Color)field.get(null));
		} catch (Exception e) {
			debugColor = null; // Not defined
		}
		int opacity = json.get("debugopacity").asInt();
		debugColor.mul(opacity/255.0f);
		setDebugColor(debugColor);
		
		// Now get the texture from the AssetManager singleton
		String key = json.get("texture").asString();
		TextureRegion texture = JsonAssetManager.getInstance().getEntry(key, TextureRegion.class);
		setTexture(texture);
	}
	
	/**
	 * Draws the physics object.
	 *
	 * @param canvas Drawing context
	 */
	public void draw(ObstacleCanvas canvas) {
		if (region != null) {
			canvas.draw(region,Color.WHITE,0,0,(getX()-anchor.x)*drawScale.x,(getY()-anchor.y)*drawScale.y,getAngle(),1,1);
		}
	}	
}
