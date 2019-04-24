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

public class Laser extends BoxObstacle {
    /** Collide Bit */
    public static final String COLLIDE_BIT = "0010";
    /** Default Width of Player */
    //public static final String EXCLUDE_BIT = "0100";
    public static final short MASK_BIT = (short)0xefef;

    private static final float LAZER_HEIGHT = 5f;
    private static final float LAZER_WIDTH = 0.01f;
    private float x_pos;
    private float y_pos;
    private boolean isOn;
    private boolean isHorizontal;
    private int time_store;
    /** how many seconds remaining until this laser turns off */
    private int time_to_live;
    /** the maximum time this laser can stay on for */
    private static final int LIFESPAN = 240;
    /** Sector of the laser */
    public int sector;
    private Fixture sensorFixture;
    private PolygonShape sensorShape;

    /** FilmStrip pointer to the texture region */
    private FilmStrip filmstrip;
    private FilmStrip sidefilmstrip;
    int animateCool;

    public Laser(float x, float y) {
        super(x, y, LAZER_WIDTH, LAZER_HEIGHT);
        x_pos = x;
        y_pos = y;
//        setSensor(true);
    }

    public Laser() {
        super(LAZER_WIDTH, LAZER_HEIGHT);
        x_pos = 0;
        y_pos = 0;
//        setSensor(true);
    }

    /**
     * Returns the name of the sensor
     *
     * This is used by ContactListener
     *
     * @return the name of the ground sensor
     */
    public String getSensorName() {
        return "laser" + x_pos + y_pos;
    }

    public void setTimeToLive(int t){time_to_live = t;}

    public void initialize() {
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("laser", TextureRegion.class);
        setTexture(texture);
        this.setBodyType(BodyDef.BodyType.StaticBody);
    }

    public void initialize(JsonValue json) {
        setName(json.name());
        int[] pos  = json.get("pos").asIntArray();
        float[] size = json.get("size").asFloatArray();
        if(size[0]==1f) {
            setPosition(pos[0] + 0.5f, pos[1] + 0.5f * size[1]);
            isHorizontal = false;
            setWidth(size[0]);
            setHeight(size[1]);
        }else{
            setPosition(pos[0]+0.5f*size[0],pos[1]+0.5f);
            isHorizontal = true;
            setWidth(size[1]);
            setHeight(size[0]);
            setAngle(1.570796f);
        }

        setBodyType(BodyDef.BodyType.StaticBody);
        setTimeToLive(json.get("timetolive").asInt()*60);

        sector = json.get("sector").asInt();

        // Create the collision filter (used for light penetration)
        short collideBits = LevelModel.bitStringToShort(COLLIDE_BIT);
        //short excludeBits = LevelModel.bitStringToComplement(EXCLUDE_BIT);
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = MASK_BIT;
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

        // Now get the texture from the AssetManager singleton
//        String key = json.get("texture").asString();
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("laser", TextureRegion.class);
        setTexture(texture);
        setOrigin(origin.x, 0);


        if (isHorizontal) {
            texture = JsonAssetManager.getInstance().getEntry("sidelaserAnimation", TextureRegion.class);
            try {
//			filmstrip = (FilmStrip)texture;
                filmstrip = new FilmStrip(texture.getTexture(), 1, 6);
            } catch (Exception e) {
                filmstrip = null;
            }
        }else {
            texture = JsonAssetManager.getInstance().getEntry("laserAnimation", TextureRegion.class);
            try {
//			filmstrip = (FilmStrip)texture;
                filmstrip = new FilmStrip(texture.getTexture(), 1, 6);
            } catch (Exception e) {
                filmstrip = null;
            }
        }


//		setTexture(texture);
    }

//    public  void switchState() {
//        isOn = !isOn;
//        setOn(isOn);
//    }

    public void permOn() {
        time_to_live = Integer.MAX_VALUE;
    }

    public void setOn(boolean val) {
        isOn = val;
        if (isOn) {
            time_to_live = time_store;
        } else {
            time_store = time_to_live;
            time_to_live = -1;
        }
    }

