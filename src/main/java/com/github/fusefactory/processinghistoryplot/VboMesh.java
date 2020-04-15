package com.github.fusefactory.processinghistoryplot;

import processing.core.PApplet;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;

enum MeshMode{
    Lines,
    LinesStrip,
    Points;
}

public class VboMesh {

    private List<PVector> vertices = new ArrayList<PVector>();
    private PApplet applet;
    private MeshMode mode = MeshMode.Lines;

    public VboMesh(PApplet applet){
        this.applet = applet;
    }

    public void draw(){
        if(mode == MeshMode.Points){
            for(PVector p : vertices){
                applet.ellipse(p.x, p.y, 1, 1);
            }
        }
        else if(mode == MeshMode.Lines){
            for(int i = 0; i < vertices.size()-1; i+=2) {
                PVector p1 = vertices.get(i);
                PVector p2 = vertices.get(i+1);

                applet.line(p1.x, p1.y, p2.x, p2.y);
            }
        }
        else if (mode == MeshMode.LinesStrip) {
            for (int i = 0; i < vertices.size() - 1; i++) {
                PVector p1 = vertices.get(i);
                PVector p2 = vertices.get(i + 1);

                applet.line(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    public void setMode(MeshMode mode){
        this.mode = mode;
    }
    public List<PVector> getVertices(){return  vertices;}
}
