package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Timer;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.util.JsonAssetManager;

public class Alarm extends ObstacleCanvas{
    //time left (in seconds) for the player to get to the exit
    private float timeLeft;
    private float alpha;
    private static float flash_speed = 0.05f;
    private boolean isOn = false;
    private TextureRegion alarmTexture;
    private BitmapFont displayFont;

    public Alarm(){
        alpha = 1;
        timeLeft = 120;
        alarmTexture = JsonAssetManager.getInstance().getEntry("alarmFlash", TextureRegion.class);
        displayFont = JsonAssetManager.getInstance().getEntry("timerFont", BitmapFont.class);
    }

    public float getAlpha(){
        return alpha;
    }

    public boolean isOn(){return isOn;}
    public void turnOn(){isOn = true;}
    public void turnOff(){isOn = false;}

    public float getTimeLeft(){return timeLeft;}
    public void start(){
        Timer.schedule(new Timer.Task() {
                           @Override
                           public void run() {
                               if((alpha > 0.5 && flash_speed > 0) || (alpha<0 && flash_speed < 0)){
                                   flash_speed*=-1;
                               }
                               alpha+=flash_speed;
                               timeLeft-=0.1f;
                           }
                       }
                , 0        //    (delay)
                , 0.1f     //    (seconds)
        );
    }
    public void draw(ObstacleCanvas canvas){
        OrthographicCamera cam = canvas.getCamera();
        canvas.draw(alarmTexture, Color.WHITE, alarmTexture.getRegionWidth()/2,
                alarmTexture.getRegionHeight()/2, canvas.getWidth(), canvas.getHeight(), 0, 5, 5, alpha);
        String seconds = ""+(int)(timeLeft%60);
        if(timeLeft%60<10){
            seconds = "0"+seconds;
        }
        if(timeLeft>=0) {
            displayFont.setColor(Color.WHITE);
            canvas.drawText((int) (timeLeft / 60) + ":" + seconds, displayFont, cam.position.x - 30 * cam.zoom-50, cam.position.y + 280 * cam.zoom);
        }
    }
}
