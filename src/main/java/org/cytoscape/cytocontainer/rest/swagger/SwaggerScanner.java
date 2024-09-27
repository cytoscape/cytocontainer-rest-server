package org.cytoscape.cytocontainer.rest.swagger;

import io.swagger.v3.jaxrs2.integration.JaxrsApplicationAndResourcePackagesAnnotationScanner;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author churas
 */
public class SwaggerScanner extends JaxrsApplicationAndResourcePackagesAnnotationScanner {

	static Logger _logger = LoggerFactory.getLogger(SwaggerScanner.class.getSimpleName());

	/**
	 * Call the super implementation of classes and then only keep classes
	 * whose package name starts with org.ndex
	 * @return 
	 */
	@Override
	public Set<Class<?>> classes() {
		Set<Class<?>> unprocessed_classes = super.classes();
		Set<Class<?>> output = new HashSet<>();
		for (Class c : unprocessed_classes){
			if (!c.getPackageName().startsWith("org.cytoscape.cytocontainer") ||
					c.getCanonicalName().startsWith("org.cytoscape.cytocontainer.rest.services.CustomOpenApiResource")){
				continue;
			}
			_logger.info("Adding to swagger " + c.getCanonicalName());
			output.add(c);
		}
		return output;
	}
}
