package dev.botcity.framework.agent;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import org.apache.commons.io.FileUtils;
import org.marvinproject.framework.image.MarvinImage;

import dev.botcity.framework.agent.util.http.HttpUtils;



public abstract class AbstractDesktopAgent implements DesktopAgent{

	protected Integer					taskId;
	protected String 					token;
	protected String 					server;
	protected Map<String, MarvinImage>	mapImages;
	protected Map<String, Object>		params;
	//private Map<String, String> 		mapParameters;
	
	@Override
	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}
	
	@Override
	public Integer getTaskId() {
		return this.taskId;
	}
	
	@Override
	public void setToken(String token) {
		this.token = token;
	}
	
	@Override
	public void setServer(String server) {
		this.server = server;
	}
	
	@Override
	public void setImageMap(Map<String, MarvinImage>	mapImages) {
		this.mapImages = mapImages;
	}
	
	protected MarvinImage getImage(String id) throws IOException {
		MarvinImage img = mapImages.get(id);
		
		if(img == null) {	throw new IOException("image not found: "+id);	}
		return img;
	}
	
	public MarvinImage getImage2(String label) throws IOException {
		String path = label+".png";
		if (path != null) {
			long time = System.currentTimeMillis();
			ImageIcon img = new ImageIcon(getClass().getClassLoader().getResource(path));
			MarvinImage m = new MarvinImage(toBufferedImage(img.getImage()));
			System.out.println("time:"+(System.currentTimeMillis()-time));
			return m;
		} else {
			throw new RuntimeException("Resource not found:" + label);
		}
	}
	
	public static BufferedImage toBufferedImage(Image img)
	{
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
	
	
	@Override
	public void setParameters(Map<String, Object> params) {
		this.params = params;
	}
	
	public Object getParameter(String label) {
		return this.params.get(label);
	}
	
	protected void loadParameters(String path) throws IOException {
		params = new HashMap<String, Object>();
		String strParams = FileUtils.readFileToString(new File(path), "UTF-8");
		
		String[] lines = strParams.split("\r\n");
		
		for(int i=0; i<lines.length; i++) {
			String line = lines[i];
			
			if(line.trim().startsWith("#")) {
				continue;
			}
			
			params.put(line.substring(0, line.indexOf("=")), line.substring(line.indexOf("=")+1));
			System.out.println(line.substring(0, line.indexOf("="))+":"+(line.substring(line.indexOf("=")+1)));
		}
	}
	
	@Override
	public void logEvent(String detail) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("access_token", token);
			map.put("detail", detail);
	
			String strResp = HttpUtils.httpPost(server + "/app/api/log", map);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void finishTask(Integer processedItems, DesktopAgent.FinishStatus status) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("access_token", token);
			
			map.put("taskId", this.taskId.toString());
			map.put("finishStatus", status.toString());
			map.put("processedItems", processedItems.toString());
			String strResp = HttpUtils.httpPost(server + "/app/api/finishTask", map);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
