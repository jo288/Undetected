package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Timer;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.util.JsonAssetManager;

public class Alarm extends ObstacleCanvas{
    //time left for the player to get to the exit
    private int timeLeft;
    private float alpha;
    private static float flash_speed = 0.05f;
    private boolean isOn = false;
    private TextureRegion alarmTexture;

    public Alarm(){
        alpha = 1;
        alarmTexture = JsonAssetManager.getInstance().getEntry("alarmFlash", TextureRegion.class);

    }

    public float getAlpha(){
        return alpha;
    }

    public boolean isOn(){return isOn;}
    public void turnOn(){isOn = true;}
    public void turnOff(){isOn = false;}

    public int getTimeLeft(){return timeLeft;}
    public void start(){
        Timer.schedule(new Timer.Task() {
                           @Override
                           public void run() {
                               alpha+=flash_speed;
                               if(alpha>1 || alpha<0){
                                   flash_speed*=-1;
                               }
                           }
                       }
                , 0        //    (delay)
                , 0.1f     //    (seconds)
        );
    }
    public void draw(ObstacleCanvas canvas){
        canvas.draw(alarmTexture, Color.WHITE, alarmTexture.getRegionWidth()/2,
                alarmTexture.getRegionHeight()/2, canvas.getWidth(), canvas.getHeight(), 0, 5, 5, alpha);
    }
}
