package dev.botcity.framework.bot;
import static org.marvinproject.plugins.collection.MarvinPluginCollection.crop;
import static org.marvinproject.plugins.collection.MarvinPluginCollection.thresholding;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.marvinproject.framework.image.MarvinImage;
import org.marvinproject.framework.image.MarvinSegment;
import org.marvinproject.framework.io.MarvinImageIO;
import org.marvinproject.framework.plugin.MarvinImagePlugin;
import org.marvinproject.plugins.image.transform.flip.Flip;

/**
 * Provides a robot interface to operate desktop applications.
 * 
 * @author Gabriel Ambrósio Archanjo
 */
public class DesktopBot {

	private Robot 						robot;
	private Integer 					x,
										y;
	
	private MarvinImage 				screen,
										visualElem;
	
	private UIElement					lastElement = new UIElement();
	
	private MarvinImagePlugin 			flip;
	
	private boolean 					debug=false;
	
	private int 						sleepAfterAction=300;
	
	private double						colorSensibility = 0.04;
	
	private ClassLoader					resourceClassLoader;
	
	private Map<String, MarvinImage>	mapImages;
	
	public DesktopBot() {
		try {
			robot = new Robot();
			mapImages = new HashMap<String, MarvinImage>();
		} catch(Exception e) {
			e.printStackTrace();
		}
		screen = new MarvinImage(1,1);
		
		flip = new Flip();
		flip.load();
		flip.setAttribute("flip", "vertical");
	}
	
	public void enableDebug(){
		this.debug = true;
	}
	
	public void setColorSensibility(double colorSensibility) {
		this.colorSensibility = colorSensibility;
	}
	
	public double getColorSensibility() {
		return this.colorSensibility;
	}
	
	/**
	 * Set classloader for loading resources exported in JAR files.
	 * @param classloader
	 */
	public void setResourceClassLoader(ClassLoader classloader) {
		this.resourceClassLoader = classloader;
	}
	
	/**
	 * Add image of UI element to be recognized in automation processes. Check method find() and findText() to recognize such elements.
	 * @param label
	 * @param path
	 * @throws IOException
	 */
	public void addImage(String label, String path) throws IOException {
		File f = new File(path);
		
		// file outside jar?
		if(f.exists())
			mapImages.put(label, MarvinImageIO.loadImage(path));
		else {
			if(this.resourceClassLoader != null) {
				URL url = this.resourceClassLoader.getResource(path);
				if(url != null) {
					ImageIcon img = new ImageIcon(url);
					mapImages.put(label, new MarvinImage(toBufferedImage(img.getImage())));
				} else {
					throw new IOException("Image File not found! Label: "+label+", path:"+path);
				}
			}
		}
	}
	
	/**
	 * Add image of UI element to be recognized in automation processes. Check method find() and findText() to recognize such elements.
	 * @param label
	 * @param path
	 * @throws IOException
	 */
	public void addImage(String label, MarvinImage image) {
		mapImages.put(label, image);
	}
	
	private static BufferedImage toBufferedImage(Image img)	{
	    if (img instanceof BufferedImage)
	    {
	        return (BufferedImage) img;
	    }
	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();
	    // Return the buffered image
	    return bimage;
	}
	
	private MarvinImage getImageFromMap(String label) {
		return mapImages.get(label);
	}
	
	public Robot getRobot() {
		return this.robot;
	}
	
	/**
	 * Returns the last recognized UI element. In other words, the last element found by find() and findText()
	 * @return
	 */
	public UIElement getLastElement() {
		return this.lastElement;
	}
	
	/**
	 * Command line execution used to run commands or start applications.
	 * @param command
	 * @throws IOException
	 */
	public void exec(String command) throws IOException {
		Runtime.getRuntime().exec(command);
	}
	
