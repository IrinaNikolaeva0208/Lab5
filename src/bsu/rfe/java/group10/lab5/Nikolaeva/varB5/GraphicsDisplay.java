package bsu.rfe.java.group10.lab5.Nikolaeva.varB5;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPanel;

public class GraphicsDisplay extends JPanel {
    // Список координат точек для построения графика
    private ArrayList<Double[]> graphicsData;
    private ArrayList<Double[]> originalData;

    private int selectedMarker = -1;
    // Границы диапазона пространства, подлежащего отображению
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double scaleX;
    private double scaleY;
    private double[][] viewport = new double[2][2];
    private ArrayList<double[][]> undoHistory = new ArrayList<>(5);

    // Флаговые переменные, задающие правила отображения графика
    private boolean showAxis = true;
    private boolean showMarkers = true;
    private boolean showExtremes = true;

    private Font axisFont;
    private Font labelsFont;
    private Font extremesFont;
    private BasicStroke axisStroke;
    private BasicStroke markerStroke;
    private BasicStroke gridStroke;
    private BasicStroke selectionStroke;

    private static DecimalFormat formatter = (DecimalFormat)NumberFormat.getInstance();
    private boolean scaleMode = false;
    private boolean zoomed = false;
    private double[] originalPoint = new double[2];
    private java.awt.geom.Rectangle2D.Double selectionRect = new java.awt.geom.Rectangle2D.Double();

    public GraphicsDisplay() {
        this.setBackground(Color.WHITE);
        this.axisStroke = new BasicStroke(2.0F, 0, 1, 10.0F, null, 0.0F);
        this.markerStroke = new BasicStroke(1.5F, 0, 1, 5.0F, new float[] {40,10,20,10,10,10,20,10,40,10}, 0.0F);
        this.selectionStroke = new BasicStroke(1.0F, 0, 0, 10.0F, new float[]{10.0F, 10.0F}, 0.0F);


        this.gridStroke = new BasicStroke(0.5F, 0, 1, 5.0F, new float[]{5.0F, 5.0F}, 2.0F);
        this.axisFont = new Font("Serif", 1, 36);
        this.labelsFont = new Font("Serif", 0, 10);
        extremesFont = new Font("Serif", Font.ITALIC, 10);
        this.addMouseMotionListener(new GraphicsDisplay.MouseMotionHandler());
        this.addMouseListener(new GraphicsDisplay.MouseHandler());
    }

    public void showGraphics(ArrayList<Double[]> graphicsData) {
        this.graphicsData = graphicsData;
        this.originalData = new ArrayList<>(graphicsData.size());
        Iterator var3 = graphicsData.iterator();

        while(var3.hasNext()){
            Double[] point = (Double[])var3.next();
            Double[] newPoint = new Double[]{point[0], point[1]};
            this.originalData.add(newPoint);
        }

        this.minX = (graphicsData.get(0))[0];
        this.maxX = (graphicsData.get(graphicsData.size() - 1))[0];
        this.minY = (graphicsData.get(0))[1];
        this.maxY = this.minY;

        for(int i = 1; i < graphicsData.size(); ++i) {
            if ((graphicsData.get(i))[1] < this.minY) {
                this.minY = (graphicsData.get(i))[1];
            }

            if ((graphicsData.get(i))[1] > this.maxY) {
                this.maxY = (graphicsData.get(i))[1];
            }
        }

        this.zoomToRegion(this.minX, this.maxY, this.maxX, this.minY);
    }

    public void zoomToRegion(double x1, double y1, double x2, double y2) {
        this.viewport[0][0] = x1;
        this.viewport[0][1] = y1;
        this.viewport[1][0] = x2;
        this.viewport[1][1] = y2;
        this.repaint();
    }

    public void setShowAxis(boolean showAxis) {
        this.showAxis = showAxis;
        this.repaint();
    }

    public void setShowMarkers(boolean showMarkers) {
        this.showMarkers = showMarkers;
        this.repaint();
    }

    public void setShowExtremes(boolean showExtremes) {
		this.showExtremes = showExtremes;
		repaint();
	}
    
    protected java.awt.geom.Point2D.Double xyToPoint(double x, double y) {
        double deltaX = x - this.viewport[0][0];
        double deltaY = this.viewport[0][1] - y;
        return new java.awt.geom.Point2D.Double(deltaX * this.scaleX, deltaY * this.scaleY);
    }

