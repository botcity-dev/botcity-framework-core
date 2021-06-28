package dev.botcity.framework.bot;
import static org.marvinproject.plugins.collection.MarvinPluginCollection.crop;
import static org.marvinproject.plugins.collection.MarvinPluginCollection.thresholding;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.io.FilenameUtils;
import org.marvinproject.framework.image.MarvinImage;
import org.marvinproject.framework.image.MarvinSegment;
import org.marvinproject.framework.io.MarvinImageIO;
import org.marvinproject.framework.plugin.MarvinImagePlugin;
import org.marvinproject.plugins.image.transform.flip.Flip;

import com.github.kklisura.cdt.launch.ChromeLauncher;
import com.github.kklisura.cdt.protocol.commands.Input;
import com.github.kklisura.cdt.protocol.commands.Network;
import com.github.kklisura.cdt.protocol.commands.Page;
import com.github.kklisura.cdt.protocol.commands.Runtime;
import com.github.kklisura.cdt.protocol.types.browser.Bounds;
import com.github.kklisura.cdt.protocol.types.browser.PermissionType;
import com.github.kklisura.cdt.protocol.types.browser.SetDownloadBehaviorBehavior;
import com.github.kklisura.cdt.protocol.types.browser.WindowState;
import com.github.kklisura.cdt.protocol.types.dom.Rect;
import com.github.kklisura.cdt.protocol.types.input.DispatchKeyEventType;
import com.github.kklisura.cdt.protocol.types.input.DispatchMouseEventType;
import com.github.kklisura.cdt.protocol.types.input.MouseButton;
import com.github.kklisura.cdt.protocol.types.page.CaptureScreenshotFormat;
import com.github.kklisura.cdt.protocol.types.page.LayoutMetrics;
import com.github.kklisura.cdt.protocol.types.page.LayoutViewport;
import com.github.kklisura.cdt.protocol.types.page.Viewport;
import com.github.kklisura.cdt.protocol.types.page.VisualViewport;
import com.github.kklisura.cdt.protocol.types.runtime.Evaluate;
import com.github.kklisura.cdt.services.ChromeDevToolsService;
import com.github.kklisura.cdt.services.ChromeService;
import com.github.kklisura.cdt.services.types.ChromeTab;

public class WebBot {

	
	private Integer 					x,
										y;
	
	private MarvinImage 				screen,
										visualElem;
	
	private UIElement					lastElement = new UIElement();
	
	private static MarvinImagePlugin 	flip;
	
	private boolean 					debug=false;
	
	private int 						defaultSleepAfterAction=300;
	
	private double						colorSensibility = 0.04;
	
	protected String lastClipboardText = "";
	
	private ClassLoader					resourceClassLoader;
	
	private Map<String, MarvinImage>	mapImages;
	
	private static ChromeLauncher launcher;

	private static ChromeService chromeService;

	public static ChromeTab tab;

	private static ChromeDevToolsService devToolsService;

	private static Page page;
	
	private static Network network;
	
	private static Input input;
	
	private static Runtime run;
	
	private boolean firstTab = true;
	
	private static List<ChromeTab> tabs = new ArrayList<ChromeTab>();
	
	private static boolean headless = true;
	
	private static String downloadFolderPath = System.getProperty("user.home") + "/Desktop";
	
	private Dimension scrennSize = new Dimension(1600, 900);
	
	public WebBot() {		
		try {
			mapImages = new HashMap<String, MarvinImage>();
		} catch(Exception e) {
			e.printStackTrace();
		}
		screen = new MarvinImage(1,1);
		
		flip = new Flip();
		flip.load();
		flip.setAttribute("flip", "vertical");
	}
	
	public void setColorSensibility(double colorSensibility) {
		this.colorSensibility = colorSensibility;
	}
	
	public double getColorSensibility() {
		return this.colorSensibility;
	}
	
	public void setDownloadFolder(String path) {
		try {
            Paths.get(path);
            this.downloadFolderPath = path;
            setDownloadFolder();   
        } catch (InvalidPathException ex) {
            ex.printStackTrace();
        }
	}
	
	private void setDownloadFolder() {
        if(devToolsService != null) {
        	devToolsService.getBrowser().setDownloadBehavior(SetDownloadBehaviorBehavior.ALLOW, null, this.downloadFolderPath, true);
		}
	}
	
