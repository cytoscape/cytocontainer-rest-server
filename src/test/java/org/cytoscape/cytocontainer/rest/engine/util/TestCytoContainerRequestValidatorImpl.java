package org.cytoscape.cytocontainer.rest.engine.util;

import com.fasterxml.jackson.databind.node.TextNode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.AlgorithmParameter;
import org.cytoscape.cytocontainer.rest.model.CytoContainerParameter;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;
import org.cytoscape.cytocontainer.rest.model.exceptions.CytoContainerException;
import static org.junit.Assert.fail;

/**
 *
 * @author churas
 */
public class TestCytoContainerRequestValidatorImpl {

    @Test
    public void testNullAlgorithmAndNullRequest(){
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(null, null);
        assertEquals("Algorithm is null", er.getMessage());
    }
    
    @Test
    public void testNullRequest(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, null);
        assertEquals("Request is null", er.getMessage());
    }
    
    @Test
    public void testDataInRequestIsNull(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("foo");
        CytoContainerRequest cdr = new CytoContainerRequest();
        
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("No data passed in with request", er.getMessage());
    }
    
    @Test
    public void testAlgorithmNameIsNull(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        CytoContainerRequest cdr = new CytoContainerRequest();
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Algorithm name is null", er.getMessage());
    }
    
    @Test
    public void testNoParameters(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("foo");

        CytoContainerRequest cdr = new CytoContainerRequest();
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testNonMatchingParameterCauseThereAreNoParameters(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--foo", "blah");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Invalid custom parameter", er.getMessage());
        assertEquals("--foo is not a custom parameter for algorithm: "
                + cda.getName(), er.getDescription());
         
    }
    
    @Test
    public void testNonMatchingParameter(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        aParams.add(cp);
        cp = new CytoContainerParameter();
        cp.setFlag("--fo");
        aParams.add(cp);
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--foo", "blah");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Invalid custom parameter", er.getMessage());
        assertEquals("--foo is not a custom parameter for algorithm: "
                + cda.getName(), er.getDescription());
        
    }
    
    @Test
    public void testAlgorithmParameterTypeNotSet(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
		Map<String, String> pMap = new HashMap<>();
		pMap.put("something", "--somearg");
		cda.setParameterFlagMap(pMap);
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setDisplayName("something");
		cp.setFlag("--somearg");
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("something", "blah");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testAlgorithmParameterUnknownType(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
		Map<String, String> pMap = new HashMap<>();
		pMap.put("something", "--somearg");
		cda.setParameterFlagMap(pMap);
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
		try {
			cp.setType("someunknowntype");
			fail("expected exception");
		} catch(CytoContainerException cce){
			
		}
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("foo", "blah");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Invalid custom parameter", er.getMessage());
    }
    
    @Test
    public void testSingleFlagParameterValid() throws CytoContainerException {
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.FLAG_TYPE);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", null);
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
        
    }
    
    @Test
    public void testSingleFlagParameterWithWhiteSpaceValueButValid() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.FLAG_TYPE);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", " ");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
        
    }
    
    @Test
    public void testSingleFlagParameterPassedValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.FLAG_TYPE);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "foo");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Flag only given a value", er.getMessage());
    }
    
    @Test
    public void testSingleStringParameterValueIsNull() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.STRING_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", null);
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Parameter missing value", er.getMessage());
    }
    
    @Test
    public void testSingleStringParameterValueIsEmptyWhiteSpace() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.STRING_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "  ");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Parameter missing value", er.getMessage());
    }
    
    @Test
    public void testSingleStringParameterNoRegex() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.STRING_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "foo");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testSingleStringParameterInvalidRegex() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.STRING_VALIDATION);
        cp.setValidationRegex("[");
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "foo");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Malformed validation expression", er.getMessage());
    }
    
    @Test
    public void testSingleStringParameterPassesRegex() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.STRING_VALIDATION);
        cp.setValidationRegex("foo|bar");
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "foo");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testSingleStringParameterFailsRegexNoHelp() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.STRING_VALIDATION);
        cp.setValidationRegex("^x.*v$");
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "foo");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Invalid parameter value", er.getMessage());
    }
    
    @Test
    public void testSingleStringParameterFailsRegexWithHelp() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.STRING_VALIDATION);
        cp.setValidationRegex("^x.*v$");
        cp.setValidationHelp("some help");
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "foo");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("some help", er.getMessage());
    }
    
    @Test
    public void testSingleNumericParameterNullValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.NUMBER_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", null);
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Parameter missing value", er.getMessage());
    }
    
    @Test
    public void testSingleNumericParameterWhitespaceValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.NUMBER_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", " ");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Parameter missing value", er.getMessage());
    }
    
    @Test
    public void testSingleDigitsParameterValidValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.DIGITS_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "10");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testSingleDigitsParameterNegativeValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.DIGITS_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "-45");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testSingleDigitsParameterInvalidFloatValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.DIGITS_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "10.5");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("10.5 does not appear to be a whole number", er.getDescription());
    }
    
    @Test
    public void testSingleDigitsParameterValidValueWithMinMaxSet() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.DIGITS_VALIDATION);
        cp.setMinValue(9);
        cp.setMaxValue(11);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "10");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testSingleDigitsParameterValidValueWithValueBelowMin() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.DIGITS_VALIDATION);
        cp.setMinValue(9);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "8");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("8 is less then minimum value: 9 allowed for this parameter", er.getDescription());
    }
    
    @Test
    public void testSingleDigitsParameterValidValueWithValueAboveMax() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.DIGITS_VALIDATION);
        cp.setMaxValue(7);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "8");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("8 is greater then maximum value: 7 allowed for this parameter", er.getDescription());
    }
    
    @Test
    public void testSingleNumberParameterInValidValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.NUMBER_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "xxx");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("xxx is not a valid number", er.getDescription());
    }
    
    @Test
    public void testSingleNumberParameterScientificNotationValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.NUMBER_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "1e-5");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testSingleNumberParameterValidValue() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.NUMBER_VALIDATION);
        
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "10.5");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testSingleNumberParameterValidValueWithMinMaxSet() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.NUMBER_VALIDATION);
        cp.setMinValue(1.5);
        cp.setMaxValue(6.3);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "4.2");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals(null, er);
    }
    
    @Test
    public void testSingleNumberParameterValidValueWithValueBelowMin() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.NUMBER_VALIDATION);
        cp.setMinValue(9.6);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "8.9");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("8.9 is less then minimum value: 9.6 allowed for this parameter", er.getDescription());
    }
    
     @Test
    public void testSingleNumberParameterValidValueWithValueAboveMax() throws CytoContainerException{
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<AlgorithmParameter> aParams = new HashSet<>();
        CytoContainerParameter cp = new CytoContainerParameter();
        cp.setFlag("--somearg");
        cp.setType(AlgorithmParameter.TEXT_TYPE);
        cp.setValidationType(AlgorithmParameter.NUMBER_VALIDATION);
        cp.setMaxValue(7.2);
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "8.3");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("8.3 is greater then maximum value: 7.2 allowed for this parameter", er.getDescription());
    }
    
}