    protected double[] translatePointToXY(int x, int y) {
        return new double[]{this.viewport[0][0] + (double)x / this.scaleX, this.viewport[0][1] - (double)y / this.scaleY};
    }

	protected void signExtremes (Graphics2D canvas) {
	canvas.setColor(Color.BLACK);
	canvas.setFont(extremesFont);
	FontRenderContext context = canvas.getFontRenderContext();
	for (int i=0; i<graphicsData.size(); i++) {
		if(i!=0 && i!=graphicsData.size()-1)
			if (graphicsData.get(i)[1]<graphicsData.get(i+1)[1] && graphicsData.get(i)[1]<graphicsData.get(i-1)[1]) {
				Rectangle2D bounds = extremesFont.getStringBounds("Ymin = "+graphicsData.get(i)[1].toString(), context);
				java.awt.geom.Point2D.Double labelPos = xyToPoint(graphicsData.get(i)[0], graphicsData.get(i)[1]);
				if(graphicsData.get(i)[1] == minY)
					canvas.drawString("Ymin = "+graphicsData.get(i)[1].toString(), (float)labelPos.getX(), (float)(labelPos.getY() - bounds.getY()- 20));
				else canvas.drawString("Ymin = "+graphicsData.get(i)[1].toString(), (float)labelPos.getX(), (float)(labelPos.getY() - bounds.getY()+20));
			}
			else if (graphicsData.get(i)[1]>graphicsData.get(i+1)[1] && graphicsData.get(i)[1]>graphicsData.get(i-1)[1]) {
				Rectangle2D bounds = extremesFont.getStringBounds("Ymax = "+graphicsData.get(i)[1].toString(), context);
				java.awt.geom.Point2D.Double labelPos = xyToPoint(graphicsData.get(i)[0], graphicsData.get(i)[1]);
				if(graphicsData.get(i)[1] == maxY)
					canvas.drawString("Ymax = "+graphicsData.get(i)[1].toString(), (float)labelPos.getX()-20, (float)(labelPos.getY() - bounds.getY()+20));
				else canvas.drawString("Ymax = "+graphicsData.get(i)[1].toString(), (float)labelPos.getX()-20, (float)(labelPos.getY() - bounds.getY()-20));
			}
	}
}

 

    protected void paintGraphics(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.RED);
        Double currentX = null;
        Double currentY = null;
        Iterator var5 = this.graphicsData.iterator();

