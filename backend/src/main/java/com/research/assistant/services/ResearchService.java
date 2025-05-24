package com.research.assistant.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.assistant.entity.GeminiResponse;
import com.research.assistant.entity.ResearchRequest;

@Service
public class ResearchService {
	
    @Value("${gemini.api.url}")
    private String geminiApiUrl;
	
    @Value("${gemini.api.key}")
    private String geminiApiKey;
	
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private final WebClient webClient;
	
    public ResearchService(WebClient.Builder webClient) {
        this.webClient = webClient.build();
    }
	
    public String processContent(ResearchRequest request) {
		
        String prompt = buildPrompt(request);
		 
        
        Map<String, Object> requestBody = Map.of(
        	    "contents", List.of(
        	        Map.of("parts", List.of(
        	            Map.of("text", prompt)
        	        ))
        	    )
        	);

       
        String rawResponse = webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        
        return extractResponse(rawResponse);

    }
	
    private String extractResponse(String response) {
        try {
            GeminiResponse geminiResponse = OBJECT_MAPPER.readValue(response, GeminiResponse.class);
			
            if (geminiResponse.getCandidates() != null && !geminiResponse.getCandidates().isEmpty()) { // checking if candidates are present or not
                GeminiResponse.Candidate firstCandidate = geminiResponse.getCandidates().get(0);
				
                if (firstCandidate.getContent() != null && 
                    firstCandidate.getContent().getParts() != null && 
                    !firstCandidate.getContent().getParts().isEmpty()) {
					
                    return firstCandidate.getContent().getParts().get(0).getText();
                }
            }
            
            return "No content found in response.";
            
        }
        catch (Exception e) {
            return "Error processing response: " + e.getMessage();
        }
    }

    private String buildPrompt(ResearchRequest request) {
        StringBuilder prompt = new StringBuilder();
		
        switch (request.getOperation().toLowerCase()) {
            case "summarize":
                prompt.append("Summarize the following content: ");
                break;
            case "suggest":
                prompt.append("Suggest improvements for the following content: ");
                break;
            case "analyze":
                prompt.append("Analyze the following content: ");
                break;
            case "explain":
                prompt.append("explain the following content: ");
                break;
            case "extract":
                prompt.append("Extract key points from the following content: ");
                break;
            default:
                throw new IllegalArgumentException("Invalid operation: " + request.getOperation());
        }
		
        prompt.append(request.getContent());
		
        return prompt.toString();
    }
}