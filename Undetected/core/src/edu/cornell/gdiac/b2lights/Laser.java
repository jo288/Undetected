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
    private static final float LAZER_HEIGHT = 5f;
    private static final float LAZER_WIDTH = 0.01f;
    private float x_pos;
    private float y_pos;
    private boolean isOn;
    /** how many seconds remaining until this laser turns off */
    private int time_to_live;
    /** the maximum time this laser can stay on for */
    private static final int LIFESPAN = 4;
    private Fixture sensorFixture;
    private PolygonShape sensorShape;



    public Laser(float x, float y) {
        super(x, y, LAZER_WIDTH, LAZER_HEIGHT);
        x_pos = x;
        y_pos = y;
        //setSensor(true);
    }

    public Laser() {
        super(LAZER_WIDTH, LAZER_HEIGHT);
        x_pos = 0;
        y_pos = 0;
        //setSensor(true);
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
        setPosition(pos[0]+0.5f,pos[1]+0.5f*size[1]);
        setWidth(size[0]);
        setHeight(size[1]);

        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setTimeToLive(json.get("timetolive").asInt());

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
        setOrigin(origin.x, 0);
    }

    public void start(){
        Timer.schedule(new Timer.Task(){
                           @Override
                           public void run() {
                               if(time_to_live==0){
                                   isOn = false;
                                   time_to_live = LIFESPAN;
                               }
                               else{
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

    @Override
    public void draw(ObstacleCanvas canvas){
        if(isOn){
            //canvas.setBlendState(ObstacleCanvas.BlendState.ALPHA_BLEND);
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),1.0f,getHeight()+0.5f);
        }
        else{
            //draw transparent texture instead
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),1,getHeight()+0.5f, 0.2f);

        }
    }
}
