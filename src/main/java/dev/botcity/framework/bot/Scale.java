package dev.botcity.framework.bot;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;

public class Scale {
	
	private Double x, y;
	
	public Scale() {
		GraphicsConfiguration config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
		AffineTransform affineTransform = config.getDefaultTransform();

		double scaleX = affineTransform.getScaleX();
		double scaleY = affineTransform.getScaleY();
		this.x = scaleX;
		this.y = scaleY;
	}
	
	public Double getScaleX() {
		return this.x;
	}
	
	public Double getScaleY() {
		return this.y;
	}
}