	public void setDownloadFileNameAndWaitTillDownloadCompleted(String nameWithoutExt) {
		File chosenFile = null;
		File directory = new File(this.downloadFolderPath);
		while(chosenFile == null && directory.exists()) {
			
		    File[] files = directory.listFiles(File::isFile);
		    long lastModifiedTime = Long.MIN_VALUE;

		    if (files != null)
		    {
		        for (File file : files)
		        {
		            if (file.lastModified() > lastModifiedTime)
		            {
		                chosenFile = file;
		                lastModifiedTime = file.lastModified();
		            }
		        }
		    }
		}
	    
	    if(nameWithoutExt != null) {
	    	try {
					String ext = FilenameUtils.getExtension(chosenFile.getName());
					boolean flag = chosenFile.renameTo(new File(nameWithoutExt+"."+ext));
					
	    		} catch(Exception e) {
	    		   e.printStackTrace();
	    		}
	    }

	}
	
	public void waitTillDownloadCompleted() {
		setDownloadFileNameAndWaitTillDownloadCompleted(null);
	}
	
	public void setScreenResolution(int width, int height) {
		this.scrennSize = new Dimension(width, height);
	}
	
	private void setScreenResolution() {
		Bounds b = new Bounds();
		b.setWidth(this.scrennSize.width);
		b.setHeight(this.scrennSize.height);
		devToolsService.getBrowser().setWindowBounds(devToolsService.getBrowser().getWindowForTarget().getWindowId(), b);
		devToolsService.getEmulation().setVisibleSize(this.scrennSize.width, this.scrennSize.height);
		//.getEmulation().setPageScaleFactor((double) 1);
	}
	
	public boolean isHeadless() {
		return headless;
	}
	
	public void setHeadless(boolean headless) {
		this.headless = headless;
	}
	
	public void enableDebug(){
		this.debug = true;
	}
	
	public void setResourceClassLoader(ClassLoader classloader) {
		this.resourceClassLoader = classloader;
	}
	
	public void setCurrentTab(ChromeTab tab) {
		this.tab = tab;
	}
	
	public ChromeTab getCurrentTab() {
		return tab;
	}
	
	public void waitUntilBroserClosed() {
		devToolsService.waitUntilClosed();
	}
	
	public List<ChromeTab> getAllTabsOpen(){
		List<ChromeTab> lstTabs = chromeService.getTabs();
		List<ChromeTab> lstRet = new ArrayList<ChromeTab>();
		for(ChromeTab c : lstTabs) {
			if(c.isPageType()){
				lstRet.add(c);
			}
		}
		return lstRet;
	}
	
	public ChromeDevToolsService openTab() {
		ChromeTab tab = chromeService.createTab();
		List<ChromeTab> lstTabs = chromeService.getTabs();
		if(firstTab) {
			for(ChromeTab c : lstTabs) {
				if(c.getId() != tab.getId()){
					chromeService.closeTab(c);
				}
			}
			firstTab = false;
		}
		devToolsService = chromeService.createDevToolsService(tab);
		page = devToolsService.getPage();
		network = devToolsService.getNetwork();
		input = devToolsService.getInput();
		network.enable();
		run = devToolsService.getRuntime();
		run.enable();
		
		List<PermissionType> permissions = new ArrayList<PermissionType>();
		permissions.add(PermissionType.CLIPBOARD_READ_WRITE);
		permissions.add(PermissionType.CLIPBOARD_SANITIZED_WRITE);
		permissions.add(PermissionType.VIDEO_CAPTURE);
		permissions.add(PermissionType.ACCESSIBILITY_EVENTS);
		permissions.add(PermissionType.AUDIO_CAPTURE);
		permissions.add(PermissionType.BACKGROUND_FETCH);
		permissions.add(PermissionType.BACKGROUND_SYNC);
		permissions.add(PermissionType.DURABLE_STORAGE);
		permissions.add(PermissionType.GEOLOCATION);
		permissions.add(PermissionType.IDLE_DETECTION);
		permissions.add(PermissionType.MIDI);
		permissions.add(PermissionType.MIDI_SYSEX);
		permissions.add(PermissionType.NFC);
		permissions.add(PermissionType.NOTIFICATIONS);
		permissions.add(PermissionType.PAYMENT_HANDLER);	
		devToolsService.getBrowser().grantPermissions(permissions);
		
		tabs.add(tab);
		
		return devToolsService;
	}
	
	public void closeCurrentTab() {
		closeTab(tab);
	}
	
