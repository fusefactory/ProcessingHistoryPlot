import processing.core.PApplet;
import it.fusefactory.processinghistoryplot;
public class Example extends PApplet {
    public static void main(String[] args) {
        PApplet.main(new String[] { Example.class.getName() });
    }

    private ProcessingHistoryPlot plot;

    public void settings(){
        size(700, 400);
    }
    public void setup() {
        plot = new ProcessingHistoryPlot(this, "mouseY", 350);
//        plot.setRange(0, height);
        plot.addHorizontalGuide(height / 2, color(255, 0, 0));
        plot.setColor(color(0, 255, 0));
        plot.setShowNumericalInfo(true);  //show the current value and the scale in the plot
        plot.setRespectBorders(true);	   //dont let the plot draw on top of text

        plot.setLineWidth(1);				//plot line width
        plot.setBackgroundColor(color(0,220)); //custom bg color

        //custom grid setup
        plot.setDrawGrid(true);
        plot.setGridColor(color(30)); //grid lines color
        plot.setGridUnit(14);
        plot.setCropToRect(true);

    }
    public void draw() {
        background(32);

        if(mousePressed){
            plot.update(mouseY);
        }

        plot.draw(10, 10, 640, 240);

        text("press mouse button and drag", 10, 300);
        text(frameRate, 10, 540);
    }
}
