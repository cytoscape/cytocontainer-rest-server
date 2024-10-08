package org.cytoscape.cytocontainer.rest;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class RequestLoggingFilter implements Filter {
	public static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
	public static final String USER_AGENT = "User-Agent";
	public static final String REQUEST_LOGGER_NAME = "requestlog";
	
	private static final Logger _requestLogger = LoggerFactory.getLogger(REQUEST_LOGGER_NAME);

    /**
     * Default constructor.
     */
    public RequestLoggingFilter() {
        // TODO Auto-generated constructor stub
    }
 
    /**
     * @see Filter#destroy()
     */
    public void destroy() {
        // TODO Auto-generated method stub
    }
 
    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
 
		HttpServletRequest request = (HttpServletRequest)servletRequest;
		String remoteIp = this.getRequestorIpAddress(request);
		String tid = getTimestampAndThreadId();
		
		//log the request
		_requestLogger.info(tid + "\t[start]\t[" + request.getMethod() 
				+ "]\t[]\t[" + remoteIp + "]\t[" + getUserAgent(request)
				+ "]\t[]\t[" + request.getRequestURI() + "]\t[" + request.getQueryString() + "]");
		
        // pass the request along the filter chain
        chain.doFilter(servletRequest, servletResponse);
		
		//log the response
		HttpServletResponse response = (HttpServletResponse)servletResponse;
		_requestLogger.info(tid + "\t[end] [status: "
				+ Integer.toString(response.getStatus()) + "]");
	
    }
	
	private String getTimestampAndThreadId(){
		return "[tid:" + Long.toString(System.currentTimeMillis()) + "-"
				+ Long.toString(Thread.currentThread().getId()) + "]";
	}
	/**
	 * Gets IP Address of request checking the X_FORWARDED_FOR header first
	 * and falling back to request.getRemoteAddr() if null
	 * @param request
	 * @return IP Address as String
	 */
	private String getRequestorIpAddress(HttpServletRequest request){
		String remoteIp = request.getHeader(X_FORWARDED_FOR);
		if (remoteIp != null){
			return remoteIp;
		}
		return request.getRemoteAddr();
	}
	
	private String getUserAgent(HttpServletRequest request){
		String userAgent = request.getHeader(USER_AGENT);
		if (userAgent != null){
			return userAgent;
		}
		return "";
	}
 
    /**
     * @see Filter#init(FilterConfig)
     */
    public void init(FilterConfig fConfig) throws ServletException {
        // TODO Auto-generated method stub
    }
}
