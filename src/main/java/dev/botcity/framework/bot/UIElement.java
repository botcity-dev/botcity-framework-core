package dev.botcity.framework.bot;

import org.marvinproject.framework.image.MarvinImage;

/**
 * UI element representation model.
 * @author Gabriel Ambrósio Archanjo
 *
 */
public class UIElement {

	private MarvinImage image;
	private Integer 	x,
						y,
						width,
						height;
	
	public MarvinImage getImage() {
		return image;
	}
	public void setImage(MarvinImage image) {
		this.image = image;
		this.width = image.getWidth();
		this.height = image.getHeight();
	}
	public Integer getX() {
		return x;
	}
	public void setX(Integer x) {
		this.x = x;
	}
	public Integer getY() {
		return y;
	}
	public void setY(Integer y) {
		this.y = y;
	}
	public Integer getWidth() {
		return width;
	}
	public Integer getHeight() {
		return height;
	}
}
