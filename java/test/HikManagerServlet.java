
package test;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.manager.Constants;
import org.apache.catalina.util.ContextName;
import org.apache.tomcat.util.modeler.Registry;
import org.apache.tomcat.util.res.StringManager;

/**
 * @author tianjunwei
 * @time 2017 ÉÏÎç10:36:38
 */
public class HikManagerServlet extends HttpServlet   {
	
	private static final long serialVersionUID = 6281626928886166709L;
	 /**
     * The associated host.
     */
    protected transient Host host = null;
    /**
     * The Wrapper container associated with this servlet.
     */
    protected transient Wrapper wrapper = null;
    
    /**
     * The associated deployer ObjectName.
     */
    protected ObjectName oname = null;
    
    /**
     * The Context container associated with our web application.
     */
    protected transient Context context = null;
    
    /**
     * The string manager for this package.
     */
    protected static final StringManager sm =
        StringManager.getManager(Constants.Package);
    
    /**
     * MBean server.
     */
    protected MBeanServer mBeanServer = null;
	
    @Override
    public void init() throws ServletException {
    	
    	 // Retrieve the MBean server
        mBeanServer = Registry.getRegistry(null, null).getMBeanServer();
    	
    };
    
    

	@Override
	public void doGet(HttpServletRequest request,
	                      HttpServletResponse response)
	        throws IOException, ServletException {


		// Identify the request parameters that we need
		// By obtaining the command from the pathInfo, per-command security can
		// be configured in web.xml
		String command = request.getPathInfo();

		String path = request.getParameter("path");
		ContextName cn = null;
	        if (path != null) {
	            cn = new ContextName(path, request.getParameter("version"));
	        }
	      Host host =  HostConfigTest.host;
	      Context context = (Context) host.findChild("/manager");
	      if(context != null){
	    	  try {
	    		  if("/start".equals(command)){
	    			  context.start();
	    		  }else if("/stop".equals(command)) {
	    			  context.stop();
				}
	    		} catch (LifecycleException e) {
	    			e.printStackTrace();
	    		}
	      }
	      PrintWriter writer = response.getWriter();
	            ObjectName queryHosts = null;
				try {
					queryHosts = new ObjectName("*:j2eeType=WebModule,*");
				} catch (MalformedObjectNameException e1) {
					e1.printStackTrace();
				}
	            Set<ObjectName> hostsON = mBeanServer.queryNames(queryHosts, null);

	            // Navigation menu
	            writer.print("<h1>");
	            writer.print("Application list");
	            writer.print("</h1>");

	            writer.print("<p>");
	            int count = 0;
	            Iterator<ObjectName> iterator = hostsON.iterator();
	            while (iterator.hasNext()) {
	                ObjectName contextON = iterator.next();
	                String webModuleName = contextON.getKeyProperty("name");
	                if (webModuleName.startsWith("//")) {
	                    webModuleName = webModuleName.substring(2);
	                }
	                int slash = webModuleName.indexOf("/");
	                if (slash == -1) {
	                    count++;
	                    continue;
	                }

	                writer.print("<a href=\"#" + (count++) + ".0\">");
	                writer.print("</a>");
	                if (iterator.hasNext()) {
	                    writer.print("<br>");
	                }

	            }
	            writer.print("</p>");

	            // Webapp list
	            count = 0;
	            iterator = hostsON.iterator();
	            while (iterator.hasNext()) {
	                ObjectName contextON = iterator.next();
	                writer.print("<a class=\"A.name\" name=\"" 
	                             + (count++) + ".0\">");
	                try {
						writeContext(writer, contextON, mBeanServer, 0);
					} catch (Exception e) {
						e.printStackTrace();
					}
	            }

	 }
	
