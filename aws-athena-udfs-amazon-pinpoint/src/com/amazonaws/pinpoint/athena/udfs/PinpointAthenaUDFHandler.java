package com.amazonaws.pinpoint.athena.udfs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.lang.reflect.*;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.athena.connector.lambda.handlers.UserDefinedFunctionHandler;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.retry.PredefinedBackoffStrategies;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.pinpoint.AmazonPinpoint;
import com.amazonaws.services.pinpoint.AmazonPinpointClientBuilder;
import com.amazonaws.services.pinpoint.model.CampaignResponse;
import com.amazonaws.services.pinpoint.model.GetCampaignRequest;
import com.amazonaws.services.pinpoint.model.GetCampaignResult;
import com.amazonaws.services.pinpoint.model.GetEndpointRequest;
import com.amazonaws.services.pinpoint.model.GetEndpointResult;
import com.google.gson.Gson;

public class PinpointAthenaUDFHandler extends UserDefinedFunctionHandler {

    private static final String SOURCE_TYPE = "pinpoint_athena_udfs";
    private AmazonPinpoint pinpointClient;
    private String pinpointProjectId;

	
	public PinpointAthenaUDFHandler() {
		super(SOURCE_TYPE);
		this.pinpointProjectId = System.getenv("PINPOINT_PROJECT_ID");
	}
	
