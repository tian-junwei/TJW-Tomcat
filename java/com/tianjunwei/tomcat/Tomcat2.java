package com.tianjunwei.tomcat;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.startup.Tomcat.FixContextListener;


public class Tomcat2 {

	public static void main(String[] args) {
		System.setProperty("catalina.base", System.getProperty("user.dir"));
		System.err.println("start TestTomcat");
		//创建连接器
		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(8080);
		
		//创建上下文容器
	    Context context = new StandardContext();
	    context.setPath("/context");

	    //创建servlet包装类
	    Wrapper defaultServlet = context.createWrapper();
		defaultServlet.setName("test");
		defaultServlet.setServletClass("com.tianjunwei.servlet.TestServlet");
		defaultServlet.setLoadOnStartup(1);
		defaultServlet.setOverridable(true);
		
		//上下文容器中添加servlet
		context.addChild(defaultServlet);
		context.addServletMappingDecoded("/test", "test");
		context.addLifecycleListener(new FixContextListener());
	    
		//创建host
	    Host host = new StandardHost();
	    host.addChild(context);
	    host.setName("localhost");
	    host.setAppBase(System.getProperty("user.dir") + File.separator + ".");
	    
	    //创建引擎
	    Engine engine = new StandardEngine();
	    engine.setName("Tomcat");
	    engine.addChild(host);
	    engine.setDefaultHost("localhost");
	    engine.getCatalinaBase();

	    //创建一个服务，作为服务器server中的一个服务
	    Service service = new StandardService();
	    service.setName("Tomcat");
		service.setContainer(engine );
		service.addConnector(connector);
		
		//创建服务器
	    Server server = new StandardServer();
	    server.setPort(-1);
	    server.addService(service);
	    server.setCatalinaBase(new File(System.getProperty("user.dir")));
	    server.setCatalinaHome(new File(System.getProperty("user.dir")));

	    if (server instanceof Lifecycle) {
	      try {
	    	//初始化服务
	        server.init();
	        //启动服务
	        ((Lifecycle) server).start();
	        //挂起服务
	        server.await();
	      }
	      catch (LifecycleException e) {
	        e.printStackTrace(System.out);
	      }
	    }

	    // Shut down the server
	    if (server instanceof Lifecycle) {
	      try {
	        ((Lifecycle) server).stop();
	      }
	      catch (LifecycleException e) {
	        e.printStackTrace(System.out);
	      }
	    }
	}

}