	/**
     * Write context state.
     */
    protected static void writeContext(PrintWriter writer, 
                                       ObjectName objectName,
                                       MBeanServer mBeanServer, int mode)
        throws Exception {

        if (mode == 0){
            String webModuleName = objectName.getKeyProperty("name");
            String name = webModuleName;
            if (name == null) {
                return;
            }
            
            String hostName = null;
            String contextName = null;
            if (name.startsWith("//")) {
                name = name.substring(2);
            }
            int slash = name.indexOf("/");
            if (slash != -1) {
                hostName = name.substring(0, slash);
                contextName = name.substring(slash);
            } else {
                return;
            }

            ObjectName queryManager = new ObjectName
                (objectName.getDomain() + ":type=Manager,context=" + contextName 
                 + ",host=" + hostName + ",*");
            Set<ObjectName> managersON =
                mBeanServer.queryNames(queryManager, null);
            ObjectName managerON = null;
            Iterator<ObjectName> iterator2 = managersON.iterator();
            while (iterator2.hasNext()) {
                managerON = iterator2.next();
            }

            ObjectName queryJspMonitor = new ObjectName
                (objectName.getDomain() + ":type=JspMonitor,WebModule=" +
                 webModuleName + ",*");
            Set<ObjectName> jspMonitorONs =
                mBeanServer.queryNames(queryJspMonitor, null);

            // Special case for the root context
            if (contextName.equals("/")) {
                contextName = "";
            }

            writer.print("<h1>");
            writer.print(filter(name));
            writer.print("</h1>");
            writer.print("</a>");

            writer.print("<p>");
            Object startTime = mBeanServer.getAttribute(objectName,
                                                        "startTime");
            writer.print(" Start time: " +
                         new Date(((Long) startTime).longValue()));
            writer.print(" Startup time: ");
            writer.print(formatTime(mBeanServer.getAttribute
                                    (objectName, "startupTime"), false));
            writer.print(" TLD scan time: ");
            writer.print(formatTime(mBeanServer.getAttribute
                                    (objectName, "tldScanTime"), false));
            if (managerON != null) {
                writeManager(writer, managerON, mBeanServer, mode);
            }
            if (jspMonitorONs != null) {
                writeJspMonitor(writer, jspMonitorONs, mBeanServer, mode);
            }
            writer.print("</p>");

            String onStr = objectName.getDomain() 
                + ":j2eeType=Servlet,WebModule=" + webModuleName + ",*";
            ObjectName servletObjectName = new ObjectName(onStr);
            Set<ObjectInstance> set =
                mBeanServer.queryMBeans(servletObjectName, null);
            Iterator<ObjectInstance> iterator = set.iterator();
            while (iterator.hasNext()) {
                ObjectInstance oi = iterator.next();
                writeWrapper(writer, oi.getObjectName(), mBeanServer, mode);
            }

        } else if (mode == 1){
            // for now we don't write out the context in XML
        }

    }
    
    /**
     * Write detailed information about a manager.
     */
    public static void writeManager(PrintWriter writer, ObjectName objectName,
                                    MBeanServer mBeanServer, int mode)
        throws Exception {

        if (mode == 0) {
            writer.print("<br>");
            writer.print(" Active sessions: ");
            writer.print(mBeanServer.getAttribute
                         (objectName, "activeSessions"));
            writer.print(" Session count: ");
            writer.print(mBeanServer.getAttribute
                         (objectName, "sessionCounter"));
            writer.print(" Max active sessions: ");
            writer.print(mBeanServer.getAttribute(objectName, "maxActive"));
            writer.print(" Rejected session creations: ");
            writer.print(mBeanServer.getAttribute
                         (objectName, "rejectedSessions"));
            writer.print(" Expired sessions: ");
            writer.print(mBeanServer.getAttribute
                         (objectName, "expiredSessions"));
            writer.print(" Longest session alive time: ");
            writer.print(formatSeconds(mBeanServer.getAttribute(
                                                    objectName,
                                                    "sessionMaxAliveTime")));
            writer.print(" Average session alive time: ");
            writer.print(formatSeconds(mBeanServer.getAttribute(
                                                    objectName,
                                                    "sessionAverageAliveTime")));
            writer.print(" Processing time: ");
            writer.print(formatTime(mBeanServer.getAttribute
                                    (objectName, "processingTime"), false));
        } else if (mode == 1) {
            // for now we don't write out the wrapper details
        }

    }


    /**
     * Write JSP monitoring information.
     */
    public static void writeJspMonitor(PrintWriter writer,
                                       Set<ObjectName> jspMonitorONs,
                                       MBeanServer mBeanServer,
                                       int mode)
            throws Exception {

        int jspCount = 0;
        int jspReloadCount = 0;

        Iterator<ObjectName> iter = jspMonitorONs.iterator();
        while (iter.hasNext()) {
            ObjectName jspMonitorON = iter.next();
            Object obj = mBeanServer.getAttribute(jspMonitorON, "jspCount");
            jspCount += ((Integer) obj).intValue();
            obj = mBeanServer.getAttribute(jspMonitorON, "jspReloadCount");
            jspReloadCount += ((Integer) obj).intValue();
        }

        if (mode == 0) {
            writer.print("<br>");
            writer.print(" JSPs loaded: ");
            writer.print(jspCount);
            writer.print(" JSPs reloaded: ");
            writer.print(jspReloadCount);
        } else if (mode == 1) {
            // for now we don't write out anything
        }
    }


