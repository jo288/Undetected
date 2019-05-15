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
    private boolean animationRepeat = false;
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
                setTexture(filmstrip);
                break;
            case "desk2":
                setWidth(1f);
                setHeight(1f);
                setFixedRotation(true);
                texture = (json.get("direction").asString().equals("up")?
                        JsonAssetManager.getInstance().getEntry("desk2Front", TextureRegion.class):
                        JsonAssetManager.getInstance().getEntry("desk2Side", TextureRegion.class));
                if (json.get("direction").asString().equals("right"))
                    flip = true;
                setTexture(texture);
                animationFrame = 0;
                break;
            case "experiment1":
                setWidth(1f);
                setHeight(1f);
                setFixedRotation(true);
                texture = JsonAssetManager.getInstance().getEntry("experiment1", TextureRegion.class);
                try {
                    filmstrip = new FilmStrip(texture.getTexture(), 1, 5);
                } catch (Exception e) {
                    filmstrip = null;
                }
                animationFrame = 5;
                setTexture(filmstrip);
                animationRate = 8;
                animationRepeat = true;
                break;
            case "experiment2":
                setWidth(1f);
                setHeight(1f);
                setFixedRotation(true);
                texture = JsonAssetManager.getInstance().getEntry("experiment2", TextureRegion.class);
                try {
                    filmstrip = new FilmStrip(texture.getTexture(), 1, 5);
                } catch (Exception e) {
                    filmstrip = null;
                }
                animationFrame = 5;
                setTexture(filmstrip);
                animationRate = 8;
                animationRepeat = true;
                break;
            case "servertower":
                setWidth(1f);
                setHeight(1f);
                setFixedRotation(true);
                texture = JsonAssetManager.getInstance().getEntry("servertower", TextureRegion.class);
                try {
                    filmstrip = new FilmStrip(texture.getTexture(), 1, 8);
                } catch (Exception e) {
                    filmstrip = null;
                }
                animationFrame = 8;
                setTexture(filmstrip);
                animationRate = 5;
                break;
            default:
                break;
        }

        // Now get the texture from the AssetManager singleton
        setOrigin(0, 0);
        if(filmstrip!=null){
            animationCount = (int)(Math.random()*(animationFrame*animationRate-1));
        }
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if(filmstrip!=null) {
            int anim = 0;
            if(animationCount>=2*(animationFrame*animationRate)-1){
                animationCount = 0;
            }
            if(animationRepeat&&animationCount>=(animationFrame*animationRate-1)){
                this.animationCount++;
                anim = (animationFrame * animationRate * 2 - this.animationCount - 1) / (animationRate);
            }else {
                animationCount = (animationCount + 1) % (animationFrame * animationRate);
                anim = animationCount/animationRate;
            }
            filmstrip.setFrame(anim);
        }
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