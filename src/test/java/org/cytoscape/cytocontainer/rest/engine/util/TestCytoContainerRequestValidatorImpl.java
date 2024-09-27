package org.cytoscape.cytocontainer.rest.engine.util;

import com.fasterxml.jackson.databind.node.TextNode;
import java.util.HashMap;
import java.util.HashSet;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.Parameter;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;

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
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        aParams.add(cp);
        cp = new Parameter();
        cp.setName("--fo");
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
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "blah");
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
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType("someunknowntype");
        aParams.add(cp);
       
        cda.setParameters(aParams);
        
        CytoContainerRequest cdr = new CytoContainerRequest();
        HashMap<String, String> cParams = new HashMap<>();
        cParams.put("--somearg", "blah");
        cdr.setParameters(cParams);
        cdr.setData(new TextNode("hi"));
        CytoContainerRequestValidatorImpl validator = new CytoContainerRequestValidatorImpl();
        ErrorResponse er = validator.validateRequest(cda, cdr);
        assertEquals("Unknown parameter type", er.getMessage());
    }
    
    @Test
    public void testSingleFlagParameterValid(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.FLAG_TYPE);
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
    public void testSingleFlagParameterWithWhiteSpaceValueButValid(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.FLAG_TYPE);
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
    public void testSingleFlagParameterPassedValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.FLAG_TYPE);
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
    public void testSingleStringParameterValueIsNull(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.STRING_VALIDATION);
        
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
    public void testSingleStringParameterValueIsEmptyWhiteSpace(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.STRING_VALIDATION);
        
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
    public void testSingleStringParameterNoRegex(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.STRING_VALIDATION);
        
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
    public void testSingleStringParameterInvalidRegex(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.STRING_VALIDATION);
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
    public void testSingleStringParameterPassesRegex(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.STRING_VALIDATION);
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
    public void testSingleStringParameterFailsRegexNoHelp(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.STRING_VALIDATION);
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
    public void testSingleStringParameterFailsRegexWithHelp(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.STRING_VALIDATION);
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
    public void testSingleNumericParameterNullValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.NUMBER_VALIDATION);
        
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
    public void testSingleNumericParameterWhitespaceValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.NUMBER_VALIDATION);
        
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
    public void testSingleDigitsParameterValidValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.DIGITS_VALIDATION);
        
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
    public void testSingleDigitsParameterNegativeValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.DIGITS_VALIDATION);
        
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
    public void testSingleDigitsParameterInvalidFloatValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.DIGITS_VALIDATION);
        
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
    public void testSingleDigitsParameterValidValueWithMinMaxSet(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.DIGITS_VALIDATION);
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
    public void testSingleDigitsParameterValidValueWithValueBelowMin(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.DIGITS_VALIDATION);
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
    public void testSingleDigitsParameterValidValueWithValueAboveMax(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.DIGITS_VALIDATION);
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
    public void testSingleNumberParameterInValidValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.NUMBER_VALIDATION);
        
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
    public void testSingleNumberParameterScientificNotationValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.NUMBER_VALIDATION);
        
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
    public void testSingleNumberParameterValidValue(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.NUMBER_VALIDATION);
        
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
    public void testSingleNumberParameterValidValueWithMinMaxSet(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.NUMBER_VALIDATION);
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
    public void testSingleNumberParameterValidValueWithValueBelowMin(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.NUMBER_VALIDATION);
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
    public void testSingleNumberParameterValidValueWithValueAboveMax(){
        CytoContainerAlgorithm cda = new CytoContainerAlgorithm();
        cda.setName("somealgo");
        HashSet<Parameter> aParams = new HashSet<>();
        Parameter cp = new Parameter();
        cp.setName("--somearg");
        cp.setType(Parameter.VALUE_TYPE);
        cp.setValidationType(Parameter.NUMBER_VALIDATION);
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
