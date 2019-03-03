package edu.cornell.gdiac.undetected;

import edu.cornell.gdiac.physics.obstacle.WheelObstacle;

public class Character extends WheelObstacle {
    public Character(){
        super(0,0,1.0f);
    }

    public Character(int x,int y,float radius){
        super(x,y,radius);
    }
}
