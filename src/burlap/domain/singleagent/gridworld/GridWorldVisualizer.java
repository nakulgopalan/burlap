package burlap.domain.singleagent.gridworld;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import burlap.oomdp.core.Domain;
import burlap.oomdp.core.ObjectInstance;
import burlap.oomdp.core.State;
import burlap.oomdp.visualizer.ObjectPainter;
import burlap.oomdp.visualizer.StaticPainter;
import burlap.oomdp.visualizer.Visualizer;



/**
 * Returns a visualizer for grid worlds in which walls are rendered as black squares, the agent is a red square and the goal is blue square. The size of the squares
 * scales to the size of the domain and the size of the canvas.
 * @author James MacGlashan
 *
 */
public class GridWorldVisualizer {

	
	/**
	 * Returns visualizer for a gird world domain with the provided wall map.
	 * @param d the domain of the grid world
	 * @param map the wall map matrix where 1s indicate a wall in that cell and 0s indicate it is clear of walls
	 * @return a grid world domain visualizer
	 */
	public static Visualizer getVisualizer(Domain d, int [][] map){
		
		Visualizer v = new Visualizer();
		
		v.addStaticPainter(new MapPainter(d, map));
		v.addObjectClassPainter(GridWorldDomain.CLASSLOCATION, new CellPainter(Color.blue, map));
		v.addObjectClassPainter(GridWorldDomain.CLASSAGENT, new CellPainter(Color.red, map));
		
		return v;
	}
	
	
	/**
	 * A static painter class for rendering the walls of the grid world as black squares.
	 * @author James MacGlashan
	 *
	 */
	public static class MapPainter implements StaticPainter{

		protected int 				dwidth;
		protected int 				dheight;
		protected int [][] 			map;
		
		
		/**
		 * Initializes for the domain and wall map
		 * @param domain the domain of the grid world
		 * @param map the wall map matrix where 1s indicate a wall in that cell and 0s indicate it is clear of walls
		 */
		public MapPainter(Domain domain, int [][] map) {
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
		}

		@Override
		public void paint(Graphics2D g2, State s, float cWidth, float cHeight) {
			
			//draw the walls; make them black
			g2.setColor(Color.black);
			
			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			//pass through each cell of the map and if it is a wall, draw it
			for(int i = 0; i < this.dwidth; i++){
				for(int j = 0; j < this.dheight; j++){
					
					if(this.map[i][j] == 1){
					
						float rx = i*width;
						float ry = cHeight - height - j*height;
					
						g2.fill(new Rectangle2D.Float(rx, ry, width, height));
						
					}
					
				}
			}
			
		}
		
		
	}
	
	
	/**
	 * A painter for a grid world cell which will fill the cell with a given color and where the cell position
	 * is indicated by the x and y attribute for the mapped object instance
	 * @author James MacGlashan
	 *
	 */
	public static class CellPainter implements ObjectPainter{

		protected Color			col;
		protected int			dwidth;
		protected int			dheight;
		protected int [][]		map;
		
		
		/**
		 * Initializes painter
		 * @param col the color to paint the cell
		 * @param map the wall map matrix where 1s indicate a wall in that cell and 0s indicate it is clear of walls
		 */
		public CellPainter(Color col, int [][] map) {
			this.col = col;
			this.dwidth = map.length;
			this.dheight = map[0].length;
			this.map = map;
		}

		@Override
		public void paintObject(Graphics2D g2, State s, ObjectInstance ob, float cWidth, float cHeight) {
			
			
			//set the color of the object
			g2.setColor(this.col);
			
			float domainXScale = this.dwidth;
			float domainYScale = this.dheight;
			
			//determine then normalized width
			float width = (1.0f / domainXScale) * cWidth;
			float height = (1.0f / domainYScale) * cHeight;
			
			float rx = ob.getDiscValForAttribute(GridWorldDomain.ATTX)*width;
			float ry = cHeight - height - ob.getDiscValForAttribute(GridWorldDomain.ATTY)*height;
			
			g2.fill(new Rectangle2D.Float(rx, ry, width, height));
			
		}
		
		
		
		
	}
	
	
}