        while(var5.hasNext()) {
            Double[] point = (Double[])var5.next();
            if (point[0] >= this.viewport[0][0] && point[1] <= this.viewport[0][1] && point[0] <= this.viewport[1][0] && point[1] >= this.viewport[1][1]) {
                if (currentX != null && currentY != null) {
                    canvas.draw(new java.awt.geom.Line2D.Double(this.xyToPoint(currentX, currentY), this.xyToPoint(point[0], point[1])));
                }

                currentX = point[0];
                currentY = point[1];
            }
        }

    }

    protected void paintAxis(Graphics2D canvas) {
        canvas.setStroke(this.axisStroke);
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.axisFont);
        FontRenderContext context = canvas.getFontRenderContext();
        Rectangle2D bounds;
        java.awt.geom.Point2D.Double labelPos;
        if (this.viewport[0][0] <= 0.0D && this.viewport[1][0] >= 0.0D) {
            canvas.draw(new java.awt.geom.Line2D.Double(this.xyToPoint(0.0D, this.viewport[0][1]), this.xyToPoint(0.0D, this.viewport[1][1])));
            canvas.draw(new java.awt.geom.Line2D.Double(this.xyToPoint(-(this.viewport[1][0] - this.viewport[0][0]) * 0.0025D, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015D), this.xyToPoint(0.0D, this.viewport[0][1])));
            canvas.draw(new java.awt.geom.Line2D.Double(this.xyToPoint((this.viewport[1][0] - this.viewport[0][0]) * 0.0025D, this.viewport[0][1] - (this.viewport[0][1] - this.viewport[1][1]) * 0.015D), this.xyToPoint(0.0D, this.viewport[0][1])));
            bounds = this.axisFont.getStringBounds("y", context);
            labelPos = this.xyToPoint(0.0D, this.viewport[0][1]);
            canvas.drawString("y", (float)labelPos.x + 10.0F, (float)(labelPos.y + bounds.getHeight() / 2.0D));
        }

        if (this.viewport[1][1] <= 0.0D && this.viewport[0][1] >= 0.0D) {
            canvas.draw(new java.awt.geom.Line2D.Double(this.xyToPoint(this.viewport[0][0], 0.0D), this.xyToPoint(this.viewport[1][0], 0.0D)));
            canvas.draw(new java.awt.geom.Line2D.Double(this.xyToPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.0D, (this.viewport[0][1] - this.viewport[1][1]) * 0.005D), this.xyToPoint(this.viewport[1][0], 0.0D)));
            canvas.draw(new java.awt.geom.Line2D.Double(this.xyToPoint(this.viewport[1][0] - (this.viewport[1][0] - this.viewport[0][0]) * 0.01D, -(this.viewport[0][1] - this.viewport[1][1]) * 0.005D), this.xyToPoint(this.viewport[1][0], 0.0D)));
            bounds = this.axisFont.getStringBounds("x", context);
            labelPos = this.xyToPoint(this.viewport[1][0], 0.0D);
            canvas.drawString("x", (float)(labelPos.x - bounds.getWidth() - 10.0D), (float)(labelPos.y - bounds.getHeight() / 2.0D));
        }

    }

    protected void paintMarkers(Graphics2D canvas) {
        canvas.setStroke(this.markerStroke);
        canvas.setColor(Color.GREEN);
        canvas.setPaint(Color.GREEN);
        GeneralPath lastMarker = null;
        int i = -1;
        Iterator var5 = this.graphicsData.iterator();

        while(var5.hasNext()) {
            Double[] point = (Double[])var5.next();
            i++;
            if (consistsOfEven(point)) {
                canvas.setColor(Color.BLUE);
            } else {
                canvas.setColor(Color.RED);
            }

            GeneralPath marker = new GeneralPath();
            java.awt.geom.Point2D.Double center = this.xyToPoint(point[0], point[1]);
            marker.moveTo(center.getX() - 5, center.getY());
            marker.lineTo(marker.getCurrentPoint().getX()+5, marker.getCurrentPoint().getY() + 5);
            marker.lineTo(marker.getCurrentPoint().getX() +5, marker.getCurrentPoint().getY() -5);
            marker.lineTo(marker.getCurrentPoint().getX() -5, marker.getCurrentPoint().getY() -5);
            marker.lineTo(marker.getCurrentPoint().getX() -5, marker.getCurrentPoint().getY()+5);
            if (i == this.selectedMarker) {
                lastMarker = marker;
            } else {
                canvas.draw(marker);
            }
        }

        if (lastMarker != null) {
            canvas.setColor(Color.BLUE);
            canvas.setPaint(Color.BLUE);
            canvas.draw(lastMarker);
            canvas.fill(lastMarker);
        }

    }
    
	private boolean consistsOfEven(Double[] point) {
		int n = point[1].intValue();
		while (n!=0) {
			if(n%2!=0) 
				return false;
			n/=10;
		}
		return true;
}
	
	 private void paintGrid(Graphics2D canvas) {
	      canvas.setStroke(this.gridStroke);
	      canvas.setColor(Color.GRAY);
	      double pos = this.viewport[0][0];

	      double step;
	      for(step = (this.viewport[1][0] - this.viewport[0][0]) / 10.0D; pos < this.viewport[1][0]; pos += step) {
	         canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(pos, this.viewport[0][1]), this.translateXYtoPoint(pos, this.viewport[1][1])));
	      }

	      canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0], this.viewport[1][1])));
	      pos = this.viewport[1][1];

	      for(step = (this.viewport[0][1] - this.viewport[1][1]) / 10.0D; pos < this.viewport[0][1]; pos += step) {
	         canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], pos), this.translateXYtoPoint(this.viewport[1][0], pos)));
	      }

	      canvas.draw(new java.awt.geom.Line2D.Double(this.translateXYtoPoint(this.viewport[0][0], this.viewport[0][1]), this.translateXYtoPoint(this.viewport[1][0], this.viewport[0][1])));
	   }

    private void paintLabels(Graphics2D canvas) {
        canvas.setColor(Color.BLACK);
        canvas.setFont(this.labelsFont);
        FontRenderContext context = canvas.getFontRenderContext();
        double labelYPos;
        if (this.viewport[1][1] < 0.0D && this.viewport[0][1] > 0.0D) {
            labelYPos = 0.0D;
        } else {
            labelYPos = this.viewport[1][1];
        }

        double labelXPos;
        if (this.viewport[0][0] < 0.0D && this.viewport[1][0] > 0.0D) {
            labelXPos = 0.0D;
        } else {
            labelXPos = this.viewport[0][0];
        }

        double pos = this.viewport[0][0];

        double step;
        java.awt.geom.Point2D.Double point;
        String label;
        Rectangle2D bounds;
        for(step = (this.viewport[1][0] - this.viewport[0][0]) / 10.0D; pos < this.viewport[1][0]; pos += step) {
            point = this.xyToPoint(pos, labelYPos);
            label = formatter.format(pos);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }

        pos = this.viewport[1][1];

        for(step = (this.viewport[0][1] - this.viewport[1][1]) / 10.0D; pos < this.viewport[0][1]; pos += step) {
            point = this.xyToPoint(labelXPos, pos);
            label = formatter.format(pos);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }

        if (this.selectedMarker >= 0) {
            point = this.xyToPoint((this.graphicsData.get(this.selectedMarker))[0], (this.graphicsData.get(this.selectedMarker))[1]);
            label = "X=" + formatter.format((this.graphicsData.get(this.selectedMarker))[0]) + ", Y=" + formatter.format((this.graphicsData.get(this.selectedMarker))[1]);
            bounds = this.labelsFont.getStringBounds(label, context);
            canvas.setColor(Color.BLACK);
            canvas.drawString(label, (float)(point.getX() + 5.0D), (float)(point.getY() - bounds.getHeight()));
        }

    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.scaleX = this.getSize().getWidth() / (this.viewport[1][0] - this.viewport[0][0]);
        this.scaleY = this.getSize().getHeight() / (this.viewport[0][1] - this.viewport[1][1]);
        if (this.graphicsData != null && this.graphicsData.size() != 0) {
            Graphics2D canvas = (Graphics2D)g;
            Stroke oldStroke = canvas.getStroke();
            Color oldColor = canvas.getColor();
            Font oldFont = canvas.getFont();
            Paint oldPaint = canvas.getPaint();

            if (this.showAxis) {
                this.paintAxis(canvas);
                this.paintLabels(canvas);
            }

            this.paintGraphics(canvas);
            if (this.showMarkers) 
                this.paintMarkers(canvas);
            
            if (this.showExtremes) 
                this.signExtremes(canvas);
            this.paintGrid(canvas);
            this.paintSelection(canvas);
            canvas.setFont(oldFont);
            canvas.setPaint(oldPaint);
            canvas.setColor(oldColor);
            canvas.setStroke(oldStroke);
        }
    }

    private void paintSelection(Graphics2D canvas) {
        if (this.scaleMode) {
            canvas.setStroke(this.selectionStroke);
            canvas.setColor(Color.BLACK);
            canvas.draw(this.selectionRect);
        }
    }

    protected java.awt.geom.Point2D.Double translateXYtoPoint(double x, double y) {
        double deltaX = x - this.viewport[0][0];
        double deltaY = this.viewport[0][1] - y;
        return new java.awt.geom.Point2D.Double(deltaX * this.scaleX, deltaY * this.scaleY);
     }


    protected int findSelectedPoint(int x, int y) {
        if (this.graphicsData == null) {
            return -1;
        } else {
            int pos = 0;

            for(Iterator var5 = this.graphicsData.iterator(); var5.hasNext(); ++pos) {
                Double[] point = (Double[])var5.next();
                java.awt.geom.Point2D.Double screenPoint = this.xyToPoint(point[0], point[1]);
                double distance = (screenPoint.getX() - (double)x) * (screenPoint.getX() - (double)x) + (screenPoint.getY() - (double)y) * (screenPoint.getY() - (double)y);
                if (distance < 100.0D) {
                    return pos;
                }
            }
            return -1;
        }
    }
    
    public class MouseHandler extends MouseAdapter {
    
    	public MouseHandler() {
    		 
        }
    
        public void mouseClicked(MouseEvent ev) {
 
    	}
        
        public void mousePressed(MouseEvent ev) {
        
        }
    
        public void mouseReleased(MouseEvent ev) {
        	
        }
        
    }
    
    public class MouseMotionHandler implements MouseMotionListener {

    	public MouseMotionHandler() {
    	       
        }
    	
       public void mouseDragged(MouseEvent ev) {
    	   
       }
       
       public void mouseMoved(MouseEvent ev) {
    	   
       }
    }
}


