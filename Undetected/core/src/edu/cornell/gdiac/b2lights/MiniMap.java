package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;

public class MiniMap {
    private Viewport miniViewport;
    private OrthographicCamera miniCam;
    private float zoom;
    private float width;
    private float height;
    LevelModel level;

    public MiniMap(float width, float height, LevelModel level){
        this.width = width;
        this.height = height;
        this.level = level;
        this.zoom = Math.max((level.bounds.width*level.scale.x)/800, (level.bounds.height*level.scale.y)/600);
        this.miniCam = new OrthographicCamera(800, 600);
        this.miniCam.zoom = zoom;
        this.miniCam.position.x = 400*zoom;
        this.miniCam.position.y = 300*zoom;
        this.miniCam.update();
        this.miniViewport = new FitViewport(800, 600, this.miniCam);
        this.miniViewport.setScreenBounds(0, (int)(600-height), (int)width, (int)height);

    }

    public void setZoom(float z){ this.zoom = z; this.miniCam.zoom = z;}

    public void render(ObstacleCanvas canvas){
        OrthographicCamera bigCamera = canvas.getCamera();
        canvas.setCamera(this.miniCam);
        miniViewport.apply();
        canvas.begin();
        level.board.draw(canvas);
        for(Obstacle obj : level.objects) {
            obj.draw(canvas);
        }
        canvas.end();
        canvas.setCamera(bigCamera);

    }

}
