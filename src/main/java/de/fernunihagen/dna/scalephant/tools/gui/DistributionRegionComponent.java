package de.fernunihagen.dna.scalephant.tools.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import de.fernunihagen.dna.scalephant.distribution.DistributionRegion;
import de.fernunihagen.dna.scalephant.distribution.membership.DistributedInstance;
import de.fernunihagen.dna.scalephant.storage.entity.BoundingBox;
import de.fernunihagen.dna.scalephant.storage.entity.FloatInterval;

public class DistributionRegionComponent {
	
	/**
	 * The x position of the root element
	 */
	protected final int posRootX;
	
	/**
	 * The y position of the root element
	 */
	protected final int posRootY;
	
	/**
	 * The x offset of a child (- if left child / + if right child)
	 */
	protected final static int LEFT_RIGHT_OFFSET = 60;
	
	/**
	 * The distance between two levels
	 */
	protected final static int LEVEL_DISTANCE = 100;

	/**
	 * The hight of the box
	 */
	protected final static int HEIGHT = 50;
	
	/**
	 * The width of the box
	 */
	protected final static int WIDTH = 100;
	
	/**
	 * The distribution region to paint
	 */
	protected final DistributionRegion distributionRegion;

	public DistributionRegionComponent(DistributionRegion distributionRegion, final int posRootX, final int posRootY) {
		super();
		this.distributionRegion = distributionRegion;
		this.posRootX = posRootX;
		this.posRootY = posRootY;
	}
	
	/**
	 * Calculate the x offset of the component
	 * @return
	 */
	protected int calculateXOffset() {
		int offset = posRootX;
		
		DistributionRegion level = distributionRegion;
		
		while(level.getParent() != null) {
			
			if(level.isLeftChild()) {
				offset = offset - calculateLevelXOffset(level.getLevel());
			} else {
				offset = offset + calculateLevelXOffset(level.getLevel());
			}
			
			level = level.getParent();
		}
		
		return offset;
	}
	
	/**
	 * Calculate the x offset for a given level
	 * @param level
	 * @return
	 */
	protected int calculateLevelXOffset(final int level) {
		return (LEFT_RIGHT_OFFSET * (distributionRegion.getTotalLevel() - level));
	}
	
	/**
	 * Calculate the Y offset
	 * @return
	 */
	protected int calculateYOffset() {
		return (distributionRegion.getLevel() * LEVEL_DISTANCE) + posRootY;
	}
	
	/**
	 * Is the mouse over this component?
	 * @return
	 */
	protected boolean isMouseOver(final MouseEvent event) {
		final int xOffset = calculateXOffset();
		final int yOffset = calculateYOffset();
				
		if(event.getX() >= xOffset && event.getX() <= xOffset + WIDTH) {
			if(event.getY() >= yOffset && event.getY() <= yOffset + HEIGHT) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Draw this component
	 * @param g
	 */
	public void drawComponent(final Graphics2D g) {
		final int xOffset = calculateXOffset();
		final int yOffset = calculateYOffset();
		
		// Draw the node
		final Color oldColor = g.getColor();
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(xOffset, yOffset, WIDTH, HEIGHT);
		g.setColor(oldColor);
		g.drawRect(xOffset, yOffset, WIDTH, HEIGHT);

		// Write node text
		String nodeText = Float.toString(distributionRegion.getSplit());
		if(distributionRegion.isLeafRegion()) {
			nodeText = "-";
		}
		
		final Rectangle2D bounds = g.getFontMetrics().getStringBounds(nodeText, g);
		int stringWidth = (int) bounds.getWidth();
		
		g.drawString(nodeText, xOffset + (WIDTH / 2) - (stringWidth / 2), yOffset + HEIGHT / 2);

		// Draw the line to the parent node
		drawParentNodeLine(g, xOffset, yOffset);
	}

	/**
	 * Draw the line to the parent node
	 * @param g
	 * @param xOffset
	 * @param yOffset
	 */
	protected void drawParentNodeLine(final Graphics2D g, final int xOffset, final int yOffset) {
		
		// The root element don't have a parent
		if(distributionRegion.getParent() == null) {
			return;
		}
		
		int lineEndX = xOffset + (WIDTH / 2);
		int lineEndY = yOffset - LEVEL_DISTANCE + HEIGHT;
		
		if(distributionRegion.isLeftChild()) {
			lineEndX = lineEndX + calculateLevelXOffset(distributionRegion.getLevel());
		} else {
			lineEndX = lineEndX - calculateLevelXOffset(distributionRegion.getLevel());
		}
		
		g.drawLine(xOffset + (WIDTH / 2), yOffset, lineEndX, lineEndY);
	}

	/**
	 * Get the tooltip text
	 * @return
	 */
	public String getToolTipText() {
		
		final BoundingBox boundingBox = distributionRegion.getConveringBox();
		final StringBuilder sb = new StringBuilder("<html>");
		
		for(int i = 0; i < boundingBox.getDimension(); i++) {
			final FloatInterval floatInterval = boundingBox.getIntervalForDimension(i);
			sb.append("Dimension: " + i + " ");
			sb.append(intervalToString(floatInterval));
			sb.append("<br>");
		}
		
		sb.append("Systems: ");
		
		for(Iterator<DistributedInstance> iter = distributionRegion.getSystems().iterator(); iter.hasNext(); ) {
			sb.append(iter.next().toGUIString());
			if(iter.hasNext()) {
				sb.append(", ");
			}
		}
		
		sb.append("<br>");
		sb.append("Nameprefix: " + distributionRegion.getNameprefix());
		sb.append("<br>");
		sb.append("State: " + distributionRegion.getState());
		
		sb.append("</html>");
		return sb.toString();
	}

	/**
	 * Convert the given interval into a string representation
	 * @param floatInterval
	 * @return
	 */
	protected String intervalToString(final FloatInterval floatInterval) {
		final StringBuilder sb = new StringBuilder();
		
		if(floatInterval.isBeginIncluded()) {
			sb.append("[");
		} else {
			sb.append("(");
		}
		
		if(floatInterval.getBegin() == BoundingBox.MIN_VALUE) {
			sb.append("min");
		} else {
			sb.append(floatInterval.getBegin());
		}
		
		sb.append(",");
		
		if(floatInterval.getEnd() == BoundingBox.MAX_VALUE) {
			sb.append("max");
		} else {
			sb.append(floatInterval.getEnd());
		}
					
		if(floatInterval.isEndIncluded()) {
			sb.append("]");
		} else {
			sb.append(")");
		}
		
		return sb.toString();
	}

}