	public void closeTab(ChromeTab tab) {
		for(ChromeTab t : tabs) {
			if(t.getId() == tab.getId()) {
				tabs.remove(t);
				break;
			}
		}
		chromeService.closeTab(tab);
		List<ChromeTab> temp = chromeService.getTabs();
		if(temp.size() > 0) {
			int index  = 0;
			for(int i= tabs.size(); i>=0; i--) {
				if(tabs.get(i).isPageType()) {
					index = i;
					break;
				}
			}
			chromeService.activateTab(tabs.get(index));
			
			devToolsService = chromeService.createDevToolsService(tabs.get(index));
			page = devToolsService.getPage();
			network = devToolsService.getNetwork();
			input = devToolsService.getInput();
			network.enable();
			run = devToolsService.getRuntime();
			run.enable();
			
			List<PermissionType> permissions = new ArrayList<PermissionType>();
			permissions.add(PermissionType.CLIPBOARD_READ_WRITE);
			permissions.add(PermissionType.CLIPBOARD_SANITIZED_WRITE);
			permissions.add(PermissionType.VIDEO_CAPTURE);
			permissions.add(PermissionType.ACCESSIBILITY_EVENTS);
			permissions.add(PermissionType.AUDIO_CAPTURE);
			permissions.add(PermissionType.BACKGROUND_FETCH);
			permissions.add(PermissionType.BACKGROUND_SYNC);
			permissions.add(PermissionType.DURABLE_STORAGE);
			permissions.add(PermissionType.GEOLOCATION);
			permissions.add(PermissionType.IDLE_DETECTION);
			permissions.add(PermissionType.MIDI);
			permissions.add(PermissionType.MIDI_SYSEX);
			permissions.add(PermissionType.NFC);
			permissions.add(PermissionType.NOTIFICATIONS);
			permissions.add(PermissionType.PAYMENT_HANDLER);	
			devToolsService.getBrowser().grantPermissions(permissions);
		}
	}
	