	/**
	 * Invoke the default browser passing a URL
	 * @param uri
	 * @throws IOException
	 */
	public void browse(String uri) throws IOException {
		try {
			Desktop.getDesktop().browse(new URI(uri));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public boolean clickOn(String elementId){
	    return clickOn(getImageFromMap(elementId));
	}
	
	public boolean clickOn(MarvinImage visualElem) {
		screenshot();
		Point p = getElementCoordsCentered(visualElem, 0.95, false);
		if(p != null) {
			mouseMove(p.x, p.y);
			robot.mousePress(InputEvent.BUTTON1_MASK);
			robot.mouseRelease(InputEvent.BUTTON1_MASK);
			
			this.x = p.x;
			this.y = p.y;
			return true;
		}
		return false;
	}
	
	public Integer getLastX() {
		return this.x;
	}
	
	public Integer getLastY() {
		return this.y;
	}
	
	
	static int id=0;
//	public boolean findUntil(String elementImage, int maxWaitingTime) {
//		visualElem = MarvinImageIO.loadImage(elementImage);
//		return findUntil(visualElem, maxWaitingTime);
//	}
	
	/**
	 * Find a text element in the UI. Text elements are processed in black and white. Therefore, avoid using this method for texts in colored backgrounds.
	 * @param elementId 		element to be found
	 * @param maxWaitingTime 	maximum time for searching the element on the UI
	 * @param best 				return the best element? Search in the entire UI. If false, returns the first element that matches the criteria.
	 * @return					true if the UI element was found, false otherwise
	 */
	public boolean findText(String elementId,int maxWaitingTime, boolean best) {
		return findText(elementId, getImageFromMap(elementId), null, maxWaitingTime, best);
	}
	
	/**
	 * Find a text element in the UI. Text elements are processed in black and white. Therefore, avoid using this method for texts in colored backgrounds.
	 * @param elementId 		element to be found
	 * @param maxWaitingTime 	maximum time for searching the element on the UI
	 * @return					true if the UI element was found, false otherwise
	 */
	public boolean findText(String elementId,int maxWaitingTime) {
		return findText(elementId, getImageFromMap(elementId), null, maxWaitingTime);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, int maxWaitingTime, boolean best) {
		return findText(elementId, visualElem, null, maxWaitingTime, best);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, int maxWaitingTime) {
		return findText(elementId, visualElem, null, maxWaitingTime);
	}
	
	/**
	 * Find a text element in the UI. Text elements are processed in black and white. Therefore, avoid using this method for texts in colored backgrounds.
	 * @param elementId 		element to be found
	 * @param threshold 		grayscale threshold for black and white processing
	 * @param maxWaitingTime 	maximum time for searching the element on the UI
	 * @param best 				Search in the entire UI for the best match or returns the first element that matches the criteria.
	 * @return					true if the UI element was found, false otherwise
	 */
	public boolean findText(String elementId, Integer threshold, int maxWaitingTime, boolean best) {
		return findText(elementId, getImageFromMap(elementId), threshold, maxWaitingTime, best);
	}
	
	/**
	 * Find a text element in the UI. Text elements are processed in black and white. Therefore, avoid using this method for texts in colored backgrounds.
	 * @param elementId 		element to be found
	 * @param threshold 		grayscale threshold for black and white processing
	 * @param maxWaitingTime 	maximum time for searching the element on the UI
	 * @return					true if the UI element was found, false otherwise
	 */
	public boolean findText(String elementId, Integer threshold, int maxWaitingTime) {
		return findText(elementId, getImageFromMap(elementId), threshold, maxWaitingTime);
	}
	
	/**
	 * Find a text element in the UI. Text elements are processed in black and white. Therefore, avoid using this method for texts in colored backgrounds.
	 * @param elementId 		element to be found
	 * @param threshold 		grayscale threshold for black and white processing
	 * @param matching 			minimum score (0.0 to 1.0) to consider a match in the element image recognition process.
	 * @param maxWaitingTime 	maximum time for searching the element on the UI
	 * @param best 				Search in the entire UI for the best match or returns the first element that matches the criteria.
	 * @return					true if the UI element was found, false otherwise
	 */
	public boolean findText(String elementId, Integer threshold, double matching, int maxWaitingTime, boolean best) {
		return findUntil(elementId, getImageFromMap(elementId), threshold, matching, maxWaitingTime, best);
	}
	
	/**
	 * Find a text element in the UI. Text elements are processed in black and white. Therefore, avoid using this method for texts in colored backgrounds.
	 * @param elementId 		element to be found
	 * @param threshold 		grayscale threshold for black and white processing
	 * @param matching 			minimum score (0.0 to 1.0) to consider a match in the element image recognition process.
	 * @param maxWaitingTime 	maximum time for searching the element on the UI
	 * @return					true if the UI element was found, false otherwise
	 */
	public boolean findText(String elementId, Integer threshold, double matching, int maxWaitingTime) {
		return findUntil(elementId, getImageFromMap(elementId), threshold, matching, maxWaitingTime);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, Integer threshold, double matching, int maxWaitingTime, boolean best) {
		return findUntil(elementId, visualElem, threshold, matching, maxWaitingTime, best);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, Integer threshold, double matching, int maxWaitingTime) {
		return findUntil(elementId, visualElem, threshold, matching, maxWaitingTime);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, Integer threshold, int maxWaitingTime, boolean best) {
		return findText(elementId, visualElem, null, null, null, null, threshold, maxWaitingTime, best);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, Integer startX, Integer startY, Integer searchWidth, Integer searchHeight, Integer threshold, int maxWaitingTime, boolean best) {
		if(threshold == null) {
			return findUntil(elementId, visualElem, startX, startY, searchWidth, searchHeight, threshold, 0.9, maxWaitingTime, best);
		} else {
			return findUntil(elementId, visualElem, startX, startY, searchWidth, searchHeight, threshold, 0.85, maxWaitingTime, best);
		}
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, Integer threshold, int maxWaitingTime) {
		return findText(elementId, visualElem, threshold, maxWaitingTime, false);
	}
	
	public boolean find(String elementId, Double elementMatching, int maxWaitingTime, boolean best) {
		return find(elementId, getImageFromMap(elementId), elementMatching, maxWaitingTime, best);
	}
	
	public boolean find(String elementId, Double elementMatching, int maxWaitingTime) {
		return find(elementId, getImageFromMap(elementId), elementMatching, maxWaitingTime);
	}
	
	public boolean find(String elementId, int startX, int startY, int searchWidth, int searchHeight, Double elementMatching, int maxWaitingTime) {
		return find(elementId, getImageFromMap(elementId), startX, startY, searchWidth, searchHeight, elementMatching, maxWaitingTime);
	}
	
	public boolean find(String elementId, MarvinImage visualElem, Double elementMatching, int maxWaitingTime, boolean best) {
		return findUntil(elementId, visualElem, null, elementMatching, maxWaitingTime, best);
	}
	
	public boolean find(String elementId, MarvinImage visualElem, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, visualElem, null, elementMatching, maxWaitingTime);
	}
	
	public boolean find(String elementId, MarvinImage visualElem, int startX, int startY, int searchWidth, int searchHeight, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, visualElem, startX, startY, searchWidth, searchHeight, null, elementMatching, maxWaitingTime);
	}
	
	public boolean findUntil(String elementId, Integer threshold, Double elementMatching, int maxWaitingTime, boolean best) {
		return findUntil(elementId, getImageFromMap(elementId), threshold, elementMatching, maxWaitingTime, best);
	}
	
