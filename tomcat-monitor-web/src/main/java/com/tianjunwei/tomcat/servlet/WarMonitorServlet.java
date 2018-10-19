package com.tianjunwei.tomcat.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Manager;
import org.apache.catalina.Session;
import org.apache.catalina.util.ContextName;

import com.alibaba.fastjson.JSON;
import com.tianjunwei.tomcat.mointor.MyHostConfig;

public class WarMonitorServlet extends HttpServlet{
	
	private static final long serialVersionUID = 1L;
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException{
		 doGet(request, response);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException{
		
		//path名称为war的上下文名称
		String path = request.getParameter("path");
		ContextName cn = null;
	     if (path != null) {
	         cn = new ContextName(path, request.getParameter("version"));
	    }
	    //path路径为start、stop、reload、status和undeploy命令
	    String command = request.getPathInfo();
	    Host host = MyHostConfig.myHost;
	    PrintWriter writer = response.getWriter();
	    if(command == null) {
	    	writer.write("command is null");
	    }else if (command.equalsIgnoreCase("/start")) {
	    	boolean result = start(host, cn);
	    	writer.write(""+result);
		}else if (command.equalsIgnoreCase("/stop")) {
			boolean result = stop(host, cn);
			writer.write(""+result);
		}else if (command.equalsIgnoreCase("/reload")) {
			boolean result = reload(host, cn);
			writer.write(""+result);
		}else if (command.equalsIgnoreCase("/undeploy")) {
			boolean result = undeploy(host, cn);
			writer.write(""+result);
		}else if (command.equalsIgnoreCase("/status")) {
			String status = status(host);
			writer.write(status);
		}
	     writer.flush();
	}
	
	/*
	 * 获取所有war服务状态
	 */
	public static String status(Host host) {
		Map<String, String> warStatus = new HashMap<String, String>();
		if(host != null){
			Container [] containers = host.findChildren();
			
			for(int i=0; i< containers.length;i++){
				String name = containers[i].getName();
				String status = containers[i].getStateName();
		    	if(name != null && name.length() > 0){
		    		warStatus.put(name, status);
			    	
		    	}else {
					warStatus.put("/", status);
				}
			}
		}
		return JSON.toJSONString(warStatus);
	}
	
	/*
	 * 启动war服务
	 */
	public static boolean start(Host host,ContextName cn) {
		 Context context = (Context) host.findChild(cn.getName());
         if (context == null) {
           
             return false;
         }
         try {
			context.start();
		} catch (LifecycleException e) {
			return false;
		}
		return true;
		
	}
	
	/*
	 * 停止war服务
	 */
	public static boolean stop(Host host,ContextName cn) {
		 Context context = (Context) host.findChild(cn.getName());
         if (context == null) {
             return false;
         }
        
         try {
			context.stop();
		} catch (LifecycleException e) {
			return false;
		}
		return true;
		
	}

	/*
	 * 重新加载war服务
	 */
	public static boolean reload(Host host,ContextName cn) {
		Context context = (Context) host.findChild(cn.getName());
        if (context == null) {
            return false;
        }
        try {
        	 context.reload();
		} catch (Exception e) {
			return false;
		}
        return true;
	}
	
	/*
	 * 卸载war服务，并删除war文件
	 */
	public static boolean undeploy(Host host,ContextName cn) {
		 String name = cn.getName();
	     String baseName = cn.getBaseName();
	     Context context = (Context) host.findChild(name);
	     try {
			context.stop();
		} catch (LifecycleException e) {
			return false;
		}
	     try {
	    	 File war = new File(host.getAppBaseFile(), baseName + ".war");
             File dir = new File(host.getAppBaseFile(), baseName);
             if (war.exists() && !war.delete()) {
                 return false;
             } else if (dir.exists() && !undeployDir(dir)) {
                 return false;
             } 
		} catch (Exception e) {
			return false;
		}
	     return true;
	     
	}
	
	/*
	 * 清理超过某个时间的session
	 */
	public static boolean expire(Host host,ContextName cn, int idle) {
		 Context context = (Context) host.findChild(cn.getName());
		 if(context == null) {
			 return false;
		 }
		 Manager manager = context.getManager() ;
         if(manager == null) {
        	 return false;
         }
         int maxCount = 60;
         int histoInterval = 1;
         int maxInactiveInterval = context.getSessionTimeout();
         if (maxInactiveInterval > 0) {
             histoInterval = maxInactiveInterval / maxCount;
             if (histoInterval * maxCount < maxInactiveInterval)
                 histoInterval++;
             if (0 == histoInterval)
                 histoInterval = 1;
             maxCount = maxInactiveInterval / histoInterval;
             if (histoInterval * maxCount < maxInactiveInterval)
                 maxCount++;
         }
         Session [] sessions = manager.findSessions();
         for (int i = 0; i < sessions.length; i++) {
             int time = (int) (sessions[i].getIdleTimeInternal() / 1000L);
             if (idle >= 0 && time >= idle*60) {
                 sessions[i].expire();
             }
         }
         return true;
        
	}
	
	
	 protected static boolean undeployDir(File dir) {

	        String files[] = dir.list();
	        if (files == null) {
	            files = new String[0];
	        }
	        for (int i = 0; i < files.length; i++) {
	            File file = new File(dir, files[i]);
	            if (file.isDirectory()) {
	                if (!undeployDir(file)) {
	                    return false;
	                }
	            } else {
	                if (!file.delete()) {
	                    return false;
	                }
	            }
	        }
	        return dir.delete();
	    }
	
}
