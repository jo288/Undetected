package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import java.lang.reflect.*;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.physics.obstacle.*;

import javax.xml.soap.Text;

public class DecorativeModel extends BoxObstacle{
    /** Collide Bit */
    public static final String COLLIDE_BIT = "0010";
    /** Exclude Bit */
    public static final String EXCLUDE_BIT = "0000000000000000";
    /** Animation stop length */
    public static final int animationSpeed = 2;

    private TextureRegion decoTexture;
    private String decoType;
    private FilmStrip filmstrip;
    private int animationFrame = 0;
    private int animationRate = 4;
    private int animationCount = 0;
    private boolean flip = false;

    public DecorativeModel() {
        super(1,1);
    }

    public void initialize(JsonValue json){
        int[] pos  = json.get("pos").asIntArray();
        setPosition(pos[0]+0.5f,pos[1]+0.5f);

        setBodyType(BodyDef.BodyType.StaticBody);

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

        decoType = json.get("type").asString();
        switch (decoType){
            case "desk":
                setWidth(1f);
                setHeight(1f);
                setFixedRotation(true);
                texture = (json.get("direction").asString().equals("up")?
                        JsonAssetManager.getInstance().getEntry("deskFrontAnimation", TextureRegion.class):
                        JsonAssetManager.getInstance().getEntry("deskAnimation", TextureRegion.class));
                if (json.get("direction").asString().equals("right"))
                    flip = true;
                try {
                    filmstrip = new FilmStrip(texture.getTexture(), 1, 4);
                } catch (Exception e) {
                    filmstrip = null;
                }
                animationFrame = 4;
                break;
            case "experiment":
                break;
            default:
                break;
        }

        // Now get the texture from the AssetManager singleton
        setTexture(filmstrip);
        setOrigin(0, 0);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        filmstrip.setFrame(animationCount/animationRate);
        animationCount = (animationCount+1)%(animationFrame*animationRate);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(ObstacleCanvas canvas) {
        if (texture != null) {
            if (!flip)
                canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x-getWidth()/2*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),1.0f,1.0f);
            else
                canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x+getWidth()/2*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),-1.0f,1.0f);
        }
    }

}