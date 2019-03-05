package edu.cornell.gdiac.physics.obstacle;

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
    private static final float LAZER_HEIGHT = 5.6f;
    private static final float LAZER_WIDTH = 0.01f;
    private float x_pos;
    private float y_pos;
    private boolean isOn;
    private Fixture sensorFixture;
    private PolygonShape sensorShape;


    public Laser(float x, float y) {
        super(x, y, LAZER_WIDTH, LAZER_HEIGHT);
        x_pos = x;
        y_pos = y;
        setSensor(true);
    }

    public Laser() {
        super(LAZER_WIDTH, LAZER_HEIGHT);
        x_pos = 0;
        y_pos = 0;
        setSensor(true);
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

    public void initialize() {
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("laser", TextureRegion.class);
        setTexture(texture);
        this.setBodyType(BodyDef.BodyType.StaticBody);

//        Vector2 sensorCenter = new Vector2(x_pos, LAZER_HEIGHT/2);
//        FixtureDef sensorDef = new FixtureDef();
//        sensorDef.density = 0f;
//        sensorDef.isSensor = true;
//        sensorShape = new PolygonShape();
//        sensorShape.setAsBox(LAZER_WIDTH, LAZER_HEIGHT, sensorCenter, 0.0f);
//        sensorDef.shape = sensorShape;
//
//        sensorFixture = body.createFixture(sensorDef);
//        sensorFixture.setUserData(getSensorName());
    }

    public void start(){
        Timer.schedule(new Timer.Task(){
                           @Override
                           public void run() {
                               isOn = !isOn;
                           }
                       }
                , 0        //    (delay)
                , 2     //    (seconds)
        );
    }

    public boolean isTurnedOn(){return isOn;}

    @Override
    public void draw(ObstacleCanvas canvas){
        if(isOn){
            super.draw(canvas);
        }
        else{
            //draw transparent texture instead
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.x,getAngle(),1,1, true);

        }
    }
}
