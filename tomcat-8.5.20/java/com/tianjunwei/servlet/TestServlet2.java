package com.tianjunwei.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet2 extends HttpServlet{
	private static final long serialVersionUID = 1L;

	public void doGet(HttpServletRequest request, 
		    HttpServletResponse response) 
		    throws ServletException, IOException {
		    
		    response.setContentType("text/html");
		    PrintWriter out = response.getWriter();
		    out.println("<html>");
		    out.println("<head>");
		    out.println("<title>Modern Servlet2</title>");
		    out.println("</head>");
		    out.println("<body>");
		    out.println("Modern Servlet2");
		    out.println("<h2>Headers</h2>");

		    out.println("<br><h2>Method</h2");
		    out.println("<br>" + request.getMethod());

		    out.println("<br><h2>Parameters</h2");
		    
		    out.println("<br><h2>Query String</h2");
		    out.println("<br>" + request.getQueryString());

		    out.println("<br><h2>Request URI</h2");
		    out.println("<br>" + request.getRequestURI());

		    out.println("</body>");
		    out.println("</html>");

		  }
	
}
