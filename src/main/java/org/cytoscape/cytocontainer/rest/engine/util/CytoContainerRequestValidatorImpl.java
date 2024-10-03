package org.cytoscape.cytocontainer.rest.engine.util;

import java.util.Map;
import org.cytoscape.cytocontainer.rest.model.CytoContainerAlgorithm;
import org.cytoscape.cytocontainer.rest.model.CytoContainerRequest;
import org.cytoscape.cytocontainer.rest.model.AlgorithmParameter;
import org.cytoscape.cytocontainer.rest.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.cytoscape.cytocontainer.rest.model.CytoContainerParameter;

/**
 * Performs validation of the {@link org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest}
 * via the {@link #validateRequest(org.ndexbio.communitydetection.rest.model.CommunityDetectionAlgorithm, org.ndexbio.communitydetection.rest.model.CommunityDetectionRequest) } method
 * 
 * 
 * @author churas
 */
public class CytoContainerRequestValidatorImpl implements CytoContainerRequestValidator  {

    private static final Logger _log
            = LoggerFactory.getLogger(CytoContainerRequestValidatorImpl.class.getName());
    
    /**
     * Verifies custom parameters in the request 'cdr' match those in the algorithm 'cda'
     * by first checking for a match by name, then utilizing the validation methods to verify
     * data is the correct type and passes any min/max rules as well as any regular expressions
     * 
     * @param cda The algorithm to run
     * @param cdr The request to validate
     * @return null if no error otherwise {@link org.ndexbio.communitydetection.rest.model.ErrorResponse} object with issues found
     */
    @Override
    public ErrorResponse validateRequest(CytoContainerAlgorithm cda, CytoContainerRequest cdr) {
        if (cda == null){
            ErrorResponse er = new ErrorResponse();
            er.setMessage("Algorithm is null");
            er.setDescription("Algorithm passed in is null");
            return er;
        }
        
        if (cdr == null){
            ErrorResponse er = new ErrorResponse();
            er.setMessage("Request is null");
            er.setDescription("Request passed in is null");
            return er;
        }
        
        if (cda.getName() == null){
            ErrorResponse er = new ErrorResponse();
            er.setMessage("Algorithm name is null");
            er.setDescription("Every algorithm should have a name to identify it");
            return er;
            
        }
        if (cdr.getData() == null){
            ErrorResponse er = new ErrorResponse();
            er.setMessage("No data passed in with request");
            er.setDescription("All requests require some data to be set in the data field");
            return er;
        }
        
        // if there are no custom parameters just return
        Map<String, String> customParams = cdr.getParameters();
        if (customParams == null){
            return null;
        }
        
        //validate the custom parameters
        Map<String,AlgorithmParameter> params = cda.getParameterMap();
        for (String pName : cdr.getParameters().keySet()){
            if (params == null || params.containsKey(pName) == false){
                ErrorResponse er = new ErrorResponse();
                er.setMessage("Invalid custom parameter");
                er.setDescription(pName + " is not a custom parameter for algorithm: " + cda.getName());
                return er;
            }
            ErrorResponse er = validateParameter((CytoContainerParameter)params.get(pName), customParams.get(pName));
            if (er != null){
                return er;
            }
            
        }
        
        // TODO: stretch goal, one could look at start of data to make sure its correct type
        return null;  
    }
    
    /**
     * Validate the parameter
     * @param algoParam {@link org.ndexbio.communitydetection.rest.model.CustomParameter} containing rules
     *                  the {@code userParamValue} should adhere to
     * @param userParamValue 
     * @return {@code null} if its a valid parameter otherwise {@link org.ndexbio.communitydetection.rest.model.ErrorResponse} denoting the problem
     */
    private ErrorResponse validateParameter(final CytoContainerParameter algoParam,
            final String userParamValue){

        //for string parameter
        if (algoParam.getType() == null ||
                AlgorithmParameter.STRING_VALIDATION.equalsIgnoreCase(algoParam.getValidationType())){
            return validateStringParameter(algoParam, userParamValue);
        }
        
        //for flag types 
        if (AlgorithmParameter.FLAG_TYPE.equalsIgnoreCase(algoParam.getType())){
            return validateFlagParameter(algoParam, userParamValue);
        }

        //for digits and number parameter
        if (AlgorithmParameter.DIGITS_VALIDATION.equalsIgnoreCase(algoParam.getValidationType()) ||
            AlgorithmParameter.NUMBER_VALIDATION.equalsIgnoreCase(algoParam.getValidationType())){
            return validateNumericParameter(algoParam, userParamValue);
        }
        
        ErrorResponse er = new ErrorResponse();
        er.setMessage("Unknown parameter type");
        er.setDescription(algoParam.getValidationType() + " is not a valid type");
        return er;
    }
    
