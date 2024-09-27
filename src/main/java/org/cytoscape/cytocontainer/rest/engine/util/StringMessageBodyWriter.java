package org.cytoscape.cytocontainer.rest.engine.util;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageBodyWriter that if given a string just writes it out and assumes
 * if the type is json, yaml, xml, or html the data is in correct format
 * 
 * TODO: Look into this more because jackson should be doing this for us automatically
 * 
 * @author churas
 */
@Provider
@Produces({"application/json", "application/yaml", "application/xml", "text/html"})
public class StringMessageBodyWriter implements MessageBodyWriter<String> {

	static Logger _logger = LoggerFactory.getLogger(StringMessageBodyWriter.class.getSimpleName());

	@Override
	public boolean isWriteable(Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
		
		if (type == null){
			return false;
		}
		if (type.equals(String.class)){
			return true;
		}
		_logger.debug("Not writable because type is "
				+ type.getCanonicalName() + " and not a string");
		return false;
	}

	@Override
	public void writeTo(String t, Class<?> type, Type type1, Annotation[] antns, MediaType mt, 
			MultivaluedMap<String, Object> mm, OutputStream out) throws IOException, WebApplicationException {
        _logger.debug("Writing String");
		out.write(t.getBytes());
		out.flush();
	}
	
}
