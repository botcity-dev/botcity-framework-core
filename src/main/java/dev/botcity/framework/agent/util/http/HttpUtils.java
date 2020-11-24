
package dev.botcity.framework.agent.util.http;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class HttpUtils {

	public static String httpGetString(String url){
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(url);
		try {
			CloseableHttpResponse response = httpclient.execute(httpget);
			BufferedReader in = new BufferedReader(
		                             new InputStreamReader(
		                             response.getEntity().getContent(), "UTF-8"));
		        
		    StringBuilder builder = new StringBuilder();
		    String inputLine;
		        
		    while ((inputLine = in.readLine()) != null){
		    	builder.append(inputLine);
		    }
		    in.close();
		        
		    return builder.toString();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static byte[] httpGetByteArray(String url){
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(url);
		try {
			CloseableHttpResponse response = httpclient.execute(httpget);
			InputStream is = response.getEntity().getContent();
			byte[] bytes = IOUtils.toByteArray(is);
		   return bytes;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String httpPost(String url, Map<String, String> nameValuePairParameters) throws ClientProtocolException, IOException{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(url);
		//httpPost.setHeader("Content-Type", "application/json");
		//httpPost.setHeader("Accept", "application/json");
		
		if(nameValuePairParameters != null){
			List <NameValuePair> nvps = new ArrayList <NameValuePair>();
			Iterator<String> it = nameValuePairParameters.keySet().iterator();
			String key;
			while(it.hasNext()){
				key = it.next();
				nvps.add(new BasicNameValuePair(key, nameValuePairParameters.get(key)));
				
			}
			httpPost.setEntity(new UrlEncodedFormEntity(nvps));
		}
		CloseableHttpResponse response2 = httpclient.execute(httpPost);
		return readResponse(response2);
	}
	
	public static String httpPostJson(String url, String json) throws ClientProtocolException, IOException{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
		
		CloseableHttpResponse response2 = httpclient.execute(httpPost);
		return readResponse(response2);
	}
	
	private static String readResponse(CloseableHttpResponse response2) throws IOException{
		StringBuilder ret = new StringBuilder();
		try {
		    HttpEntity entity2 = response2.getEntity();

		    BufferedReader rd = new BufferedReader(new InputStreamReader(entity2.getContent()));
		      String line = "";
		      while ((line = rd.readLine()) != null) {
		       ret.append(line);
		      }
		      
		    // do something useful with the response body
		    // and ensure it is fully consumed
		    EntityUtils.consume(entity2);
		} finally {
		    response2.close();
		}
		
		return ret.toString();
	}
	
	public static String httpPostFile(String url, File file, Map<String, String> nameValuePairParameters) throws ClientProtocolException, IOException{
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
		//File and Parameters
		
		 FileBody body = new FileBody(file);
	     
		 MultipartEntityBuilder builder =  MultipartEntityBuilder.create();
		 builder.addPart("body", body);
		 HttpEntity entity = builder.build();
		
		 
		 RequestBuilder requestBuilder = RequestBuilder.post(url);
		 requestBuilder.setEntity(entity);
	     
		 Iterator<String> it = nameValuePairParameters.keySet().iterator();
		 while(it.hasNext()) {
			 String key = it.next();
			 requestBuilder.addParameter(key, nameValuePairParameters.get(key));
		 }
		 
		 HttpUriRequest request = requestBuilder.build();
        
         CloseableHttpResponse response = httpclient.execute(request);
 		return readResponse(response);
	}
}
