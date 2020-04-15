package it.fusefactory.processing.historyplot;

import processing.core.PApplet;
import processing.core.PVector;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class ProcessingHistoryPlot {
    static int DEFAULT_WIDTH = 160;
    static int DEFAULT_HEIGHT = 160;

    PApplet applet;

    private enum RangeMode{
        RANGE_MANUAL,
        RANGE_LOWER_FIXED,
        RANGE_AUTOMATIC
    };

    boolean         drawGuideValues;
    float			lowest, highest;
    float			manualLowest, manualHighest;
    RangeMode		rangeMode;
    boolean			drawBackground;
    String          varName;
    boolean			showNumericalInfo;
    boolean			respectBorders;
    boolean			drawGrid;
    boolean			shrinkBackInAutoRange;
    boolean			drawFromRight; //begin drawing graph from right instead of left
    boolean			drawTitle;
    boolean			scissor;

    int				index;
    int				addedCounter;

    int				precision;

    int	            count;
    int             MAX_HISTORY;

    int             lineColor;
    int			    bgColor;
    int			    gridColor;

    int				drawSkip;
    float			lineWidth;
    float			gridUnit;

    boolean			showSmoothedPlot;
    float			smoothValue; //average of the last plotted vals
    float			smoothFactor; //(0 1.0] >> 1.0 means no smoothing

    boolean plotNeedsRefresh;

    List<Float> values = new ArrayList<Float>();
    List<Float> smoothValues = new ArrayList<Float>();
    List<Float> horizontalGuides= new ArrayList<Float>();
    List<Integer> horizontalGuideColors= new ArrayList<Integer>();

    VboMesh gridMesh;
    VboMesh plotMesh;
    VboMesh smoothPlotMesh;

    Rectangle prevRect;

    public ProcessingHistoryPlot(PApplet applet, String varName, float maxHistory){
        this.applet = applet;

        this.gridMesh = new VboMesh(applet);
        this.gridMesh.setMode(MeshMode.Lines);

        this.plotMesh = new VboMesh(applet);
        this.plotMesh.setMode(MeshMode.LinesStrip);

        this.smoothPlotMesh = new VboMesh(applet);
        this.smoothPlotMesh.setMode(MeshMode.LinesStrip);

        this.varName = varName;
        this.MAX_HISTORY = (int) maxHistory;

        rangeMode = RangeMode.RANGE_AUTOMATIC;
        count = 1;
        precision = 2;
        lineWidth = 1.0f;
        drawSkip = 1;
        showNumericalInfo = true;
        respectBorders = true;
        drawTitle = true;
        drawBackground = true;

        bgColor = applet.color(0);
        gridColor = applet.color(255,16);
        drawGrid = true;
        shrinkBackInAutoRange = false;
        plotNeedsRefresh = true;

        gridUnit = 40;
        smoothFactor = 0.1f;
        smoothValue = 0;
        showSmoothedPlot = false;
        drawGuideValues = false;
        scissor = false;
        lineColor = applet.color(255,0,0);
        drawFromRight = false;
    }

    void setMaxHistory(int max){
        this.MAX_HISTORY = max;
    }

    void reset(){
        values.clear();
        smoothValues.clear();
        count = 0;
    }

    public void update(float newVal){
        if (count <= 1 && newVal == newVal/*nan filter*/){
            smoothValue = newVal;
        }

        count++;

        int skip = 1;

        //if((!manualRange || onlyLowestIsFixed) && shrinkBackInAutoRange){
        //if (!autoUpdate) skip = 1;	//if not doing this too fast, no need to skip range processing
        if ( count%skip == 0 ){
            lowest = Float.MAX_VALUE;
            highest = -Float.MIN_VALUE;
            for (int i = 0; i < values.size(); i += skip){
                Float val = values.get(i);
                if (val > highest) highest = val;
                if (val < lowest) lowest = val;
            }
            if (lowest == Float.MAX_VALUE) lowest = -1;
            if (highest == -Float.MIN_VALUE) highest = 1;
        }
        //}
        //if(!manualRange){
        if ( newVal > highest) highest = newVal;
        if ( newVal < lowest) lowest = newVal;
        //}

        values.add(newVal);

        if(showSmoothedPlot) {
            smoothValue = newVal * smoothFactor + smoothValue * (1.0f - smoothFactor);
            smoothValues.add(smoothValue);
        }

        while (values.size() > MAX_HISTORY){
            values.remove(0);
        }

        if(showSmoothedPlot) {
            while (smoothValues.size() > MAX_HISTORY){
                smoothValues.remove(0);
            }
        }

        plotNeedsRefresh = true;
    }

    public void refillGridMesh(int x, int y , int w, int h){

        gridMesh.getVertices().clear();

        int gridH = (int) gridUnit;
        double numLinesH =  Math.floor(h / gridH);
        gridMesh.setMode(MeshMode.Lines);
        for(int i = 0; i < numLinesH; i++){
            gridMesh.getVertices().add(new PVector(x, y + gridH * i));
            gridMesh.getVertices().add(new PVector(x + w,  y + gridH * i));
        }
        double numLinesW = Math.floor(w / gridH);
        for(int i = 0; i < numLinesW; i++){
            gridMesh.getVertices().add(new PVector((float) Math.floor(gridH * 0.5) + x + gridH * i, y));
            gridMesh.getVertices().add(new PVector((float) Math.floor(gridH * 0.5) + x + gridH * i, y + h));
        }
    }

    void refillPlotMesh(VboMesh vboMesh, List<Float> _vals, float x, float y , float w, float h){
        vboMesh.getVertices().clear();

        int start = 0;
        if (count >= MAX_HISTORY){
            start = drawSkip - (count) % (drawSkip);
        }

        for (int i =  start; i < _vals.size(); i+= drawSkip){
            vboMesh.getVertices().add(new PVector(i, _vals.get(i)));
        }
    }

    public void draw(int x, int y){
        this.draw(x, y, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public void draw(int x, int y, int w, int h){
        boolean needsMesh = false;
        Rectangle r = new Rectangle(x, y, w, h);

        if ( r != prevRect  || plotNeedsRefresh){
            needsMesh = true;
            plotNeedsRefresh = false;
        }

        float plotLow = 0;
        float plotHigh = 0;

        switch (rangeMode) {
            case RANGE_MANUAL: plotLow = manualLowest; plotHigh = manualHighest; break;
            case RANGE_LOWER_FIXED: plotLow = manualLowest; plotHigh = highest; break;
            case RANGE_AUTOMATIC: plotLow = lowest; plotHigh = highest; break;
        }

        boolean needsGrid = r != prevRect;

        applet.pushStyle();

        boolean haveData = true;
        if (values.size() == 0 ) haveData = false;

        if (drawBackground){
            applet.fill(bgColor);
            applet.rect(x, y, w, h);
        }

        if (drawGrid){
            if(needsGrid || gridMesh.getVertices().size() == 0){
                refillGridMesh(x, y, w, h);
            }
            applet.stroke(gridColor);
            applet.strokeWeight(1);
            gridMesh.draw();
        }
        String precisionString = "%." + precision + "f";

        applet.fill(lineColor);
        float cVal = 0;
        if(haveData) cVal = values.get(values.size()-1);
        if(drawTitle){

            String text = varName;
            if(haveData) text += " " + String.format(precisionString, cVal);

            applet.text(text, x + w - (text.length()) * 8  , y + 10);
        }

        if (showNumericalInfo){
            applet.fill(85);
            applet.text(String.format(precisionString, plotHigh), 1 + x, y + 10);
            applet.text(String.format(precisionString, plotLow), 1 + x, y + h - 10);
        }

        for(int i = 0; i < horizontalGuides.size(); i++){
            float myY = horizontalGuides.get(i);
            if (myY > plotLow && myY < plotHigh){ //TODO negative!
                float yy = applet.map( myY, plotLow, plotHigh, 0, h);
                if(drawGuideValues){
                    applet.fill(horizontalGuideColors.get(i), 50);
                    applet.text(String.format(precisionString, horizontalGuides.get(i)),10 + x, y + h - yy + 10 );
                }

//                applet.fill(horizontalGuides.get(i), 64);
//                applet.line(x, y + h - yy, x + w, y + h - yy );
            }
        }

        if (haveData){
//            applet.noFill();
            applet.strokeWeight(lineWidth);
            applet.fill(lineColor);
            applet.stroke(lineColor);

            if(needsMesh){
                refillPlotMesh(plotMesh, values, x, y, w, h);
                if (showSmoothedPlot){
                    refillPlotMesh(smoothPlotMesh, smoothValues, x, y, w, h);
                }
            }

            if(showSmoothedPlot){
                applet.fill(applet.red(lineColor) * 0.25f,
                            applet.green(lineColor) * 0.25f,
                            applet.blue(lineColor) * 0.25f,
                                applet.alpha(lineColor));
            }else{
                applet.fill(lineColor);
            }

            applet.pushMatrix();
//            if(scissor){
//                glEnable(GL_SCISSOR_TEST);
//                glScissor(x, ofGetViewportHeight() -y -h, w, h);
//            }
            if (respectBorders) h -= 12;
            applet.translate(x,y + h + (respectBorders ? 12 : 0) - 1);
            float plotValuesRange = plotHigh - plotLow;
            float yscale = plotValuesRange > 0.0f ? (h-1) / plotValuesRange : 1;
            if(drawFromRight){
                applet.translate(w, 0);
                applet.scale(-1, 1);
            }
            applet.scale((1.0f * w) / MAX_HISTORY, - yscale);
            applet.translate(0, -plotLow);

            plotMesh.draw();
            if (showSmoothedPlot){
                applet.fill(lineColor);
                smoothPlotMesh.draw();
            }
            applet.popMatrix();
//            ofFill();
        }
        applet.popStyle();
        prevRect = r;

    }

    public void setRange(float low, float high){
        rangeMode = RangeMode.RANGE_MANUAL;
        manualLowest = low;
        manualHighest = high;
    }

    public void addHorizontalGuide(float yval, int c){
        horizontalGuides.add(yval);
        plotNeedsRefresh = true;
        horizontalGuideColors.add(c);
    }

    public float getLowerRange(){
        switch (rangeMode) {
            case RANGE_MANUAL:
            case RANGE_LOWER_FIXED:
                return manualLowest;
            case RANGE_AUTOMATIC: return lowest;
        }
        return -1.0f;
    }


    public float getHigerRange(){
        switch (rangeMode) {
            case RANGE_MANUAL: return manualHighest;
            case RANGE_LOWER_FIXED:
            case RANGE_AUTOMATIC:
                return highest;
        }
        return 1.0f;
    }

    public void setColor(int c){ lineColor = c;}
    public void setBackgroundColor(int c){ bgColor = c;}
    public void setGridColor(int c){ gridColor = c;}
    public void setShowNumericalInfo(boolean show){ showNumericalInfo = show;}
    public void setRespectBorders(boolean respect){respectBorders = respect;}
    public void setDrawSkipVal(int skip){ drawSkip = skip; if (drawSkip <1) drawSkip = 1;} //draw evey n samples, might speed up drawing
    public void setLineWidth(float w){ lineWidth = w;}
    public void setDrawBackground(boolean d) { drawBackground = d;}
    public void setDrawTitle(boolean doit){drawTitle = doit;}
    public void setDrawGrid(boolean d) { drawGrid = d;}
    public void setGridUnit(float g){gridUnit = g;} //pixels
    public void setAutoRangeShrinksBack(boolean shrink){shrinkBackInAutoRange = shrink;};
    public void setCropToRect(boolean s){scissor = s;}
}

