package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
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
    LevelModel level;

    public MiniMap(float width, float height, LevelModel level){
        this.width = width;
        this.height = height;
        this.showExit = false;
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
        ShapeRenderer shapeRenderer = new ShapeRenderer();
        miniViewport.apply();
        canvas.begin();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        level.board.draw(canvas);
        for(Obstacle obj : level.objects) {
            if(obj instanceof ObjectiveModel) {
                if (!((ObjectiveModel) obj).getIsStolen()) {
                    alpha+=delta*2;
                    shapeRenderer.setColor(0.24f, 0.7f, 0.44f, alpha);
                    float x = obj.getX()*level.scale.x/zoom;
                    float y = obj.getY()*level.scale.y/zoom;
                    System.out.println("objective " + x + " " + y);
                    shapeRenderer.circle(x, y, 20);
                    if(alpha>1){
                        alpha=0;
                    }
                }
                else{
                    if(!showExit){showExit = true;}
                }
            }
            else if(obj instanceof ExitModel && showExit){
                alpha+=delta*2;
                shapeRenderer.setColor(1, 0, 0, alpha);
                float x = obj.getX()*level.scale.x/zoom;
                float y = obj.getY()*level.scale.y/zoom;
                shapeRenderer.circle(x, y, 20);
                if(alpha>1){
                    alpha=0;
                }
            }
            else {
                obj.draw(canvas);
            }
        }

        shapeRenderer.end();

        canvas.end();
        canvas.setCamera(bigCamera);

    }

}
