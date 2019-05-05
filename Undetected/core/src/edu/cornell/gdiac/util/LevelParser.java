package edu.cornell.gdiac.util;

import com.badlogic.gdx.Gdx;
import java.io.StringWriter;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.*;
import edu.cornell.gdiac.b2lights.DecorativeModel;

public class LevelParser {

    private class Level{
        protected int[] graphicSize = {800,600};
        protected int[] boardSize = {20,20};
        protected int tileSize = 32;
        protected int[] fpsRange = {20,60};
        protected Lighting lighting = new Lighting();
        protected Array<Light> lights = new Array<Light>();
        protected Player avatar = new Player();
        protected Array<Guard> guards = new Array<Guard>();
        protected Exit exit = new Exit();
        protected Array<Box> boxes = new Array<Box>();
        protected Array<Switch> switches = new Array<Switch>();
        protected Array<Door> doors = new Array<Door>();
        protected Array<Laser> lasers = new Array<Laser>();
        protected Array<Decorative> decoratives = new Array<Decorative>();
        protected Objective objective = new Objective();
        protected Array<Integer> invalidTiles = new Array<Integer>();
        protected ExteriorWall exteriorwall = new ExteriorWall();
        protected InteriorWall interiorwall = new InteriorWall();
    }

    private class Lighting{
        protected float[] color = {0.9f,0.9f,0.9f,0.8f};
        protected boolean gamma = true;
        protected boolean diffuse = true;
        protected int blur = 3;
    }

    private class Light{
        protected float[] color = {1f,1f,0,1f};
        protected float distance = 9;
        protected float angle = 25;
    }

    private class Player{
        protected int[] pos = new int[2];
    }

    private class Guard{
        protected String name = "guard";
        protected int[] pos = new int[2];
        protected String status = "sleep";
        protected float force = 40f;
        protected float sensitiveRadius = 1.6f;
        protected Array<Integer> path = new Array<Integer>();
        protected Array<Integer> objectivepath = new Array<Integer>();
        protected int lightIndex;
        protected int sector = 0;
        protected String direction = "up";
    }

    private class Exit{
        protected int[] pos = new int[2];
        protected int[] size = {2,1};
    }

    private class Box{
        protected String name = "box";
        protected int[] pos = new int[2];
        protected String texture = "box1";
    }

    private class Laser{
        protected String name = "laser";
        protected int[] pos = new int[2];
        protected int[] size = new int[2];
        protected int timetolive = 0;
        protected int sector = 0;
    }

    private class Objective{
        protected int[] pos = new int[2];
        protected int[] size = {1,1};
        protected String texture = "key";
        protected boolean hasAlarm = false;
        protected Array<String> doors = new Array<String>();
        protected Array<String> lasers = new Array<String>();
    }

    private class Switch{
        protected String name = "switch";
        private int[] pos = new int[2];
        private boolean switched = false;
        private Array<String> doors = new Array<String>();
        private Array<String> lasers = new Array<String>();
    }

    private class Door{
        protected String name = "door";
        protected boolean isVertical = true;
        private int[] pos = new int[2];
        private boolean open = false;
    }

    private class Camera{
        protected String name = "camera";
        private int[] pos = new int[2];
    }

    private class Decorative{
        protected int[] pos = new int[2];
        protected String type = "desk";
        protected String direction = "right";
    }

    private class ExteriorWall{
        protected Array<Integer> pos = new Array<Integer>();
        protected Array<Integer> type = new Array<Integer>();
    }

    private class InteriorWall{
        protected Array<Integer> pos = new Array<Integer>();
        protected Array<Integer> type = new Array<Integer>();
    }