	public boolean findUntil(String elementId, Integer threshold, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, getImageFromMap(elementId), threshold, elementMatching, maxWaitingTime);
	}
	
	public boolean findUntil(String elementId, MarvinImage visualElem, Integer threshold, Double elementMatching, int maxWaitingTime, boolean best) {
		return findUntil(elementId, visualElem, null, null, null, null, threshold, elementMatching, maxWaitingTime, best);
	}
	
	public boolean findUntil(String elementId, MarvinImage visualElem, Integer threshold, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, visualElem, null, null, null, null, threshold, elementMatching, maxWaitingTime, false);
	}
	
	public boolean findUntil(String elementId, MarvinImage visualElem, int startX, int startY, Integer threshold, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, visualElem, null, null, null, null, threshold, elementMatching, maxWaitingTime, false);
	}
	
	public boolean findUntil(String elementId, MarvinImage visualElem, int startX, int startY, int searchWidth, int searchHeight, Integer threshold, Double elementMatching, int maxWaitingTime) {
		return findUntil(elementId, visualElem, startX, startY, searchWidth, searchHeight, threshold, elementMatching, maxWaitingTime, false);
	}
	
	
	
	
	public boolean findRelative
	(
		String elementId,
		MarvinImage visualElem,
		UIElement anchor,
		int xDiff,
		int yDiff,
		int searchWindowWidth,
		int searchWindowHeight,
		Integer threshold,
		Double elementMatching,
		int maxWaitingTim,
		boolean best
	) {
		return findUntil(elementId, visualElem, anchor.getX()+xDiff, anchor.getY()+yDiff, searchWindowWidth, searchWindowHeight, threshold, elementMatching, maxWaitingTim, best);
	}
	
	public boolean findUntil
	(
		String elementId, 
		MarvinImage visualElem,
		Integer startX,
		Integer startY,
		Integer searchWindowWidth,
		Integer searchWindowHeight,
		Integer threshold, 
		Double elementMatching, 
		int maxWaitingTime,
		boolean best
	) {
		long startTime = System.currentTimeMillis();
		while(true) {
			
			if(System.currentTimeMillis() - startTime > maxWaitingTime) {
				return false;
			}
			
			sleep(100);
			screenshot();
			
			Point p=null;
			
			startX = (startX != null ? startX : 0);
			startY = (startY != null ? startY : 0);
			searchWindowWidth = (searchWindowWidth != null ? searchWindowWidth : screen.getWidth());
			searchWindowHeight = (searchWindowHeight != null ? searchWindowHeight : screen.getHeight());
			
			if(threshold != null) {
				
				
				
				MarvinImage screenCopy = screen.clone();
				thresholding(screenCopy, threshold);
				
				MarvinImage visualElemCopy = visualElem.clone();
				thresholding(visualElemCopy, threshold);
				
				p = getElementCoords(visualElemCopy, screenCopy, startX, startY, searchWindowWidth, searchWindowHeight, elementMatching, best);
				
				if(debug) {
					long timestamp = System.currentTimeMillis();
					String match = (p != null ? "true" : "false");
					MarvinImageIO.saveImage(screen, "./debug/"+timestamp+"_screen"+"_"+elementId+"_"+match+".png");
					MarvinImageIO.saveImage(visualElem, "./debug/"+timestamp+"_"+elementId+"_"+match+".png");
					MarvinImageIO.saveImage(screenCopy, "./debug/"+timestamp+"_screen_bw_"+elementId+"_"+match+".png");
					MarvinImageIO.saveImage(visualElemCopy, "./debug/"+timestamp+"_"+elementId+"_bw"+"_"+match+".png");
				}
				
			} else {
				p = getElementCoords(visualElem, startX, startY, searchWindowWidth, searchWindowHeight, elementMatching, best);
				
				if(debug) {
					long timestamp = System.currentTimeMillis();
					String match = (p != null ? "true" : "false");
					MarvinImageIO.saveImage(screen, "./debug/"+timestamp+"_screen"+"_"+elementId+"_"+match+".png");
					MarvinImageIO.saveImage(visualElem, "./debug/"+timestamp+"_"+elementId+"_"+match+".png");
				}
			}
			
			if(p != null) {
				this.visualElem = visualElem;
				
				if(debug)
					System.out.println("found:"+p.x+","+p.y+": "+elementId);
				
				this.x = p.x;
				this.y = p.y;
				
				lastElement.setX(p.x);
				lastElement.setY(p.y);
				lastElement.setImage(this.visualElem);
				
				return true;
			}
		}
	}
	
	public Point getCoordinates(String elementImage, int maxWaitingTime, boolean best) {
		long startTime = System.currentTimeMillis();
		while(true) {
			
			if(System.currentTimeMillis() - startTime > maxWaitingTime) {
				return null;
			}
			
			sleep(300);
			screenshot();
			visualElem = MarvinImageIO.loadImage(elementImage);
			Point p = getElementCoords(visualElem, 0.95, best);
			
			if(p != null) {
				
				if(debug)
					System.out.println("found:"+p.x+","+p.y+": "+elementImage);
				
				return p;
			}
		}
	}
	
	public boolean findLastUntil(String elementId, int maxWaitingTime){
	     return findLastUntil(elementId, getImageFromMap(elementId), maxWaitingTime);
	}
	
	public boolean findLastUntil(String elementId, MarvinImage visualElem, int maxWaitingTime) {
		return findLastUntil(elementId, visualElem, null, maxWaitingTime);
	}
	
	public boolean findLastUntil(String elementId, MarvinImage visualElem, Integer threshold, int maxWaitingTime) {
		long startTime = System.currentTimeMillis();
		while(true) {
			
			if(System.currentTimeMillis() - startTime > maxWaitingTime) {
				return false;
			}
			
			sleep(300);
			screenshot();
			
			MarvinImage screenCopy = screen.clone();
			flip.process(screen, screenCopy);
			
			MarvinImage visualElemCopy = visualElem.clone();
			flip.process(visualElem, visualElemCopy);
			
			Point p;
			
			if(threshold != null) {
				
				thresholding(screenCopy, threshold);
				thresholding(visualElemCopy, threshold);
				
				if(debug) {
					MarvinImageIO.saveImage(screenCopy, "./debug/screenCopy.png");
					MarvinImageIO.saveImage(visualElemCopy, "./debug/visualElemCopy.png");
				}
				
				p = getElementCoords(visualElemCopy, screenCopy, 0.95, false);
			} else {
				p = getElementCoords(visualElemCopy, screenCopy, 0.95, false);
			}
			
			if(p != null) {
				this.visualElem = visualElem;
				
				if(debug)
					System.out.println("found:"+p.x+","+p.y+": "+elementId);
				
				this.x = p.x;
				this.y = screen.getHeight()-(p.y+visualElem.getHeight());
				return true;
			}
		}
	}
	
	private void mouseMove(int px, int py) {
		Point p;
		do{
			p = MouseInfo.getPointerInfo().getLocation();
			robot.mouseMove(px,  py);
		}while(p.x != px || p.y != py);
		
		p = MouseInfo.getPointerInfo().getLocation();
		
		this.x = px;
		this.y = py;
	}
	
	public void clickAt(int px, int py) {
		this.x = px;
		this.y = py;
		moveAndclick();
		
	}
	
	public void rightClickAt(int x, int y) {
		this.x = x;
		this.y = y;
		moveAndRightClick();
	}
	
	/**
	 * Click in last found UI element.
	 */
	public void click() {
		clickRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
		sleep(sleepAfterAction);
	}
	
	/**
	 * Right Click in last found UI element.
	 */
	public void rightClick() {
		rightClickRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
		sleep(sleepAfterAction);
	}
	
	
	
	/**
	 * Double-click in last found UI element.
	 */
	public void doubleclick() {
		doubleClickRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
		sleep(sleepAfterAction);
	}
	
	/**
	 * Click relative the last found UI element.
	 * @param x 		horizontal offset to the UI element.
	 * @param y 		vertical offset to the UI element.
	 */
	public void clickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndclick();
		sleep(sleepAfterAction);
	}
	
	public void rightClickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndRightClick();
		sleep(sleepAfterAction);
	}
	
	/**
	 * Double-click relative the last found UI element.
	 * @param x 		horizontal offset to the UI element.
	 * @param y 		vertical offset to the UI element.
	 */
	public void doubleClickRelative(int x, int y) {
		doubleClickRelative(x, y, 100);
	}
	
	/**
	 * Double-click relative the last found UI element.
	 * @param x 					horizontal offset to the UI element.
	 * @param y 					vertical offset to the UI element.
	 * @param sleepBetweenClicks	time in ms between individual click events.
	 */
	public void doubleClickRelative(int x, int y, int sleepBetweenClicks) {
		this.x += x;
		this.y += y;
		moveAndclick();
		sleep(sleepBetweenClicks);
		moveAndclick();
		sleep(sleepAfterAction);
	}
	
	/**
	 * Triple-click in last found UI element.
	 */
	public void tripleClick() {
		tripleClickRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
	}
	
	/**
	 * Triple-click relative the last found UI element.
	 * @param x 					horizontal offset to the UI element.
	 * @param y 					vertical offset to the UI element.
	 */
	public void tripleClickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndclick();
		sleep(100);
		moveAndclick();
		sleep(100);
		moveAndclick();
		sleep(sleepAfterAction);
	}
	
	/**
	 * Scroll down wheel action.
	 * @param y wheel actions.
	 */
	public void scrollDown(int y) {
		robot.mouseWheel(y);
	}
	
	/**
	 * Scroll up wheel action.
	 * @param y wheel actions.
	 */
	public void scrollUp(int y) {
		robot.mouseWheel(-y);
	}
	
	/**
	 * Move cursor to the last found element.
	 */
	public void move() {
		moveRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
	}
	
	/**
	 * Move cursor to an specific coordinate.
	 * @param x 		coordinate x
	 * @param y			coordinate y
	 */
	public void moveTo(int x, int y) {
		mouseMove(x, y);
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Move cursor relative the last found UI element.
	 * @param x			horizontal offset to the UI Element.
	 * @param y			vertical offset to the UI Element.
	 */
	public void moveRelative(int x, int y) {
		mouseMove(this.x+x, this.y+y);
	}
	
	
	public void moveRandom(int rangeX, int rangeY) {
		int x = (int)Math.round((Math.random()*rangeX));
		int y = (int)Math.round((Math.random()*rangeY));
		moveRelative(x, y);
	}
	
	/**
	 * Type a text, char by char (inividual key events)
	 * @param text text to be typed.
	 */
	public void type(String text) {
		for(int i=0; i<text.length(); i++) {
			typeKey(text.charAt(i));
		}
		sleep(sleepAfterAction);
	}
	
	/**
	 * Type text, char by char, specifying key interval time
	 * @param text				text to be typed.
	 * @param waitAfterChars	time interval between key events.
	 */
	public void typeWaitAfterChars(String text, int waitAfterChars) {
		for(int i=0; i<text.length(); i++) {
			typeKey(text.charAt(i));
			sleep(waitAfterChars);
		}
		sleep(sleepAfterAction);
	}
	
	/**
	 * Type text, char by char, specifying key interval time
	 * @param text				text to be typed.
	 * @param waitAfterChars	time interval between key events.
	 * @param waitAfter			sleep interval after event.
	 */
	public void typeWaitAfterChars(String text, int waitAfterChars, int waitAfter) {
		typeWaitAfterChars(text, waitAfterChars);
		sleep(waitAfter);
	}
	
	/**
	 * Type text, char by char, specifying key interval time
	 * @param text				text to be typed.
	 * @param waitAfterChars	time interval between key events.
	 * @param waitAfter			sleep interval after event.
	 */
	public void type(String text, int waitAfterChars, int waitAfter) {
		typeWaitAfterChars(text, waitAfterChars);
		sleep(waitAfter);
	}
	
	/**
	 * Type text, char by char.
	 * @param text				text to be typed.
	 * @param waitAfter			sleep interval after action.
	 */
	public void type(String text, int waitAfter) {
		type(text);
		sleep(waitAfter);
	}
	
	/**
	 * Paste content from the clipboard.
	 * @param text 		content to be pasted.
	 */
	public void paste(String text) {
		paste(text, 0);
	}
	
	/**
	 * Paste content from the clipboard.
	 * @param text 			content to be pasted.
	 * @param waitAfter		sleep interval after action.
	 */
	public void paste(String text, int waitAfter) {
		try {
			StringSelection selection = new StringSelection(text);
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			clip.setContents(selection, selection);
			sleep(500);
			controlV();
			sleep(waitAfter);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Copy content to the clipboard
	 * @param text			content to copy.
	 * @param waitAfter		sleep interval after action.
	 */
	public void copyToClipboard(String text, int waitAfter) {
		copyToClipboard(text);
		sleep(waitAfter);
	}
	
	/**
	 * Copy content to the clipboard
	 * @param text			content to copy.
	 */
	public void copyToClipboard(String text) {
		StringSelection stringSelection = new StringSelection(text);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(stringSelection, null);
	}
	
	
	private void moveAndclick() {
		mouseMove(this.x, this.y);
		robot.mousePress(InputEvent.BUTTON1_MASK);
		robot.mouseRelease(InputEvent.BUTTON1_MASK);
	}
	
	private void moveAndRightClick() {
		mouseMove(this.x, this.y);
		robot.mousePress(InputEvent.BUTTON3_MASK);
		robot.mouseRelease(InputEvent.BUTTON3_MASK);
	}
	
	private void click(int waitAfter) {
		moveAndclick();
		sleep(waitAfter);
	}
	
	private void singleKeyAction(int keyCode, int waitAfter) {
		robot.keyPress(keyCode);
		robot.keyRelease(keyCode);
		sleep(waitAfter);
	}
	
	private void doubleKeyActions(int keyCodeFirst, int keyCodeSecond, int waitAfter) {
		robot.keyPress(keyCodeFirst);
		robot.keyPress(keyCodeSecond);
		robot.keyRelease(keyCodeSecond);
		robot.keyRelease(keyCodeFirst);
		sleep(waitAfter);
	}
	
	/**
	 * Press key tab
	 */
	public void tab() {
		tab(sleepAfterAction);
	}
	
	/**
	 * Press key "tab"
	 * @param waitAfter			sleep interval after action.
	 */
	public void tab(int waitAfter) {
		singleKeyAction(KeyEvent.VK_TAB, waitAfter);
	}
	
	/**
	 * Press key "up"
	 * @param waitAfter			sleep interval after action.
	 */
	public void keyUp(int waitAfter) {
		singleKeyAction(KeyEvent.VK_UP, waitAfter);
	}
	
	/**
	 * Press key "up"
	 */
	public void keyUp() {
		keyUp(sleepAfterAction);
	}
	
	/**
	 * Press key "down"
	 * @param waitAfter			sleep interval after action.
	 */
	public void keyDown(int waitAfter) {
		singleKeyAction(KeyEvent.VK_DOWN, waitAfter);
	}
	
	/**
	 * Press key "down"
	 */
	public void keyDown() {
		keyDown(sleepAfterAction);
	}
	
	/**
	 * Press key "left"
	 */
	public void keyLeft() {
		keyLeft(sleepAfterAction);
	}
	
	/**
	 * Press key "left"
	 * @param waitAfter			sleep interval after action.
	 */
	public void keyLeft(int waitAfter) {
		singleKeyAction(KeyEvent.VK_LEFT, waitAfter);
	}
	
	/**
	 * Press key "right"
	 */
	public void keyRight() {
		keyRight(sleepAfterAction);
	}
	
	/**
	 * Press key "right"
	 * @param waitAfter			sleep interval after action.
	 */
	public void keyRight(int waitAfter) {
		singleKeyAction(KeyEvent.VK_RIGHT, waitAfter);
	}
	
	/**
	 * Press key "enter"
	 */
	public void enter() {
		enter(sleepAfterAction);
	}
	
	/**
	 * Press key "enter"
	 * @param waitAfter			sleep interval after action.
	 */
	public void enter(int waitAfter) {
		singleKeyAction(KeyEvent.VK_ENTER, waitAfter);
	}
	
	/**
	 * Press key "enter"
	 * @param waitAfter			sleep interval after action.
	 */
	public void keyEnter(int waitAfter) {
		singleKeyAction(KeyEvent.VK_ENTER, waitAfter);
	}
	
	public void keyEnd() {
		keyEnd(sleepAfterAction);
	}
	
	public void keyEnd(int waitAfter) {
		singleKeyAction(KeyEvent.VK_END, waitAfter);
	}
	
	public void keyEsc() {
		keyEsc(sleepAfterAction);
	}
	
	public void keyEsc(int waitAfter) {
		singleKeyAction(KeyEvent.VK_ESCAPE, waitAfter);
	}
	
	public void keyF1() {					robot.keyPress(KeyEvent.VK_F1);		robot.keyRelease(KeyEvent.VK_F1);	sleep(sleepAfterAction);}
	public void keyF2() {					robot.keyPress(KeyEvent.VK_F2);		robot.keyRelease(KeyEvent.VK_F2);	sleep(sleepAfterAction);}
	public void keyF3() {					robot.keyPress(KeyEvent.VK_F3);		robot.keyRelease(KeyEvent.VK_F3);	sleep(sleepAfterAction);}
	public void keyF4() {					robot.keyPress(KeyEvent.VK_F4);		robot.keyRelease(KeyEvent.VK_F4);	sleep(sleepAfterAction);}
	public void keyF5() {					robot.keyPress(KeyEvent.VK_F5);		robot.keyRelease(KeyEvent.VK_F5);	sleep(sleepAfterAction);}
	public void keyF6() {					robot.keyPress(KeyEvent.VK_F6);		robot.keyRelease(KeyEvent.VK_F6);	sleep(sleepAfterAction);}
	public void keyF7() {					robot.keyPress(KeyEvent.VK_F7);		robot.keyRelease(KeyEvent.VK_F7);	sleep(sleepAfterAction);}
	public void keyF8() {					robot.keyPress(KeyEvent.VK_F8);		robot.keyRelease(KeyEvent.VK_F8);	sleep(sleepAfterAction);}
	public void keyF9() {					robot.keyPress(KeyEvent.VK_F9);		robot.keyRelease(KeyEvent.VK_F9);	sleep(sleepAfterAction);}
	public void keyF10() {					robot.keyPress(KeyEvent.VK_F10);	robot.keyRelease(KeyEvent.VK_F10);	sleep(sleepAfterAction);}
	public void keyF11() {					robot.keyPress(KeyEvent.VK_F11);	robot.keyRelease(KeyEvent.VK_F11);	sleep(sleepAfterAction);}
	public void keyF12() {					robot.keyPress(KeyEvent.VK_F12);	robot.keyRelease(KeyEvent.VK_F12);	sleep(sleepAfterAction);}
	
	public void keyF1(int waitAfter) 	{	keyF1();	sleep(waitAfter);	}
	public void keyF2(int waitAfter) 	{	keyF2();	sleep(waitAfter);	}
	public void keyF3(int waitAfter) 	{	keyF3();	sleep(waitAfter);	}
	public void keyF4(int waitAfter) 	{	keyF4();	sleep(waitAfter);	}
	public void keyF5(int waitAfter) 	{	keyF5();	sleep(waitAfter);	}
	public void keyF6(int waitAfter) 	{	keyF6();	sleep(waitAfter);	}
	public void keyF7(int waitAfter) 	{	keyF7();	sleep(waitAfter);	}
	public void keyF8(int waitAfter) 	{	keyF8();	sleep(waitAfter);	}
	public void keyF9(int waitAfter) 	{	keyF9();	sleep(waitAfter);	}
	public void keyF10(int waitAfter) 	{	keyF10();	sleep(waitAfter);	}
	public void keyF11(int waitAfter) 	{	keyF11();	sleep(waitAfter);	}
	public void keyF12(int waitAfter) 	{	keyF12();	sleep(waitAfter);	}
	
	/**
	 * Hold key "shift"
	 */
	public void holdShift() {
		robot.keyPress(KeyEvent.VK_SHIFT);
	}
	
	/**
	 * Hold key "shift"
	 * @param waitAfter			sleep interval after action.
	 */
	public void holdShift(int waitAfter) {
		robot.keyPress(KeyEvent.VK_SHIFT);
		sleep(waitAfter);
	}
	
	/**
	 * Release key "shift". Need to be invoked after holdShift() method or similar.
	 */
	public void releaseShift() {
		robot.keyRelease(KeyEvent.VK_SHIFT);
	}
	
	/**
	 * Shortcut to maximize window on Windows Operating System
	 */
	public void maximizeWindow() {
		altSpace();
		sleep(1000);
		robot.keyPress(KeyEvent.VK_X);
		robot.keyRelease(KeyEvent.VK_X);
	}
	
	/**
	 * Press a sequence of keys. Hold the keys in the specified order, then release them.
	 * @param keys			array of key identification values like KeyEvent.VK_ENTER
	 */
	public void typeKeys(Integer interval, Integer... keys) {
		typeKeysWithInterval(100, keys);
	}
	
	public void typeKeysWithInterval(Integer interval, Integer... keys) {
		// Press
		for(int i=0; i<keys.length; i++){
			robot.keyPress(keys[i]);
			sleep(interval);
		}
		
		// release
		for(int i=keys.length-1; i>=0; i--){
			robot.keyRelease(keys[i]);
			sleep(interval);
		}
	}
	
	public void altPlusLetter(char c) {
		altPlusLetter(c, sleepAfterAction);
	}
	
	public void altPlusLetter(char c, int waitAfter) {
		doubleKeyActions(KeyEvent.VK_ALT, Character.toUpperCase(c), waitAfter);
		sleep(waitAfter);
	}
	
	public void altE() {
		altE(sleepAfterAction);
	}
	
	public void altE(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_ALT, KeyEvent.VK_E, waitAfter);
	}
	
	public void altR() {
		altR(sleepAfterAction);
	}
	
	public void altR(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_ALT, KeyEvent.VK_R, waitAfter);
	}
	
	public void altF() {
		altF(sleepAfterAction);
	}
	
	public void altF(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_ALT, KeyEvent.VK_F, waitAfter);
	}
	
	public void altU() {
		altU(sleepAfterAction);
	}
	
	public void altU(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_ALT, KeyEvent.VK_U, waitAfter);
	}
	
	public void altSpace() {
		altSpace(sleepAfterAction);
	}
	
	public void altSpace(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_ALT, KeyEvent.VK_SPACE, waitAfter);
	}
	
	public void altF4() {
		altF4(sleepAfterAction);
	}
	
	public void altF4(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_ALT, KeyEvent.VK_F4, waitAfter);
	}
	
	public void controlC() {
		controlC(sleepAfterAction);
	}
	
	public void controlC(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_C, waitAfter);
	}
	
	public void controlV() {
		controlV(sleepAfterAction);
	}
	
	public void controlV(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_V, waitAfter);
	}
	
	public void controlA() {
		controlA(sleepAfterAction);
	}
	
	public void controlA(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_A, waitAfter);
	}
	
	public void controlF() {
		controlF(sleepAfterAction);
	}
	
	public void controlF(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_F, waitAfter);
	}
	
	public void controlP() {
		controlP(sleepAfterAction);
	}
	
	public void controlP(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_P, waitAfter);
	}
	
	public void controlU() {
		controlU(sleepAfterAction);
	}
	
	public void controlU(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_U, waitAfter);
	}
	
	public void controlR() {
		controlR(sleepAfterAction);
	}
	
	public void controlR(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_R, waitAfter);
	}
	
	public void controlT() {
		controlT(sleepAfterAction);
	}
	
	public void controlT(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_T, waitAfter);
	}
	
	public void controlEnd() {
		controlEnd(sleepAfterAction);
	}
	
	public void controlEnd(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_END, waitAfter);
	}
	
	public void controlHome(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_HOME, waitAfter);
	}
	
	public void controlHome() {
		controlHome(sleepAfterAction);
	}
	
	public void controlW() {
		controlW(sleepAfterAction);
	}
	
	public void controlW(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_CONTROL, KeyEvent.VK_W, waitAfter);
	}
	
	public void controlShiftP() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(KeyEvent.VK_P);
		robot.keyRelease(KeyEvent.VK_P);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(sleepAfterAction);
	}
	
	public void controlShiftP(int waitAfter) {
		controlShiftP();
		sleep(waitAfter);
	}
	
	public void controlShiftJ() {
		robot.keyPress(KeyEvent.VK_CONTROL);
		robot.keyPress(KeyEvent.VK_SHIFT);
		robot.keyPress(KeyEvent.VK_J);
		robot.keyRelease(KeyEvent.VK_J);
		robot.keyRelease(KeyEvent.VK_SHIFT);
		robot.keyRelease(KeyEvent.VK_CONTROL);
		sleep(sleepAfterAction);
	}
	
	public void controlShiftJ(int waitAfter) {
		controlShiftJ();
		sleep(waitAfter);
	}
	
	public void shiftTab() {
		shiftTab(sleepAfterAction);
	}
	
	public void shiftTab(int waitAfter) {
		doubleKeyActions(KeyEvent.VK_SHIFT, KeyEvent.VK_TAB, waitAfter);
	}
	
	/**
	 * Get the current content in the clipboard.
	 * @return	content in the clipboard.
	 */
	public String getClipboard() {
		try {
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable t = clip.getContents(this);
			return new String(((String) t.getTransferData(DataFlavor.stringFlavor)).getBytes(), "UTF-8");
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void keyWindows() {
		keyWindows(sleepAfterAction);
	}
	
	public void keyWindows(int waitAfter) {
		singleKeyAction(KeyEvent.VK_WINDOWS, waitAfter);
	}
	
	public void space() {
		space(sleepAfterAction);
	}
	
	public void space(int waitAfter) {
		singleKeyAction(KeyEvent.VK_SPACE, waitAfter);
	}
	
	public void backspace() {
		backspace(sleepAfterAction);
	}
	
	public void backspace(int waitAfter) {
		singleKeyAction(KeyEvent.VK_BACK_SPACE, waitAfter);
	}

	public void delete() {
		delete(sleepAfterAction);
	}
	
	public void delete(int waitAfter) {
		singleKeyAction(KeyEvent.VK_DELETE, waitAfter);
	}
	
	/**
	 * Returns the current screen in MarvinImage format.
	 * @return			Image of the current screen in MarvinImage format.
	 */
	public MarvinImage getScreenShot() {
		screenshot();
		return screen;
	}
	
	private void screenshot() {
		screen.setBufferedImage(robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())));
	}
	
	/**
	 * Returns a given region of the current screen in MarvinImage format
	 * @param x			region start position x.
	 * @param y			region start position y.
	 * @param width		region's with.
	 * @param height	region's height.
	 * @return			Image of the current screen in MarvinImage format.	
	 */
	public MarvinImage screenCut(int x, int y, int width, int height) {
		MarvinImage img = new MarvinImage(robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())));
		MarvinImage imgOut = new MarvinImage(width, height);
		crop(img, imgOut, x, y, width, height);
		return imgOut;
	}
	
	
	/**
	 * Saves a screenshot in a given path
	 * @param path			desired path to save the screenshot.
	 */
	public void saveScreenshot(String path) {
		screenshot();
		MarvinImageIO.saveImage(screen, path);
	}
	
	/**
	 * Key "Window" + R shortcut to run commands on windws UI.
	 * @param command		command to execute.
	 */
	public void startRun(String command) {
		robot.keyPress(KeyEvent.VK_WINDOWS);
		sleep(1000);
		robot.keyPress(KeyEvent.VK_R);
		sleep(300);
		robot.keyRelease(KeyEvent.VK_R);
		sleep(300);
		robot.keyRelease(KeyEvent.VK_WINDOWS);
		sleep(100);
		type(command);
		sleep(3000);
		enter();
	}
	
	public void print(String text) {
		System.out.println(text);
	}
	
	/**
	 * Wait / Sleep for a given interval.
	 * @param ms	interval in milliseconds.
	 */
	public void wait(int ms) {
		sleep(ms);
	}
	
	/**
	 * Wait / Sleep for a given interval.
	 * @param sleep		interval in milliseconds.
	 */
	private void sleep(int sleep) {
		try {	
			Thread.sleep(sleep);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void typeKey(char c) {
		int code = KeyEvent.getExtendedKeyCodeForChar(c);
		
		if((int) c >= 65 && (int)c <= 90) {
			robot.keyPress(KeyEvent.VK_SHIFT);
		}
		
		switch(c) {
			case 'á':
				robot.keyPress(KeyEvent.VK_DEAD_ACUTE);
				typeKey('a');
				return;
			case 'à':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case 'ã':
				robot.keyPress(KeyEvent.VK_DEAD_TILDE);
				typeKey('a');
				return;
			case 'Ã':
				robot.keyPress(KeyEvent.VK_DEAD_TILDE);
				typeKey('A');
				return;
			case 'é':
				robot.keyPress(KeyEvent.VK_DEAD_ACUTE);
				typeKey('e');
				return;
			case 'ê':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case 'í':
				robot.keyPress(KeyEvent.VK_DEAD_ACUTE);
				typeKey('i');
				return;
			case 'ç':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD5);
				robot.keyRelease(KeyEvent.VK_NUMPAD5);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case 'Ç':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyPress(KeyEvent.VK_NUMPAD2);
				robot.keyRelease(KeyEvent.VK_NUMPAD2);
				robot.keyPress(KeyEvent.VK_NUMPAD8);
				robot.keyRelease(KeyEvent.VK_NUMPAD8);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case ':':
				robot.keyPress(KeyEvent.VK_SHIFT);
				robot.keyPress(KeyEvent.VK_SEMICOLON);
				robot.keyRelease(KeyEvent.VK_SEMICOLON);
				robot.keyRelease(KeyEvent.VK_SHIFT);
				return;
			case '/':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_NUMPAD4);
				robot.keyPress(KeyEvent.VK_NUMPAD7);
				robot.keyRelease(KeyEvent.VK_NUMPAD7);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '&':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD8);
				robot.keyRelease(KeyEvent.VK_NUMPAD8);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '@':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_NUMPAD6);
				robot.keyPress(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '$':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '%':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyPress(KeyEvent.VK_NUMPAD7);
				robot.keyRelease(KeyEvent.VK_NUMPAD7);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '?':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD6);
				robot.keyRelease(KeyEvent.VK_NUMPAD6);
				robot.keyPress(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_NUMPAD3);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case '_':
				robot.keyPress(KeyEvent.VK_SHIFT);
				robot.keyPress(KeyEvent.VK_MINUS);
				robot.keyRelease(KeyEvent.VK_MINUS);
				robot.keyRelease(KeyEvent.VK_SHIFT);
				return;
			case '(':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD4);
				robot.keyPress(KeyEvent.VK_NUMPAD0);
				robot.keyRelease(KeyEvent.VK_NUMPAD0);
				robot.keyRelease(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
			case ')':
				robot.keyPress(KeyEvent.VK_ALT);
				robot.keyPress(KeyEvent.VK_NUMPAD4);
				robot.keyPress(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD1);
				robot.keyRelease(KeyEvent.VK_NUMPAD4);
				robot.keyRelease(KeyEvent.VK_ALT);
				return;
		}
		
		
		robot.keyPress(code);
		robot.keyRelease(code);
		robot.keyRelease(KeyEvent.VK_SHIFT);
	}
	
	private Point getElementCoords(MarvinImage sub, double matching, boolean best) {
		return getElementCoords(sub, 0, 0, screen.getWidth(), screen.getHeight(), matching, best);
	}
	
	private Point getElementCoords
	(
		MarvinImage sub, 
		int startX, 
		int startY,
		int searchWindowWidth,
		int searchWindowHeight,
		double matching,
		boolean best
	) {
		return getElementCoords(sub, screen, startX, startY, searchWindowWidth, searchWindowHeight, matching, best);
	}
	
	private Point getElementCoords(MarvinImage sub, MarvinImage screen, double matching, boolean best) {
		return getElementCoords(sub, screen, 0, 0, screen.getWidth(), screen.getHeight(), matching, best);
	}
	
	private Point getElementCoords
	(
			MarvinImage sub, 
			MarvinImage screen, 
			int startX, 
			int startY,
			int searchWindowWidth,
			int searchWindowHeight,
			double matching,
			boolean best
	) {
		long time=System.currentTimeMillis();
		MarvinSegment seg = findSubimage(sub, screen, startX, startY, searchWindowWidth, searchWindowHeight, matching, best);
		
		if(seg != null) {
			return new Point(seg.x1,seg.y1);
		}
		return null;
	}
	
	private Point getElementCoordsCentered(MarvinImage sub, double matching, boolean best) {
		Point p = getElementCoords(sub, matching, best);
		
		if(p != null) {
			int x = p.x + (sub.getWidth() / 2);
			int y = p.y + (sub.getHeight() / 2);
			return new Point(x,y);
		}
		return null;
	}
	
	//findSubimage(sub, screen, startX, startY, matching);
	
	
	public MarvinSegment findSubimage
	(
		MarvinImage subimage,
		MarvinImage imageIn,
		int startX,
		int startY,
		Double similarity,
		boolean findBest
	) {
		return findSubimage(subimage, imageIn, startX, startY, imageIn.getWidth(), imageIn.getHeight(), similarity, findBest);
	}
	
	
	public MarvinSegment findSubimage
	(
		MarvinImage subimage,
		MarvinImage imageIn,
		int startX,
		int startY,
		int searchWindowWidth,
		int searchWindowHeight,
		Double similarity,
		boolean findBest
	) {
		List<MarvinSegment> segments = new ArrayList<MarvinSegment>();
		int subImagePixels = subimage.getWidth()*subimage.getHeight();
		boolean[][] processed=new boolean[imageIn.getWidth()][imageIn.getHeight()];
		
		double currScore;
		double bestScore=0;
		MarvinSegment bestSegment=null;
		
		int r1,g1,b1,r2,g2,b2;
		// Full image
		mainLoop:for(int y=startY; y<startY+searchWindowHeight; y++){
			for(int x=startX; x<startX+searchWindowWidth; x++){
				
				if(processed[x][y]){
					continue;
				}
				
				int notMatched=0;
				boolean match=true;
				int colorThreshold = (int)(255 * colorSensibility);
				
				// subimage
				if(y+subimage.getHeight() < imageIn.getHeight() && x+subimage.getWidth() < imageIn.getWidth()){
				
					
					outerLoop:for(int i=0; i<subimage.getHeight(); i++){
						for(int j=0; j<subimage.getWidth(); j++){
							
							if(processed[x+j][y+i]){
								match=false;
								break outerLoop;
							}
							
							r1 = imageIn.getIntComponent0(x+j, y+i);
							g1 = imageIn.getIntComponent1(x+j, y+i);
							b1 = imageIn.getIntComponent2(x+j, y+i);
							
							r2 = subimage.getIntComponent0(j, i);
							g2 = subimage.getIntComponent1(j, i);
							b2 = subimage.getIntComponent2(j, i);
							
							if
							(
								Math.abs(r1-r2) > colorThreshold ||
								Math.abs(g1-g2) > colorThreshold ||
								Math.abs(b1-b2) > colorThreshold
							){
								notMatched++;
								
								if(notMatched > (1-similarity)*subImagePixels){
									match=false;
									break outerLoop;
								}
							}
						}
					}
				} else{
					match=false;
				}
				
				if(match){
					
					currScore = 1.0 - ((double)notMatched / subImagePixels);
					
					if(!findBest)
						return new MarvinSegment(x,y,x+subimage.getWidth(), y+subimage.getHeight());
					else {
						if(currScore >= bestScore) {
							bestScore = currScore;
							bestSegment = new MarvinSegment(x,y,x+subimage.getWidth(), y+subimage.getHeight());
						}
					}
				}
			}
		}
		
		return bestSegment;
	}
}
