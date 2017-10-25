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
		Connector connector = new Connector("HTTP/1.1");
		connector.setPort(8080);
		
	    Context context = new StandardContext();
	    context.setPath("/context");
	    //context.setDocBase("app1");

	    Wrapper defaultServlet = context.createWrapper();
		defaultServlet.setName("test");
		defaultServlet.setServletClass("com.tianjunwei.servlet.TestServlet");
		defaultServlet.setLoadOnStartup(1);
		defaultServlet.setOverridable(true);
		context.addChild(defaultServlet);
		context.addServletMappingDecoded("/test", "test");
		context.addLifecycleListener(new FixContextListener());
	    
	    Host host = new StandardHost();
	    host.addChild(context);
	    host.setName("localhost");
	    host.setAppBase(System.getProperty("user.dir") + File.separator + ".");
	    
	    Engine engine = new StandardEngine();
	    engine.setName("Tomcat");
	    engine.addChild(host);
	    engine.setDefaultHost("localhost");
	    engine.getCatalinaBase();

	    Service service = new StandardService();
	    service.setName("Tomcat");
		service.setContainer(engine );
		service.addConnector(connector);
		
	    Server server = new StandardServer();
	    server.setPort(-1);
	    server.addService(service);
	    server.setCatalinaBase(new File(System.getProperty("user.dir")));
	    server.setCatalinaHome(new File(System.getProperty("user.dir")));

	    // Start the new server
	    if (server instanceof Lifecycle) {
	      try {
	        server.init();
	        ((Lifecycle) server).start();
	        server.await();
	        // the program waits until the await method returns,
	        // i.e. until a shutdown command is received.
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
