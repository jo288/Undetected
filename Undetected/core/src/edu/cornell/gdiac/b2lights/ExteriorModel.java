/*
 * WallModel.java
 *
 * This is a refactored version of the wall (screen boundary) from Lab 4.  We have made 
 * it a specialized class so that we can import its properties from a JSON file.  
 *
 * A further change from the JSON version is the notion of "padding".  Padding is the
 * the amount that the visible sprite is larger than the physics object itself (measured
 * in physics coordinates, not Sprite coordinates). This keeps the shadow from completely 
 * obstructing the wall sprite.  
 *
 * Because this is an arbitrary polygon, it is difficult to compute proper padding easily.  
 * Therefore, we avoid the problem by allowing the user to specify a second polygon (still
 * in physics coordinates) for the Sprite.  In theory, this means that physics body and
 * sprite could be in completely different locations. In practice, however, the padding
 * polygon always "contains" the physics polygon.
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
 * A polygon shape representing the screen boundary
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.
 */
public class ExteriorModel extends PolygonObstacle {
	
	/**
	 * Create a new WallModel with degenerate settings
	 */	
	public ExteriorModel() {
		super(new float[]{0,0,1,0,1,1,0,1},0,0);
	}

	/**
	 * Initializes the Box2d shapes for this polygon
	 *
	 * If the texture is not null, this method also allocates the PolygonRegion 
	 * for drawing.  However, the points in the polygon region may be rescaled 
	 * later.
	 *
	 * In this version of the method, the physics polygon and the padding polygon
	 * are the same.
	 *
	 * @param points   The polygon vertices
	 */
	protected void initShapes(float[] points) {
		initShapes(points,points);
	}

	/**
	 * Initializes the Box2d shapes for this polygon
	 *
	 * If the texture is not null, this method also allocates the PolygonRegion 
	 * for drawing.  However, the points in the polygon region may be rescaled 
	 * later.
	 *
	 * In this version of the method, the physics polygon and the padding polygon
	 * are the same.
	 *
	 * @param points   The polygon vertices for the physics body
	 * @param padding  The polygon vertices for the object sprite
	 */
	protected void initShapes(float[] points, float[] padding) {
		// Dispose of any active shapes
		dispose();
	
		// Triangulate
		ShortArray array = TRIANGULATOR.computeTriangles(points);
		trimColinear(points,array);
		
		tridx = new short[array.items.length];
		System.arraycopy(array.items, 0, tridx, 0, tridx.length);
		
		// Allocate space for physics triangles.
		int tris = array.items.length / 3;
		vertices = new float[tris*6];
		shapes = new PolygonShape[tris];
		geoms  = new Fixture[tris];
		for(int ii = 0; ii < tris; ii++) {
			for(int jj = 0; jj < 3; jj++) {
				vertices[6*ii+2*jj  ] = points[2*array.items[3*ii+jj]  ];
				vertices[6*ii+2*jj+1] = points[2*array.items[3*ii+jj]+1];
			}
			shapes[ii] = new PolygonShape();
			shapes[ii].set(vertices,6*ii,6);
		}
		
		// Draw the shape with the appropriate scaling factor
		scaled = new float[padding.length];
		for(int ii = 0; ii < padding.length; ii+= 2) {
			scaled[ii  ] = padding[ii  ]*drawScale.x;
			scaled[ii+1] = padding[ii+1]*drawScale.y;
		}
		if (texture != null) {
			// WARNING: PolygonRegion constructor by REFERENCE
			region = new PolygonRegion(texture,scaled,tridx);
		}
		markDirty(true);
	}

	/**
	 * Initializes the wall via the given JSON value
	 *
	 * The JSON value has been parsed and is part of a bigger level file.  However, 
	 * this JSON value is limited to the wall subtree
	 *
	 * @param json	the JSON subtree defining the dude
	 */
	public void initialize(JsonValue json) {
		setName(json.name());
		
		// Technically, we should do error checking here.
		// A JSON field might accidentally be missing
		float[] verts = json.get("boundary").asFloatArray();
		float[] pads  = json.get("padding").asFloatArray();
		initShapes(verts,pads);
		
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
}
