package com.tianjunwei.tomcat.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleState;

import com.tianjunwei.tomcat.mointor.MyHostConfig;

public class MonitorServlet extends HttpServlet{
	
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException{
		String result = "{";
		Host hikHost = MyHostConfig.myHost;
		if(hikHost != null){
			Container [] containers = hikHost.findChildren();
			
			for(int i=0; i< containers.length;i++){
				String name = containers[i].getName();
		    	if(name != null && name.length() > 0){
		    		name = name.substring(1);
			    	result = result +"\""+name+"\""+":";
			    	String status = containers[i].getStateName();
			    	if(status.equals(LifecycleState.STARTED.name()) && (i<containers.length-1)){
			    		if(i+1 < containers.length){
			    			result = result +"0"+",";
			    		}else {
			    			result = result +"0";
						}
			    		
			    	}else{
			    		if(i+1 < containers.length){
			    			result = result +"1"+",";
			    		}else {
			    			result = result +"1";
						}
					}
			    	
		    	}
			}
		   
		}
		result=result+"}";
		PrintWriter writer = response.getWriter();
		writer.write(result);
		writer.flush();
	}

}