    /**
     * Verify the value for a parameter of type {@value org.ndexbio.communitydetection.rest.model.CustomParameter#FLAG_TYPE}
     * has null or empty string for a value
     * @param algoParam 
     * @param userParamValue user's parameter value
     * @return {@code null} if its a valid parameter otherwise {@link org.ndexbio.communitydetection.rest.model.ErrorResponse}
     */
    private ErrorResponse validateFlagParameter(final CytoContainerParameter algoParam,
            final String userParamValue){
        
        
        if (userParamValue != null && userParamValue.trim().length() > 0){
                ErrorResponse er = new ErrorResponse();
                er.setMessage("Flag only given a value");
                er.setDescription(algoParam.getFlag() + " is a flag only parameter, "
                        + "but the following value was passed in " + userParamValue);
                return er;
        }
        return null;
    }
    
    private ErrorResponse validateStringParameter(final CytoContainerParameter algoParam,
            final String userParamValue){
        if (userParamValue == null || userParamValue.trim().length() == 0){
                ErrorResponse er = new ErrorResponse();
                er.setMessage("Parameter missing value");
                er.setDescription(algoParam.getFlag() + " is a value parameter, "
                        + "but no value was passed in");
                return er;
        }
        if (algoParam.getValidationRegex() == null){
            return null;
        }
        try {
            boolean res = Pattern.matches(algoParam.getValidationRegex(), userParamValue);
            if (res == true){
                return null;
            }
            ErrorResponse er = new ErrorResponse();
            setValidationHelpInErrorResponse(algoParam, er);
            er.setDescription(userParamValue + " did not match regular expression ");
            return er;
        } catch(PatternSyntaxException pse){
            
            ErrorResponse er = new ErrorResponse();
            er.setMessage("Malformed validation expression");
            er.setDescription(algoParam.getValidationRegex() + " is not a valid regular expression."
                    + "Cannot validate");
            _log.error(er.getDescription(), pse);
            return er;
        }
    }
    
    private ErrorResponse validateNumericParameter(final CytoContainerParameter algoParam,
            final String userParamValue){
        if (userParamValue == null || userParamValue.trim().length() == 0){
                ErrorResponse er = new ErrorResponse();
                er.setMessage("Parameter missing value");
                er.setDescription(algoParam.getFlag() + " is a value parameter, "
                        + "but no value was passed in");
                return er;
        }
        try {
            if (algoParam.getValidationType().equalsIgnoreCase(AlgorithmParameter.DIGITS_VALIDATION)){
                return checkIfParamIsDigitsAndWithinRange(algoParam, userParamValue);
            }
            return checkIfParamIsNumberAndWithinRange(algoParam, userParamValue); 
        } catch(NumberFormatException nfe){
            ErrorResponse er = new ErrorResponse();
            setValidationHelpInErrorResponse(algoParam, er);
            er.setDescription(userParamValue + " is not a valid number");
            _log.error(er.getDescription(), nfe);
            return er;
        }
    }
    
    private ErrorResponse checkIfParamIsDigitsAndWithinRange(final CytoContainerParameter algoParam,
            final String userParamValue){
        
        if (Pattern.matches("-?\\d+$", userParamValue) == false){
            ErrorResponse er = new ErrorResponse();
                setValidationHelpInErrorResponse(algoParam, er);
                er.setDescription(userParamValue + " does not appear to be a whole"
                        + " number");
                return er;
        }
        int val = Integer.parseInt(userParamValue);
        if (algoParam.getMinValue() != null){

            if (val < algoParam.getMinValue().intValue()){
                ErrorResponse er = new ErrorResponse();
                setValidationHelpInErrorResponse(algoParam, er);
                er.setDescription(userParamValue + " is less then minimum value: " + 
                algoParam.getMinValue().toString() + " allowed for this parameter");
                return er;
            }
        }
        if (algoParam.getMaxValue() != null){
            if (val > algoParam.getMaxValue().intValue()){
                ErrorResponse er = new ErrorResponse();
                setValidationHelpInErrorResponse(algoParam, er);
                er.setDescription(userParamValue + " is greater then maximum value: " + 
                algoParam.getMaxValue().toString() + " allowed for this parameter");
                return er;
            }
        }
        return null;
    }
    
    private ErrorResponse checkIfParamIsNumberAndWithinRange(final CytoContainerParameter algoParam,
            final String userParamValue){
        double val = Double.parseDouble(userParamValue);
        if (algoParam.getMinValue() != null){

            if (val < algoParam.getMinValue().doubleValue()){
                ErrorResponse er = new ErrorResponse();
                setValidationHelpInErrorResponse(algoParam, er);
                er.setDescription(userParamValue + " is less then minimum value: " + 
                algoParam.getMinValue().toString() + " allowed for this parameter");
                return er;
            }
        }
        if (algoParam.getMaxValue() != null){
            if (val > algoParam.getMaxValue().doubleValue()){
                ErrorResponse er = new ErrorResponse();
                setValidationHelpInErrorResponse(algoParam, er);
                er.setDescription(userParamValue + " is greater then maximum value: " + 
                algoParam.getMaxValue().toString() + " allowed for this parameter");
                return er;
            }
        }
        return null;
    }
    
    private void setValidationHelpInErrorResponse(final CytoContainerParameter algoParam,
            ErrorResponse er){
        if (algoParam.getValidationHelp() != null){
            er.setMessage(algoParam.getValidationHelp());
            return;
        }
        er.setMessage("Invalid parameter value");
    }
}
