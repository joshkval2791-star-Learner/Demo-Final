package com.example.frontend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        logger.info("==================== USER INFO REQUEST ====================");
        logger.info("Client Registration ID: {}", userRequest.getClientRegistration().getRegistrationId());
        logger.info("Access Token Value (first 50 chars): {}", 
            userRequest.getAccessToken().getTokenValue().substring(0, Math.min(50, userRequest.getAccessToken().getTokenValue().length())));
        logger.info("Access Token Type: {}", userRequest.getAccessToken().getTokenType().getValue());
        logger.info("Access Token Scopes: {}", userRequest.getAccessToken().getScopes());
        logger.info("User Info URI: {}", userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
        logger.info("User Info Authentication Method: {}", 
            userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getAuthenticationMethod());
        
        try {
            OAuth2User user = super.loadUser(userRequest);
            logger.info("Successfully loaded user: {}", user.getName());
            logger.info("User attributes: {}", user.getAttributes());
            return user;
        } catch (Exception e) {
            logger.error("Failed to load user info", e);
            throw e;
        }
    }
}