    /**
     * Write detailed information about a wrapper.
     */
    public static void writeWrapper(PrintWriter writer, ObjectName objectName,
                                    MBeanServer mBeanServer, int mode)
        throws Exception {

        if (mode == 0) {
            String servletName = objectName.getKeyProperty("name");
            
            String[] mappings = (String[]) 
                mBeanServer.invoke(objectName, "findMappings", null, null);
            
            writer.print("<h2>");
            writer.print(filter(servletName));
            if ((mappings != null) && (mappings.length > 0)) {
                writer.print(" [ ");
                for (int i = 0; i < mappings.length; i++) {
                    writer.print(filter(mappings[i]));
                    if (i < mappings.length - 1) {
                        writer.print(" , ");
                    }
                }
                writer.print(" ] ");
            }
            writer.print("</h2>");
            
            writer.print("<p>");
            writer.print(" Processing time: ");
            writer.print(formatTime(mBeanServer.getAttribute
                                    (objectName, "processingTime"), true));
            writer.print(" Max time: ");
            writer.print(formatTime(mBeanServer.getAttribute
                                    (objectName, "maxTime"), false));
            writer.print(" Request count: ");
            writer.print(mBeanServer.getAttribute(objectName, "requestCount"));
            writer.print(" Error count: ");
            writer.print(mBeanServer.getAttribute(objectName, "errorCount"));
            writer.print(" Load time: ");
            writer.print(formatTime(mBeanServer.getAttribute
                                    (objectName, "loadTime"), false));
            writer.print(" Classloading time: ");
            writer.print(formatTime(mBeanServer.getAttribute
                                    (objectName, "classLoadTime"), false));
            writer.print("</p>");
        } else if (mode == 1){
            // for now we don't write out the wrapper details
        }

    }


    /**
     * Filter the specified message string for characters that are sensitive
     * in HTML.  This avoids potential attacks caused by including JavaScript
     * codes in the request URL that is often reported in error messages.
     *
     * @param obj The message string to be filtered
     */
    public static String filter(Object obj) {

        if (obj == null)
            return ("?");
        String message = obj.toString();

        char content[] = new char[message.length()];
        message.getChars(0, message.length(), content, 0);
        StringBuilder result = new StringBuilder(content.length + 50);
        for (int i = 0; i < content.length; i++) {
            switch (content[i]) {
            case '<':
                result.append("&lt;");
                break;
            case '>':
                result.append("&gt;");
                break;
            case '&':
                result.append("&amp;");
                break;
            case '"':
                result.append("&quot;");
                break;
            default:
                result.append(content[i]);
            }
        }
        return (result.toString());

    }


    /**
     * Display the given size in bytes, either as KB or MB.
     *
     * @param mb true to display megabytes, false for kilobytes
     */
    public static String formatSize(Object obj, boolean mb) {

        long bytes = -1L;

        if (obj instanceof Long) {
            bytes = ((Long) obj).longValue();
        } else if (obj instanceof Integer) {
            bytes = ((Integer) obj).intValue();
        }

        if (mb) {
            StringBuilder buff = new StringBuilder();
            if (bytes < 0) {
                buff.append('-');
                bytes = -bytes;
            }
            long mbytes = bytes / (1024 * 1024);
            long rest = 
                ((bytes - (mbytes * (1024 * 1024))) * 100) / (1024 * 1024);
            buff.append(mbytes).append('.');
            if (rest < 10) {
                buff.append('0');
            }
            buff.append(rest).append(" MB");
            return buff.toString();
        } else {
            return ((bytes / 1024) + " KB");
        }

    }


    /**
     * Display the given time in ms, either as ms or s.
     *
     * @param seconds true to display seconds, false for milliseconds
     */
    public static String formatTime(Object obj, boolean seconds) {

        long time = -1L;

        if (obj instanceof Long) {
            time = ((Long) obj).longValue();
        } else if (obj instanceof Integer) {
            time = ((Integer) obj).intValue();
        }

        if (seconds) {
            return ((((float) time ) / 1000) + " s");
        } else {
            return (time + " ms");
        }
    }


    /**
     * Formats the given time (given in seconds) as a string.
     *
     * @param obj Time object to be formatted as string
     *
     * @return String formatted time
     */
    public static String formatSeconds(Object obj) {

        long time = -1L;

        if (obj instanceof Long) {
            time = ((Long) obj).longValue();
        } else if (obj instanceof Integer) {
            time = ((Integer) obj).intValue();
        }

        return (time + " s");
    }
	
}
