package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
    private float alpha; //flashing objective
    private boolean showExit;
    private boolean showObjective;
    LevelModel level;

    public MiniMap(float width, float height, LevelModel level){
        this.width = width;
        this.height = height;
        this.showExit = false;
        this.showObjective = false;
        this.level = level;
        this.zoom = Math.max((level.bounds.width*level.scale.x)/800, (level.bounds.height*level.scale.y)/600);
        System.out.println("ZOOM "+zoom);
        this.miniCam = new OrthographicCamera(800, 600);
        this.miniCam.zoom = zoom;
        this.miniCam.position.x = 400*zoom;
        this.miniCam.position.y = 300*zoom;
        this.miniCam.update();
        this.miniViewport = new FitViewport(800, 600, this.miniCam);
        this.miniViewport.setScreenBounds(0, (int)(600-height), (int)width, (int)height);

    }

    public void setZoom(float z){ this.zoom = z; this.miniCam.zoom = z;}

    public void render(ObstacleCanvas canvas, float delta){
        OrthographicCamera bigCamera = canvas.getCamera();
        canvas.setCamera(this.miniCam);
        miniViewport.apply();
        canvas.begin();
        level.board.draw(canvas);
        for(Obstacle obj : level.objects) {
            if(obj instanceof ObjectiveModel) {
                if (!((ObjectiveModel) obj).getIsStolen()) {
                    alpha+=delta*2;
                    if(alpha>1){
                        alpha=0;
                    }
                    ((ObjectiveModel) obj).drawMiniMap(canvas, alpha);
                }
                else{
                    if(!showExit){showExit = true;}
                }
            }
            else if(obj instanceof ExitModel && showExit){
                alpha+=delta*2;
                ((ExitModel) obj).drawMiniMap(canvas, alpha);
                if(alpha>1){
                    alpha=0;
                }
            }
            else {
                obj.draw(canvas);
            }
        }

        canvas.end();
        canvas.setCamera(bigCamera);
    }

}
