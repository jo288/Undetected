package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.RayCastCallback;
import edu.cornell.gdiac.physics.lights.ConeSource;
import edu.cornell.gdiac.physics.lights.LightSource;
import edu.cornell.gdiac.physics.obstacle.Obstacle;

import java.util.ArrayList;
import java.util.List;

public class LightController {
    LevelModel level;
    static float dist_to_player;
    static Vector2 lightPos;
    static short maskBits;
    static short collideBits;
    private static List<Body> intersected = new ArrayList<Body>();
    private static List<Vector2> contact_points = new ArrayList<Vector2>();
    final RayCastCallback ray = new RayCastCallback() {
        @Override
        final public float reportRayFixture(Fixture fixture, Vector2 point,
                                            Vector2 normal, float fraction) {
            Body b = fixture.getBody();
            Object o = b.getUserData();
            if((maskBits & fixture.getFilterData().categoryBits) !=0 && (collideBits & fixture.getFilterData().maskBits)!=0){
                intersected.add(fixture.getBody());
                contact_points.add(point);
                //System.out.println("collided with "+fixture.getUserData());
            }
            else{
                //System.out.println("collided with nothing");
            }
            if(!(o instanceof DudeModel) && point.dst(lightPos)<dist_to_player){
                return 0;
            }
            return 1;
        }
    };

    public LightController(LevelModel level){
        this.level = level;
    }

    public boolean detect(){
        ArrayList<GuardModel> guards = this.level.getGuards();
        ArrayList<CameraModel> cameras = this.level.getCameras();
        DudeModel player = this.level.getAvatar();

        for(GuardModel guard: guards){
            Vector2 playerPos = new Vector2(player.getX(), player.getY());
            ConeSource light = guard.getLight();
            maskBits = light.getContactFilter().maskBits;
            collideBits = light.getContactFilter().categoryBits;
            lightPos = light.getPosition();
            float range = ((ConeSource)light).getDistance();
            //a vector from the guard to the player
            Vector2 guard_to_player = playerPos.sub(guard.getPosition());

            dist_to_player = Vector2.len(guard_to_player.x, guard_to_player.y);
            //the angle between the player and the guard
            float player_guard_angle = guard_to_player.angle(guard.getDirection());
            player_guard_angle = player_guard_angle < 0 ? player_guard_angle+360:player_guard_angle;
            player_guard_angle = player_guard_angle > 180? 360-player_guard_angle:player_guard_angle;

            //if player is within the cone light region, raycast from guard to player
            if(dist_to_player<=range && player_guard_angle <= light.getConeDegree()){
                level.getWorld().rayCast(ray, light.getPosition(), player.getPosition());
                for(int i=0; i<intersected.size(); i++){
                    Object b = intersected.get(i).getUserData();
                    if(!(b instanceof DudeModel && (Obstacle)b==player)){
                        if(light.getPosition().dst(contact_points.get(i))<dist_to_player){
                            clearIntersectionData();
                            return false;
                        }
                    }
                }
                clearIntersectionData();
                return true;
            }

            //player is not in guard's cone light, but within their sensitive radius
            /**else if(dist_to_player<=guard.getSensitiveRadius()){
                level.getWorld().rayCast(ray, guard.getPosition(), player.getPosition());
                for(int i=0; i<intersected.size(); i++){
                    Object b = intersected.get(i).getUserData();
                    if(!(b instanceof DudeModel && (Obstacle)b==player)){
                        if(guard.getPosition().dst(contact_points.get(i))<dist_to_player){
                            clearIntersectionData();
                            return false;
                        }
                    }
                }
                clearIntersectionData();
                guard.collidedAvatar(player);
                guard.setAlarmed(true);
                return true;
            }**/                                                  //PUT THIS IN LATER WHEN WE GET IN RINGS FOR THE GUARD
        }
        //will probably group guards and cameras together later
        for(CameraModel cam: cameras) {
            if (cam.isOn()) {
                Vector2 playerPos = new Vector2(player.getX(), player.getY());
                ConeSource light = cam.getLight();
                maskBits = light.getContactFilter().maskBits;
                lightPos = light.getPosition();
                float range = ((ConeSource) light).getDistance();
                //a vector from the guard to the player
                Vector2 guard_to_player = playerPos.sub(cam.getPosition());

                dist_to_player = Vector2.len(guard_to_player.x, guard_to_player.y);
                //the angle between the player and the guard
                float player_guard_angle = guard_to_player.angle(cam.getDirection());
                player_guard_angle = player_guard_angle < 0 ? player_guard_angle + 360 : player_guard_angle;
                player_guard_angle = player_guard_angle > 180 ? 360 - player_guard_angle : player_guard_angle;

                //if player is within the cone light region, raycast from guard to player
                if (dist_to_player <= range && player_guard_angle <= light.getConeDegree()) {
                    level.getWorld().rayCast(ray, light.getPosition(), player.getPosition());
                    for (int i = 0; i < intersected.size(); i++) {
                        Object b = intersected.get(i).getUserData();
                        if (!(b instanceof DudeModel && (Obstacle) b == player)) {
                            if (light.getPosition().dst(contact_points.get(i)) < dist_to_player) {
                                clearIntersectionData();
                                return false;
                            }
                        }
                    }
                    clearIntersectionData();
                    return true;
                }
            }
            clearIntersectionData();
        }
        return false;
    }

    public void clearIntersectionData(){
        this.intersected = new ArrayList<Body>();
        this.contact_points = new ArrayList<Vector2>();
    }

}