	private ClientConfiguration createClientConfiguration()
    {
        int retryBaseDelay = 1000;
        int retryMaxBackoffTime = 600000;
        int maxRetries = 100;
        RetryPolicy retryPolicy = new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
                                                  new PredefinedBackoffStrategies.ExponentialBackoffStrategy(retryBaseDelay, retryMaxBackoffTime),
                                                  maxRetries,
                                                  false);
        ClientConfiguration clientConfiguration = new ClientConfiguration()
                                                    .withRequestTimeout(5000)
                                                    .withRetryPolicy(retryPolicy);
        return clientConfiguration;
    }
	
	
	private AmazonPinpoint getPinpointClient() {
		// create client first time on demand
        String region = (System.getenv("AWS_REGION") != null) ? System.getenv("AWS_REGION") : "us-east-1";
        AWSCredentialsProvider awsCreds = DefaultAWSCredentialsProviderChain.getInstance();
        if (this.pinpointClient == null) {
            System.out.println("Creating Translate client connection with ExponentialBackoffStrategy");
            ClientConfiguration clientConfiguration = createClientConfiguration();
            this.pinpointClient = AmazonPinpointClientBuilder.standard()
                                             .withCredentials(awsCreds)
                                             .withRegion(region)
                                             .withClientConfiguration(clientConfiguration)
                                             .build();
        }
        return this.pinpointClient;
	}
	
	public String get_endpoint_json(String endpointId) {
		
		try {
			
			System.out.println("Got EndpointID: " + endpointId);
						
			GetEndpointRequest request = new GetEndpointRequest();
			request.setApplicationId(this.pinpointProjectId);
			request.setEndpointId(endpointId);
			
			GetEndpointResult response = getPinpointClient().getEndpoint(request);
			
			Gson gson = new Gson();			
			
			String endpoint = gson.toJson(response.getEndpointResponse());
			
			System.out.println(endpoint);
			
			return endpoint;
			
		} catch(Exception e) {
			System.out.println(e);
			return "{}";
		}
		
	}
	
	
	public Map<String, Map<String, List<String>>> get_endpoint(String endpointId) {
		
		try {
			
			System.out.println("Got EndpointID: " + endpointId);
			
			Map<String, Map<String, List<String>>> retVal = new HashMap<String, Map<String, List<String>>>();
			
			GetEndpointRequest request = new GetEndpointRequest();
			request.setApplicationId(this.pinpointProjectId);
			request.setEndpointId(endpointId);
			
			GetEndpointResult response = getPinpointClient().getEndpoint(request);
			
			
			retVal.put("EndpointAttributes", response.getEndpointResponse().getAttributes().entrySet()
					.stream()
					.collect(Collectors.toMap(
								Map.Entry::getKey,
								e -> e.getValue()
							)));
			retVal.put("UserAttributes", response.getEndpointResponse().getUser().getUserAttributes().entrySet()
					.stream()
					.collect(Collectors.toMap(
								Map.Entry::getKey,
								e -> e.getValue()
							)));
		
			
			// stream().map((a) -> a.toString()).toArray(String[]::new)
			System.out.println(retVal);
			
			return retVal;
		} catch(Exception e) {
			System.out.println(e);
			return new HashMap<String, Map<String, List<String>>>();
		}
		
	}
	
	
	public Map<String, List<String>> endpoint_attributes(String endpointId) {
		
		try {
					
			System.out.println("Got EndpointID: " + endpointId);
			
			GetEndpointRequest request = new GetEndpointRequest();
			request.setApplicationId(this.pinpointProjectId);
			request.setEndpointId(endpointId);
			
			GetEndpointResult response = getPinpointClient().getEndpoint(request);
						
			Map<String, List<String>> attributes = response.getEndpointResponse().getAttributes().entrySet()
					.stream()
					.collect(Collectors.toMap(
								Map.Entry::getKey,
								e -> e.getValue()
							));
			
			// stream().map((a) -> a.toString()).toArray(String[]::new)
			System.out.println(attributes);
			
			return attributes;
		} catch(Exception e) {
			System.out.println(e);
			return new HashMap<String, List<String>>();
		}
	}
	
	public String get_campaign(String campaignId) {
		try {
			
			Gson gson = new Gson();
			
			System.out.println("Got CampaignID: " + campaignId);
			
			GetCampaignRequest request = new GetCampaignRequest();
			request.setApplicationId(this.pinpointProjectId);
			request.setCampaignId(campaignId);
			
			GetCampaignResult response = getPinpointClient().getCampaign(request);
			
			String campaign = gson.toJson(response.getCampaignResponse());
			
			System.out.println(campaign);
			
			return campaign;
			
			
		} catch(Exception e) {
			return "{}";
		}
	}
	
	public Map<String, String> get_campaign_object(String campaignId) {
		
		Map<String, String> retVal = new HashMap<String, String>();

		try {
			
			System.out.println("Got CampaignID: " + campaignId);
			
			GetCampaignRequest request = new GetCampaignRequest();
			request.setApplicationId(this.pinpointProjectId);
			request.setCampaignId(campaignId);
			
			GetCampaignResult response = getPinpointClient().getCampaign(request);
			
			CampaignResponse campaign = response.getCampaignResponse();
			
			System.out.println(campaign);
			
			retVal.put("Name", campaign.getName());
			retVal.put("CreationDate", campaign.getCreationDate());
			retVal.put("Description", campaign.getDescription());
			retVal.put("Id", campaign.getId());
			retVal.put("SegmentId", campaign.getSegmentId());
			retVal.put("State", campaign.getState().getCampaignStatus());

			System.out.println(retVal);
			
			return retVal;
			
			
		} catch(Exception e) {
			System.out.println(e);
			return new HashMap<String, String>();
		}
	}
	
	public String get_campaign_property(String campaignId, String property) {
		try {
			
			Gson gson = new Gson();
			
			System.out.println("Got CampaignID: " + campaignId);
			
			GetCampaignRequest request = new GetCampaignRequest();
			request.setApplicationId(this.pinpointProjectId);
			request.setCampaignId(campaignId);
			
			GetCampaignResult response = getPinpointClient().getCampaign(request);
			
			Object campaignProperty = getProperty(response.getCampaignResponse(), property);
			
			String campaignPropertyString = null;
			
			if (campaignProperty.getClass().equals(String.class)) {
				campaignPropertyString = (String) campaignProperty;
			} else {
				campaignPropertyString = gson.toJson(campaignProperty);
			}
			
			
			System.out.println(campaignPropertyString);
			
			return campaignPropertyString;
			
			
		} catch(Exception e) {
			System.out.println(e);
			return "{}";
		}
	}
	
	/**
     * Fetch a property from an object. For example of you wanted to get the foo
     * property on a bar object you would normally call {@code bar.getFoo()}. This
     * method lets you call it like {@code BeanUtil.getProperty(bar, "foo")}
     * @param obj The object who's property you want to fetch
     * @param property The property name
     * @return The value of the property or null if it does not exist.
     */
    public static Object getProperty(Object obj, String property) {
        Object returnValue = null;

        try {
            String methodName = "get" + property.substring(0, 1).toUpperCase() + property.substring(1, property.length());
            Class clazz = obj.getClass();
            Method method = clazz.getMethod(methodName, null);
            returnValue = method.invoke(obj, null);
        }
        catch (Exception e) {
            // Do nothing, we'll return the default value
        }

        return returnValue;
    }
	

}