	public void browserMaximized() {
		try {
			if(devToolsService != null) {
				Bounds b = new Bounds();
				b.setWindowState(WindowState.MAXIMIZED);
				devToolsService.getBrowser().setWindowBounds(devToolsService.getBrowser().getWindowForTarget().getWindowId(), b);
			}else {
				throw new NullPointerException("Error: You must first call the method: navigateTo('linkToNavegate')");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void browserMinimized() {
		try {
			if(devToolsService != null) {
				Bounds b = new Bounds();
				b.setWindowState(WindowState.MINIMIZED);
				devToolsService.getBrowser().setWindowBounds(devToolsService.getBrowser().getWindowForTarget().getWindowId(), b);
			}else {
				throw new NullPointerException("Error: You must first call the method: navigateTo('linkToNavegate')");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void browserFullScren() {
		try {
			if(devToolsService != null) {
				Bounds b = new Bounds();
				b.setWindowState(WindowState.FULLSCREEN);
				devToolsService.getBrowser().setWindowBounds(devToolsService.getBrowser().getWindowForTarget().getWindowId(), b);
			}else {
				throw new NullPointerException("Error: You must first call the method: navigateTo('linkToNavegate')");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Object executeJavascript(String code) {
		Evaluate evaluation = run.evaluate(code);
        return evaluation.getResult().getValue();
	}
	
	public void addImage(String label, String path) {
		File f = new File(path);
		
		// file outside jar?
		if(f.exists())
			mapImages.put(label, MarvinImageIO.loadImage(path));
		else {
			if(this.resourceClassLoader != null) {
				ImageIcon img = new ImageIcon(this.resourceClassLoader.getResource(path));
				mapImages.put("label", new MarvinImage(toBufferedImage(img.getImage())));
			}
		}
	}
	
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
	
	public MarvinImage getImageFromMap(String label) {
		return mapImages.get(label);
	}
	
	public UIElement getLastElement() {
		return this.lastElement;
	}
	
	public void redirectTo(String uri){
		page.navigate(uri);
	}
	
	public void startBrowser() {
		
		if(launcher == null)
			launcher = new ChromeLauncher();
		
		if(chromeService == null)
			chromeService = launcher.launch(headless);
		
		List<ChromeTab> lstTabs = chromeService.getTabs();
		if(devToolsService == null) {
			for(ChromeTab c : lstTabs) {
				if(c.isPageType()){
					tab = c;
				}
			}
		}
		
		if(devToolsService == null) {
			devToolsService = chromeService.createDevToolsService(tab);
			page = devToolsService.getPage();
			network = devToolsService.getNetwork();
			input = devToolsService.getInput();
			network.enable();
			run = devToolsService.getRuntime();
			run.enable();
			devToolsService.getAccessibility().enable();
			devToolsService.getApplicationCache().enable();
			setDownloadFolder(this.downloadFolderPath);
		
			List<PermissionType> permissions = new ArrayList<PermissionType>();
			permissions.add(PermissionType.CLIPBOARD_READ_WRITE);
			permissions.add(PermissionType.CLIPBOARD_SANITIZED_WRITE);
			permissions.add(PermissionType.VIDEO_CAPTURE);
			permissions.add(PermissionType.ACCESSIBILITY_EVENTS);
			permissions.add(PermissionType.AUDIO_CAPTURE);
			permissions.add(PermissionType.BACKGROUND_FETCH);
			permissions.add(PermissionType.BACKGROUND_SYNC);
			permissions.add(PermissionType.DURABLE_STORAGE);
			permissions.add(PermissionType.GEOLOCATION);
			permissions.add(PermissionType.IDLE_DETECTION);
			permissions.add(PermissionType.MIDI);
			permissions.add(PermissionType.MIDI_SYSEX);
			permissions.add(PermissionType.NFC);
			permissions.add(PermissionType.NOTIFICATIONS);
			permissions.add(PermissionType.PAYMENT_HANDLER);		
			devToolsService.getBrowser().grantPermissions(permissions);
		}
	}
	
	public ChromeDevToolsService navigateTo(String uri){
		startBrowser();
		page.navigate(uri);	
		setScreenResolution(1600, 900);
		return devToolsService;
	}
	
	public boolean clickOn(String elementId){
	    return clickOn(getImageFromMap(elementId));
	}
	
	public boolean clickOn(MarvinImage visualElem) {
		screenshot();
		Point p = getElementCoordsCentered(visualElem, 0.95, false);
		if(p != null) {
			mouseMove(p.x, p.y);
			input.dispatchMouseEvent(DispatchMouseEventType.MOUSE_PRESSED, (double)p.x, (double)p.y);
			input.dispatchMouseEvent(DispatchMouseEventType.MOUSE_RELEASED, (double)p.x, (double)p.y);
			
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
	
	public boolean findText(String elementId,int maxWaitingTime, boolean best) {
		return findText(elementId, getImageFromMap(elementId), null, maxWaitingTime, best);
	}
	
	public boolean findText(String elementId,int maxWaitingTime) {
		return findText(elementId, getImageFromMap(elementId), null, maxWaitingTime);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, int maxWaitingTime, boolean best) {
		return findText(elementId, visualElem, null, maxWaitingTime, best);
	}
	
	public boolean findText(String elementId, MarvinImage visualElem, int maxWaitingTime) {
		return findText(elementId, visualElem, null, maxWaitingTime);
	}
	
	public boolean findText(String elementId, Integer threshold, int maxWaitingTime, boolean best) {
		return findText(elementId, getImageFromMap(elementId), threshold, maxWaitingTime, best);
	}
	
	public boolean findText(String elementId, Integer threshold, int maxWaitingTime) {
		return findText(elementId, getImageFromMap(elementId), threshold, maxWaitingTime);
	}
	
	public boolean findText(String elementId, Integer threshold, double matching, int maxWaitingTime, boolean best) {
		return findUntil(elementId, getImageFromMap(elementId), threshold, matching, maxWaitingTime, best);
	}
	
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
		input.dispatchMouseEvent(DispatchMouseEventType.MOUSE_MOVED, (double)px, (double)py);
		input.dispatchMouseEvent(DispatchMouseEventType.MOUSE_MOVED, (double)px, (double)py);
		
		this.x = px;
		this.y = py;
	}
	
	public void clickAt(int px, int py) {
		this.x = px;
		this.y = py;
		moveAndclick(1);
	}
	
	public void click() {
		clickRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
		sleep(defaultSleepAfterAction);
	}
	
	public void doubleclick() {
		doubleClickRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
		sleep(defaultSleepAfterAction);
	}
	
	public void clickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndclick(1);
		sleep(defaultSleepAfterAction);
	}
	
	public void doubleClickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndclick(2);
		sleep(defaultSleepAfterAction);
	}
	
	public void tripleClickRelative(int x, int y) {
		this.x += x;
		this.y += y;
		moveAndclick(3);
		sleep(defaultSleepAfterAction);
	}
	
	public void scrollDown(int y) {
		input.dispatchMouseEvent(DispatchMouseEventType.MOUSE_WHEEL, 0.0, 0.0, null, null , null, null, null, null, null, null, null, null, 0.0, (double)y, null);
	}
	
	public void scrollUp(int y) {
		input.dispatchMouseEvent(DispatchMouseEventType.MOUSE_WHEEL, 0.0, 0.0, null, null , null, null, null, null, null, null, null, null, 0.0, (double)-y, null);
	}
	
	public void move() {
		moveRelative(visualElem.getWidth()/2, visualElem.getHeight()/2);
	}
	
	public void moveTo(int x, int y) {
		mouseMove(x, y);
		this.x = x;
		this.y = y;
	}
	
	public void moveRelative(int x, int y) {
		mouseMove(this.x+x, this.y+y);
	}
	
	public void moveRandom(int rangeX, int rangeY) {
		int x = (int)Math.round((Math.random()*rangeX));
		int y = (int)Math.round((Math.random()*rangeY));
		moveRelative(x, y);
	}
	
	public void type(String text) {
		for(int i=0; i<text.length(); i++) {
			typeKey(text.charAt(i));
		}
		sleep(defaultSleepAfterAction);
	}
	
	public void typeWaitAfterChars(String text, int waitAfterChars) {
		for(int i=0; i<text.length(); i++) {
			typeKey(text.charAt(i));
			sleep(waitAfterChars);
		}
		sleep(defaultSleepAfterAction);
	}
	
	public void typeWaitAfterChars(String text, int waitAfterChars, int waitAfter) {
		typeWaitAfterChars(text, waitAfterChars);
		sleep(waitAfter);
	}
	
	public void type(String text, int waitAfterChars, int waitAfter) {
		typeWaitAfterChars(text, waitAfterChars);
		sleep(waitAfter);
	}
	
	public void type(String text, int waitAfter) {
		type(text);
		sleep(waitAfter);
	}
	
	public void paste(String text) {
		paste(text, 0);
	}
	
	public void paste(String text, int waitAfter) {
		executeJavascript("var elementfocused = document.activeElement; function copyStringToClipboard(str) { var el = document.createElement('textarea'); el.value = str; el.setAttribute('readonly', ''); el.style = { position: 'absolute', left: '-9999px' }; document.body.appendChild(el); el.select(); document.execCommand('copy'); document.body.removeChild(el); }copyStringToClipboard('"+text+"'); elementfocused.focus();");
		sleep(1000);
		String[] commands = {"Paste"};
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, null, null, null, null, null, null, null, Arrays.asList(commands));
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(waitAfter);
	}
	
	
	public void copyToClipboard(String text, int waitAfter) {
		try {
			copyToClipboard(text);
		} catch (Exception e) {
			e.printStackTrace();
		}
		sleep(waitAfter);
	}
	
	public void copyToClipboard(String text) throws Exception {
		if(!headless) {
			StringSelection stringSelection = new StringSelection(text);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		}else {
			throw new Exception("The clipboard functionality is only avaliable out of Headless mode.");
		}
	}
	
	private void moveAndclick(int count) {
		input.dispatchMouseEvent(DispatchMouseEventType.MOUSE_PRESSED, (double)this.x, (double)this.y, null, null , MouseButton.LEFT, null, count, null, null, null, null, null, null, null, null);
		input.dispatchMouseEvent(DispatchMouseEventType.MOUSE_RELEASED, (double)this.x, (double)this.y, null, null , MouseButton.LEFT, null, count, null, null, null, null, null, null, null, null);
		sleep(defaultSleepAfterAction);
	}
	
	private void click(int waitAfter) {
		moveAndclick(1);
		sleep(waitAfter);
	}
	
	public void tab() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "Tab", 9, 9, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void tab(int waitAfter) {
		tab();
		sleep(waitAfter);
	}
	
	public void keyRight() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "Right", KeyEvent.VK_RIGHT, KeyEvent.VK_RIGHT, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void keyRight(int waitAfter) {
		keyRight();
		sleep(waitAfter);
	}
	
	public void enter() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, "\r", "\r", null, null, "Enter", 13, 13, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void keyEnter(int waitAfter) {
		enter();
		sleep(waitAfter);
	}
	
	public void keyEnd() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "End", KeyEvent.VK_END, KeyEvent.VK_END, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void keyEnd(int waitAfter) {
		keyEnd();
		sleep(waitAfter);
	}
	
	public void keyEsc() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "Escape", KeyEvent.VK_ESCAPE, KeyEvent.VK_ESCAPE, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void keyEsc(int waitAfter) {
		keyEsc();
		sleep(waitAfter);
	}
	
	public void keyF1() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F1", KeyEvent.VK_F1, KeyEvent.VK_F1, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF2() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F2", KeyEvent.VK_F2, KeyEvent.VK_F2, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF3() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F3", KeyEvent.VK_F3, KeyEvent.VK_F3, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF4() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F4", KeyEvent.VK_F4, KeyEvent.VK_F4, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF5() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F5", KeyEvent.VK_F5, KeyEvent.VK_F5, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF6() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F6", KeyEvent.VK_F6, KeyEvent.VK_F6, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF7() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F7", KeyEvent.VK_F7, KeyEvent.VK_F7, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF8() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F8", KeyEvent.VK_F8, KeyEvent.VK_F8, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF9() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F9", KeyEvent.VK_F9, KeyEvent.VK_F9, null, null, null, null, null);		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF10() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F10", KeyEvent.VK_F10, KeyEvent.VK_F10, null, null, null, null, null);	input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF11() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F11", KeyEvent.VK_F11, KeyEvent.VK_F11, null, null, null, null, null);	input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	public void keyF12() {					input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "F12", KeyEvent.VK_F12, KeyEvent.VK_F12, null, null, null, null, null);	input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);	sleep(defaultSleepAfterAction);}
	
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
	
//	public void holdShift() {
//		robot.keyPress(KeyEvent.VK_SHIFT);
//	}
//	
//	public void holdShift(int waitAfter) {
//		robot.keyPress(KeyEvent.VK_SHIFT);
//		sleep(waitAfter);
//	}
//	
//	public void releaseShift() {
//		robot.keyRelease(KeyEvent.VK_SHIFT);
//	}
	
	public void typeKeys(Integer... keys) {
		// Press
		for(int i=0; i<keys.length; i++){
			Field[] fields = java.awt.event.KeyEvent.class.getDeclaredFields();
			for (Field f : fields) {
			    if (Modifier.isStatic(f.getModifiers())) {
			        input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, f.getName(), keys[i], keys[i], null, null, null, null, null);
			    } 
			}
			sleep(100);
		}
		
		// release
		for(int i=keys.length-1; i>=0; i--){
			Field[] fields = java.awt.event.KeyEvent.class.getDeclaredFields();
			for (Field f : fields) {
			    if (Modifier.isStatic(f.getModifiers())) {
			        input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP, null, null, null, null, null, null, f.getName(), keys[i], keys[i], null, null, null, null, null);
			    } 
			}
			sleep(100);
		}
	}
	
//	public void altE() {
//		robot.keyPress(KeyEvent.VK_ALT);
//		robot.keyPress(KeyEvent.VK_E);
//		robot.keyRelease(KeyEvent.VK_E);
//		robot.keyRelease(KeyEvent.VK_ALT);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void altE(int waitAfter) {
//		altE();
//		sleep(waitAfter);
//	}
		
	public void controlC() {
		executeJavascript("var text = ''; if (window.getSelection) { text = window.getSelection().toString(); } else if (document.selection && document.selection.type != 'Control') { text = document.selection.createRange().text; }");
		executeJavascript("if( null == document.getElementById('clipboardTransferText')){ let el = document.createElement('textarea'); el.value = ''; el.setAttribute('readonly', ''); el.style = {position: 'absolute', left: '-9999px'}; el.id = 'clipboardTransferText'; document.body.appendChild(el); } document.getElementById('clipboardTransferText').value = text;");
		String[] commands = {"Copy"};
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, null, null, null, null, null, null, null, Arrays.asList(commands));
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlC(int waitAfter) {
		controlC();
		sleep(waitAfter);
	}
	
	public void controlV() {
		String[] commands = {"Paste"};
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, null, null, null, null, null, null, null, Arrays.asList(commands));
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlA() {
		String[] commands = {"SelectAll"};
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, null, null, null, null, null, null, null, Arrays.asList(commands));
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void controlA(int waitAfter) {
		controlA();
		sleep(waitAfter);
	}
	
	public void scrollToEndOfDocument() {
		String[] commands = {"ScrollToEndOfDocument"};
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, null, null, null, null, null, null, null, Arrays.asList(commands));
		sleep(500);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void scrollToEndOfDocument(int waitAfter) {
		scrollToEndOfDocument();
		sleep(waitAfter);
	}
	
	public void scrollToBeginningOfDocument(int waitAfter) {
		scrollToBeginningOfDocument();
		sleep(waitAfter);
	}
	
	public void scrollToBeginningOfDocument() {
		String[] commands = {"ScrollToBeginningOfDocument"};
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, null, null, null, null, null, null, null, Arrays.asList(commands));
		sleep(500);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void scrollPageForward() {
		String[] commands = {"ScrollPageForward"};
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, null, null, null, null, null, null, null, Arrays.asList(commands));
		sleep(500);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void scrollPageForward(int waitAfter) {
		scrollPageForward();
		sleep(waitAfter);
	}
	
	public void scrollPageBackward(int waitAfter) {
		scrollPageBackward();
		sleep(waitAfter);
	}
	
	public void scrollPageBackward() {
		String[] commands = {"ScrollPageBackward"};
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, null, null, null, null, null, null, null, Arrays.asList(commands));
		sleep(500);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
//	public void controlF() {
//		robot.keyPress(KeyEvent.VK_CONTROL);
//		robot.keyPress(KeyEvent.VK_F);
//		robot.keyRelease(KeyEvent.VK_F);
//		robot.keyRelease(KeyEvent.VK_CONTROL);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void controlF(int waitAfter) {
//		controlF();
//		sleep(waitAfter);
//	}
//	
//	public void controlP() {
//		robot.keyPress(KeyEvent.VK_CONTROL);
//		robot.keyPress(KeyEvent.VK_P);
//		robot.keyRelease(KeyEvent.VK_P);
//		robot.keyRelease(KeyEvent.VK_CONTROL);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void controlP(int waitAfter) {
//		controlP();
//		sleep(waitAfter);
//	}
//	
//	public void controlU() {
//		robot.keyPress(KeyEvent.VK_CONTROL);
//		robot.keyPress(KeyEvent.VK_U);
//		robot.keyRelease(KeyEvent.VK_U);
//		robot.keyRelease(KeyEvent.VK_CONTROL);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void controlU(int waitAfter) {
//		controlU();
//		sleep(waitAfter);
//	}
//	
//	public void controlR() {
//		robot.keyPress(KeyEvent.VK_CONTROL);
//		robot.keyPress(KeyEvent.VK_R);
//		robot.keyRelease(KeyEvent.VK_R);
//		robot.keyRelease(KeyEvent.VK_CONTROL);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void controlR(int waitAfter) {
//		controlR();
//		sleep(waitAfter);
//	}
//	
//	public void controlEnd() {
//		robot.keyPress(KeyEvent.VK_CONTROL);
//		robot.keyPress(KeyEvent.VK_END);
//		robot.keyRelease(KeyEvent.VK_END);
//		robot.keyRelease(KeyEvent.VK_CONTROL);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void controlEnd(int waitAfter) {
//		controlEnd();
//		sleep(waitAfter);
//	}
//	
//	public void controlShiftP() {
//		robot.keyPress(KeyEvent.VK_CONTROL);
//		robot.keyPress(KeyEvent.VK_SHIFT);
//		robot.keyPress(KeyEvent.VK_P);
//		robot.keyRelease(KeyEvent.VK_P);
//		robot.keyRelease(KeyEvent.VK_SHIFT);
//		robot.keyRelease(KeyEvent.VK_CONTROL);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void controlShiftP(int waitAfter) {
//		controlShiftP();
//		sleep(waitAfter);
//	}
//	
//	public void controlShiftJ() {
//		robot.keyPress(KeyEvent.VK_CONTROL);
//		robot.keyPress(KeyEvent.VK_SHIFT);
//		robot.keyPress(KeyEvent.VK_J);
//		robot.keyRelease(KeyEvent.VK_J);
//		robot.keyRelease(KeyEvent.VK_SHIFT);
//		robot.keyRelease(KeyEvent.VK_CONTROL);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void controlShiftJ(int waitAfter) {
//		controlShiftJ();
//		sleep(waitAfter);
//	}
//	
//	public void shiftTab() {
//		robot.keyPress(KeyEvent.VK_SHIFT);
//		robot.keyPress(KeyEvent.VK_TAB);
//		robot.keyRelease(KeyEvent.VK_TAB);
//		robot.keyRelease(KeyEvent.VK_SHIFT);
//		sleep(defaultSleepAfterAction);
//	}
//	
//	public void shiftTab(int waitAfter) {
//		shiftTab();
//		sleep(waitAfter);
//	}
	
	public String getClipboard(){
		String text = "";
		Object o = executeJavascript("document.getElementById('clipboardTransferText').value");
		if(null != o) {
			text = o.toString();
			lastClipboardText = text;
			executeJavascript("document.getElementById('clipboardTransferText').remove();");
		}else {
			text = lastClipboardText;
		}
		return text;
	}
	

	public void typeLeft(int waitAfter) {
		typeLeft();
		sleep(waitAfter);
	}
	
	public void typeLeft() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "Left", KeyEvent.VK_LEFT, KeyEvent.VK_LEFT, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void typeDown(int waitAfter) {
		typeDown();
		sleep(waitAfter);
	}
	
	public void typeDown() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "Down", KeyEvent.VK_DOWN, KeyEvent.VK_DOWN, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void typeUp(int waitAfter) {
		typeUp();
		sleep(waitAfter);
	}
	
	public void typeUp() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "Up", KeyEvent.VK_UP, KeyEvent.VK_UP, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void space() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, " ", null, null, null, "Space", KeyEvent.VK_SPACE, KeyEvent.VK_SPACE, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void space(int waitAfter) {
		space();
		sleep(waitAfter);
	}
	
	public void backspace() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "Back Space", KeyEvent.VK_BACK_SPACE, KeyEvent.VK_BACK_SPACE, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void backspace(int waitAfter) {
		backspace();
		sleep(waitAfter);
		sleep(defaultSleepAfterAction);
	}

	public void delete() {
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_DOWN, null, null, null, null, null, null, "Delete", KeyEvent.VK_DELETE, KeyEvent.VK_DELETE, null, null, null, null, null);
		input.dispatchKeyEvent(DispatchKeyEventType.KEY_UP);
		sleep(defaultSleepAfterAction);
	}
	
	public void delete(int waitAfter) {
		delete();
		sleep(waitAfter);
		sleep(defaultSleepAfterAction);
	}

	public MarvinImage getScreenShot() {
		screenshot();
		return screen;
	}
	
	protected BufferedImage getScreenImage() {
		LayoutMetrics layoutMetrics = page.getLayoutMetrics();
	    double width = layoutMetrics.getContentSize().getWidth();
	    double height = layoutMetrics.getContentSize().getHeight();;
		Viewport viewport = new Viewport();
		viewport.setScale(1d);
		viewport.setX(0d);
		viewport.setY(0d);
		viewport.setWidth(width);
		viewport.setHeight(height);
		String data = "";
		try {
			//Version 4.0
			data = page.captureScreenshot(CaptureScreenshotFormat.PNG, 100, viewport, Boolean.FALSE, Boolean.TRUE);
			//Version 3.0
			//data = page.captureScreenshot(CaptureScreenshotFormat.PNG, 100, viewport, Boolean.TRUE);
		} catch (Exception e) {
			return getScreenImage();
		}
		BufferedImage image = null;
		byte[] imageByte;
		imageByte = Base64.getDecoder().decode(data);
		ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
		try {
			return ImageIO.read(bis);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private void screenshot() {
		screen.setBufferedImage(getScreenImage());
	}
	
	public MarvinImage screenCut(int x, int y, int width, int height) {
		MarvinImage img = new MarvinImage(getScreenImage());
		MarvinImage imgOut = new MarvinImage(width, height);
		crop(img, imgOut, x, y, width, height);
		return imgOut;
	}
	
	public void saveScreenshot(String path) {
		screenshot();
		MarvinImageIO.saveImage(screen, path);
	}
	
	public void print(String text) {
		System.out.println(text);
	}
	
	public void wait(int ms) {
		sleep(ms);
	}
	
	private void sleep(int sleep) {
		try {	
			Thread.sleep(sleep);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void typeKey(char c) {
		input.dispatchKeyEvent(DispatchKeyEventType.CHAR, null, null, ""+c, null, null, null, null, null, null, null, null, null, null, null);
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
//		System.out.println("start - X: "+ startX + " Y: "+startY);
//		System.out.println("imgIn: largura: "+imageIn.getWidth()+" - altura: "+ imageIn.getHeight());
//		System.out.println("altura: "+ searchWindowHeight);
//		System.out.println("largura: "+ searchWindowWidth);
		// Full image
		
		int colorThreshold = (int)(255 * colorSensibility);
		
		mainLoop:for(int y=startY; y<startY+searchWindowHeight; y++){
			for(int x=startX; x<startX+searchWindowWidth; x++){
				
				if(processed[x][y]){
					continue;
				}
				
				int notMatched=0;
				boolean match=true;
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