    public void pause() {
        time_store = time_to_live;
        if (isOn) {
            time_to_live = Integer.MAX_VALUE;
        } else {
            time_to_live = -1;
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

        if (time_to_live == 0) {
            isOn = false;
            time_to_live = LIFESPAN;
        } else if (time_to_live<=60){
            isOn = false;
        } else {
            isOn = true;
        }
        time_to_live--;

        //Animate
        if (time_to_live <= -1) {
            filmstrip.setFrame(0);
        }
        else if (time_to_live <= 8 && time_to_live >= 0) {
            if (filmstrip != null) {
//                int next = (filmstrip.getFrame()+1) % filmstrip.getSize();
                filmstrip.setFrame(5-time_to_live/2);
            }
        }else if (time_to_live>=60 && time_to_live<=68){
            if (filmstrip != null) {
//                int next = (filmstrip.getFrame()+5) % filmstrip.getSize();
//                filmstrip.setFrame(next);
                filmstrip.setFrame((time_to_live-59)/2);
            }
        }
        else if (time_to_live<60) {
            filmstrip.setFrame(0);
        }
        else {
            filmstrip.setFrame(5);
        }

        /*
        // Animate if necessary
        if (animate && animateCool == 0) {
            if (filmstrip != null) {
                int next = (filmstrip.getFrame()+1) % filmstrip.getSize();
                filmstrip.setFrame(next);
            }
            animateCool = 6;
        } else if (walkCool > 0) {
            walkCool--;
        } else if (!animate) {
            if (filmstrip != null) {
                filmstrip.setFrame(0);
            }
            animateCool = 0;
        }

        super.update(dt);
        */
    }

    public void resume() {
        time_to_live = time_store;
    }

    public void start(){
        Timer.schedule(new Timer.Task() {
                           @Override
                           public void run() {
                               if (time_to_live == 0) {
                                   isOn = false;
                                   time_to_live = LIFESPAN;
                               } else if (time_to_live <= -1) {
                                   isOn = false;
                               } else {
                                   isOn = true;
                               }
                               time_to_live--;
                           }
                       }
                , 0        //    (delay)
                , 1     //    (seconds)
        );
    }

    public boolean isTurnedOn(){return isOn;}
    public boolean isHorizontal(){return isHorizontal;}

    @Override
    public void draw(ObstacleCanvas canvas){
        if (!isHorizontal) {
            canvas.draw(filmstrip, Color.WHITE, 16, origin.y, getX() * drawScale.x, getY() * drawScale.y + getHeight() / 2 * drawScale.y, 0, 1, 1);
            if (isOn) {
                //canvas.setBlendState(ObstacleCanvas.BlendState.ALPHA_BLEND);
                canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y - getHeight() / 2 * drawScale.y, getAngle(), 1.0f, getHeight() + 0.5f);
            } else {
                //draw transparent texture instead
                canvas.draw(texture, Color.WHITE, origin.x, origin.y, getX() * drawScale.x, getY() * drawScale.y - getHeight() / 2 * drawScale.y, getAngle(), 1, getHeight() + 0.5f, 0.0f);

            }
        }else{
            if (isOn) {
                //canvas.setBlendState(ObstacleCanvas.BlendState.ALPHA_BLEND);
                canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x + getHeight()/2*drawScale.x,getY()*drawScale.y,getAngle(),1f,getHeight());
            } else {
                //draw transparent texture instead
                canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x + getHeight()/2*drawScale.x,getY()*drawScale.y,getAngle(),1f,getHeight(),0.0f);
            }
            canvas.draw(filmstrip, Color.WHITE, 16, 16, getX() * drawScale.x + getHeight()/2*drawScale.x, getY() * drawScale.y, 3.1415f, 1, 1);
            canvas.draw(filmstrip, Color.WHITE, 16, 16, getX() * drawScale.x - getHeight()/2*drawScale.x, getY() * drawScale.y, 0, 1, 1);
        }
    }
}
