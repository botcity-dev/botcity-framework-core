package dev.botcity.framework.agent;

import java.util.Map;

import org.marvinproject.framework.image.MarvinImage;

public interface DesktopAgent {
	
	public enum FinishStatus{SUCCESS, FAILED};
	
	void setTaskId(Integer taskId);
	Integer getTaskId();
	void setImageMap(Map<String, MarvinImage> map);
	void setParameters(Map<String, Object> params);
	void action();
	void logEvent(String detail);
	void finishTask(Integer processedItems, DesktopAgent.FinishStatus status);
	void setToken(String token);
	void setServer(String server);
}
