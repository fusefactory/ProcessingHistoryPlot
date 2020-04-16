package it.fusefactory.processing.historyplot;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

public class VboMesh {

    private List<PVector> vertices = new ArrayList<PVector>();
    private PApplet applet;
    private int mode = PApplet.LINES;

    public VboMesh(PApplet applet){
        this.applet = applet;
    }

    public void draw(){

        applet.beginShape(mode);

        for(PVector p : vertices){
            applet.vertex(p.x, p.y);
        }
        applet.endShape();
    }

    public void setMode(int mode){
        this.mode = mode;
    }
    public List<PVector> getVertices(){return  vertices;}
}