    public String readXml(FileHandle fh){
        XmlReader x = new XmlReader();
        XmlReader.Element xmlLevel = x.parse(fh);
        Level testLevel = new Level();

        testLevel.boardSize = new int[] {xmlLevel.getInt("width"),xmlLevel.getInt("height")};
        testLevel.graphicSize = new int[]{testLevel.boardSize[0]*32,testLevel.boardSize[1]*32};
        try {
            String t = xmlLevel.getChildByName("properties").getChildByName("property").get("value");
            testLevel.lighting.color[3] = Integer.parseInt(t.substring(1, 3), 16) / 255f;
            testLevel.lighting.color[0] = Integer.parseInt(t.substring(3, 5), 16) / 255f;
            testLevel.lighting.color[1] = Integer.parseInt(t.substring(5, 7), 16) / 255f;
            testLevel.lighting.color[2] = Integer.parseInt(t.substring(7, 9), 16) / 255f;
        } catch (Exception e){}

        try {
            XmlReader.Element tileLayer = xmlLevel.getChildByName("layer");
            String tileData = tileLayer.getChildByName("data").getText();
            tileData = tileData.replaceAll("[,\r\n]", "");
            for (int i = 0; i < tileData.length(); i++) {
                if (tileData.charAt(i) == '0' &&
                !searchCoordinateArrays(i % testLevel.boardSize[0],testLevel.boardSize[1] - 1 - (i / testLevel.boardSize[0]),testLevel.exteriorwall.pos)
                && !searchCoordinateArrays(i % testLevel.boardSize[0],testLevel.boardSize[1] - 1 - (i / testLevel.boardSize[0]),testLevel.interiorwall.pos)) {
                    testLevel.invalidTiles.add(i % testLevel.boardSize[0]);
                    testLevel.invalidTiles.add(testLevel.boardSize[1] - 1 - (i / testLevel.boardSize[0]));
                }
            }
        } catch (Exception e){}


        Array<XmlReader.Element> objects = xmlLevel.getChildrenByNameRecursively("object");
        for (XmlReader.Element e:objects){
            if(e.get("template").equals("HorizontalWall.tx")){
                int h = testLevel.boardSize[1]-e.getInt("y")/32;
                for(int i=e.getInt("x")/32;i<e.getInt("x")/32+e.getInt("width",32)/32;i++){
                    testLevel.exteriorwall.pos.add(i);
                    testLevel.exteriorwall.pos.add(h);
                    testLevel.exteriorwall.type.add(0);
                    deleteFromCoordinateArrays(i,h,testLevel.invalidTiles);
                }
            }
            if(e.get("template").equals("VerticalWall.tx")){
                int w = e.getInt("x")/32;
                for(int i=testLevel.boardSize[1]-e.getInt("y")/32;i<testLevel.boardSize[1]-e.getInt("y")/32+e.getInt("height",32)/32;i++){
                    testLevel.exteriorwall.pos.add(w);
                    testLevel.exteriorwall.pos.add(i);
                    testLevel.exteriorwall.type.add(1);
                    deleteFromCoordinateArrays(w,i,testLevel.invalidTiles);
                }
            }
            if(e.get("template").equals("KeyObjective.tx")){
                try {
                    testLevel.objective.hasAlarm = e.getChildByNameRecursive("property").getBoolean("value", false);
                }catch (Exception ex){}
                testLevel.objective.texture = "key";
                testLevel.objective.size = new int[] {1,1};
                testLevel.objective.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                if (e.hasChildRecursive("property")){
                    Array<XmlReader.Element> properties = e.getChildrenByNameRecursively("property");
                    for (XmlReader.Element p: properties){
                        if (p.get("name").equals("doors")){
                            String[] ds = p.get("value").split(",");
                            //for(String d:ds){ s.doors.add(Integer.parseInt(d));}
                            for(String d:ds){ testLevel.objective.doors.add(d);}
                        } else if (p.get("name").equals("lasers")){
                            String[] ds = p.get("value").split(",");
                            //for(String d:ds){ s.lasers.add(Integer.parseInt(d));}
                            for(String d:ds){ testLevel.objective.lasers.add(d);}
                        }
                    }
                }
            }
            if(e.get("template").equals("Exit.tx")) {
                testLevel.exit.pos = new int[] {e.getInt("x")/32+1,testLevel.boardSize[1]-e.getInt("y")/32};
            }
            if(e.get("template").equals("DoorClosed.tx")){
                Door d = new Door();
                d.name = (e.hasAttribute("name")?e.getAttribute("name"):d.name);
                d.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                d.open = false;
                d.isVertical = true;
                testLevel.doors.add(d);
            }
            if(e.get("template").equals("DoorOpen.tx")){
                Door d = new Door();
                d.name = (e.hasAttribute("name")?e.getAttribute("name"):d.name);
                d.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                d.open = true;
                d.isVertical = true;
                testLevel.doors.add(d);
            }
            if(e.get("template").equals("DoorClosedSide.tx")){
                Door d = new Door();
                d.name = (e.hasAttribute("name")?e.getAttribute("name"):d.name);
                d.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                d.open = false;
                d.isVertical = false;
                testLevel.doors.add(d);
            }
            if(e.get("template").equals("Switch.tx")){
                Switch s = new Switch();
                s.name = (e.hasAttribute("name")?e.getAttribute("name"):s.name);
                s.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                if (e.hasChildRecursive("property")){
                    Array<XmlReader.Element> properties = e.getChildrenByNameRecursively("property");
                    for (XmlReader.Element p: properties){
                        if (p.get("name").equals("doors")){
                            String[] ds = p.get("value").split(",");
                            //for(String d:ds){ s.doors.add(Integer.parseInt(d));}
                            for(String d:ds){ s.doors.add(d);}
                        } else if (p.get("name").equals("lasers")){
                            String[] ds = p.get("value").split(",");
                            //for(String d:ds){ s.lasers.add(Integer.parseInt(d));}
                            for(String d:ds){ s.lasers.add(d);}
                        } else if(p.get("name").equals("switched")){
                            s.switched = p.getBoolean("value");
                        }
                    }
                }
                testLevel.switches.add(s);
            }
//            if(e.get("template").equals(""))
            if(e.get("template").equals("Player.tx")){
                testLevel.avatar.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
            }
            if(e.get("template").equals("Box1.tx")){
                Box b1 = new Box();
                b1.name = (e.hasAttribute("name")?e.getAttribute("name"):b1.name);
                b1.texture = "box1";
                b1.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                testLevel.boxes.add(b1);
            }
            if(e.get("template").equals("Box2.tx")){
                Box b2 = new Box();
                b2.name = (e.hasAttribute("name")?e.getAttribute("name"):b2.name);
                b2.texture = "box2";
                b2.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                testLevel.boxes.add(b2);
            }
            if(e.get("template").equals("Box3.tx")){
                Box b3 = new Box();
                b3.name = (e.hasAttribute("name")?e.getAttribute("name"):b3.name);
                b3.texture = "box3";
                b3.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                testLevel.boxes.add(b3);
            }
            if (e.get("template").equals("DeskLeft.tx")){
                Decorative deskl = new Decorative();
                deskl.type = "desk";
                deskl.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                deskl.direction = "left";
                testLevel.decoratives.add(deskl);
            }
            if (e.get("template").equals("DeskRight.tx")){
                Decorative deskr = new Decorative();
                deskr.type = "desk";
                deskr.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                deskr.direction = "right";
                testLevel.decoratives.add(deskr);
            }
            if (e.get("template").equals("DeskUp.tx")){
                Decorative desku = new Decorative();
                desku.type = "desk";
                desku.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                desku.direction = "up";
                testLevel.decoratives.add(desku);
            }
            if (e.get("template").equals("Laser.tx")){
                Laser l = new Laser();
                l.name = (e.hasAttribute("name")?e.getAttribute("name"):l.name);
                if(e.hasAttribute("rotation")){
                    l.size = new int[] {e.getInt("height")/32,1};
                    l.pos = new int[] {Math.round(e.getFloat("x"))/32,testLevel.boardSize[1]-Math.round(e.getFloat("y"))/32};
                }else{
                    l.size = new int[] {1,e.getInt("height")/32};
                    l.pos = new int[] {Math.round(e.getFloat("x"))/32,testLevel.boardSize[1]-Math.round(e.getFloat("y"))/32};
                }
                if(e.hasChild("properties")) {
                    Array<XmlReader.Element> properties = e.getChildrenByNameRecursively("property");
                    for (XmlReader.Element property: properties){
                        try {
                            switch (property.get("name")) {
                                case "sector":
                                    l.sector = property.getInt("value");
                                    break;
                                case "timetolive":
                                    l.timetolive = property.getInt("value");
                                    break;
                                default:
                                    break;
                            }
                        } catch (Exception ex) { System.err.println("laser property parsing error");}
                    }
                }
                testLevel.lasers.add(l);
            }
            if(e.get("template").equals("Guard.tx")){
                Guard g = new Guard();
                Light l = new Light();
                Array<XmlReader.Element> properties = e.getChildrenByNameRecursively("property");
                for (XmlReader.Element property: properties){
                    try {
                        switch (property.get("name")) {
                            case "sector":
                                g.sector = property.getInt("value");
                                break;
                            case "IsPatrolling":
                                if (property.getBoolean("value")) g.status = "patrol";
                                break;
                            case "force":
                                g.force = property.getFloat("value");
                                break;
                            case "sensitiveRadius":
                                g.sensitiveRadius = property.getFloat("value");
                                break;
                            case "path":
                                String p = property.get("value");
                                String[] pSplit = p.substring(1, p.length() - 1).split("[^0-9]");
                                for (String s : pSplit) {
                                    g.path.add(Integer.parseInt(s));
                                }
                                break;
                            case "objectivepath":
                                String op = property.get("value");
                                String[] opSplit = op.substring(1, op.length() - 1).split("[^0-9]");
                                for (String s : opSplit) {
                                    g.objectivepath.add(Integer.parseInt(s));
                                }
                                break;
                            case "lightAngle":
                                l.angle = property.getFloat("value");
                                break;
                            case "lightRadius":
                                l.distance = property.getFloat("value");
                                break;
                            case "lightColor":
                                String t = property.get("value");
                                l.color[3] = Integer.parseInt(t.substring(1, 3), 16) / 255f;
                                l.color[0] = Integer.parseInt(t.substring(3, 5), 16) / 255f;
                                l.color[1] = Integer.parseInt(t.substring(5, 7), 16) / 255f;
                                l.color[2] = Integer.parseInt(t.substring(7, 9), 16) / 255f;
                                break;
                            case "direction":
                                if (property.get("value").equals("up")||property.get("value").equals("down")||
                                        property.get("value").equals("left")||property.get("value").equals("right"))
                                    g.direction = property.get("value");
                                break;
                            default:
                                break;
                        }
                    } catch (Exception ex) { System.err.println("guard property parsing error"+fh.name()+"\n"+ex.getMessage());}
                }
                g.name = (e.hasAttribute("name")?e.getAttribute("name"):g.name);
                g.pos = new int[] {e.getInt("x")/32,testLevel.boardSize[1]-e.getInt("y")/32};
                g.lightIndex = testLevel.guards.size;
                testLevel.guards.add(g);
                testLevel.lights.add(l);
            }
        }


        Json json = new Json();
        json.setOutputType(JsonWriter.OutputType.json);

        return json.prettyPrint(testLevel);
    }


    private void deleteFromCoordinateArrays(int x, int y, Array<Integer> a){
        for (int i=0; i<a.size; i+=2){
            if (a.get(i)==x&&a.get(i+1)==y){
                a.removeIndex(i+1);
                a.removeIndex(i);
            }
        }
    }

    private boolean searchCoordinateArrays(int x, int y, Array<Integer> a){
        for (int i=0; i<a.size; i+=2){
            if (a.get(i)==x&&a.get(i+1)==y){
                return true;
            }
        }
        return false;
    }


}
