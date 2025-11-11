package io.github.nahomgh.portfolio.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nahomgh.portfolio.dto.AssetDTO;
import io.github.nahomgh.portfolio.exceptions.PriceUnavailableException;
import io.github.nahomgh.portfolio.exceptions.UnsupportedAssetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PriceDataService {

    private final RestTemplate restTemplate;
    // Plan to put this into 'applications.properties' file and use the @Values($) to retrieve the api uri or even in the application-local_original.yml file I want to create

    @Value("${API_TOP_ASSETS_ENDPOINT:}")
    private String TOP_ASSETS_URI;

    @Value("${API_HISTORICAL_PRICE_ENDPOINT}")
    private String HISTORICAL_PRICE_URI;

    @Value("${API_SPECIFIC_ASSET_ENDPOINT}")
    private String SPECIFIC_ASSET_URI;

    @Value("${API_KEY_COINGECKO}")
    private String API_KEY_COINGECKO;

    private ObjectMapper objectMapper;

    private RedisCacheManager cacheManager;

    private static final Logger logger = LoggerFactory.getLogger(PriceDataService.class);
    private final Map<String, String> symbolToId = new ConcurrentHashMap<>();

    public PriceDataService(RestTemplate restTemplate, ObjectMapper objectMapper, RedisCacheManager cacheManager){
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;

    }
    public List<AssetDTO> getTopAssets(){
        URI uri = UriComponentsBuilder
                .fromUriString(TOP_ASSETS_URI)
                .build().toUri();
        List<AssetDTO> listOfTopAssets = fetchMarketData(uri, new ParameterizedTypeReference<List<AssetDTO>>(){});
        updatePriceCache(listOfTopAssets);
        return listOfTopAssets;
    }

    private void updatePriceCache(List<AssetDTO> listOfTopAssets) {
        Cache priceCache = cacheManager.getCache("prices");

        for(AssetDTO asset : listOfTopAssets) {
            priceCache.put(asset.assetName(),asset.price());
            symbolToId.put(asset.symbol().toUpperCase(), asset.assetName());
        }
    }

    @Cacheable(value="prices", key="#assetName")
    public BigDecimal getAssetPrice(String assetName){
        URI uri = UriComponentsBuilder
                .fromUriString(SPECIFIC_ASSET_URI)
                .queryParam("ids", assetName) //totalCost?ids=bitcoin&vs_currencies=usd
                .queryParam("vs_currencies","usd")
                .build().toUri();

        Map<String, Map<String, BigDecimal>> response =
                fetchMarketData(uri,
                        new ParameterizedTypeReference<Map<String, Map<String, BigDecimal>>>(){});
        if(response.isEmpty())
            throw new UnsupportedAssetException("Unsupported Asset - Please Check Spelling Or Provide Another Asset");
        logger.warn("Asset information response: "+response.get(assetName));
        return response.get(assetName).get("usd");
    }

    public BigDecimal getHistoricalAssetPrice(String assetNamePassed, LocalDate  transactionDate){
        String assetName = resolveAssetSymbol(assetNamePassed);
            URI uri = UriComponentsBuilder
                    .fromUriString(HISTORICAL_PRICE_URI)
                    .path(assetName)
                    .path("/history")
                    .queryParam("date", transactionDate)
                    .build().toUri();
            logger.info("URI built: to query price for " + assetName + " at " + transactionDate.toString());
            return fetchHistoricalMarketData(uri);

    }

    public BigDecimal fetchHistoricalMarketData(URI uri){
        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-cg-demo-api-key", API_KEY_COINGECKO);

        HttpEntity<String> httpEntity = new HttpEntity<>(headers); // Adds the headers above into the httpEntity
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    httpEntity,
                    String.class
            );
            logger.info("Asset List Prices Updated");
            JsonNode jsonDataRoot = objectMapper.readTree(response.getBody());
            BigDecimal assetPrice = jsonDataRoot
                    .path("market_data")
                    .path("current_price")
                    .path("usd")
                    .decimalValue();
            logger.info("SUCCESS: Price returned! "+assetPrice);
            return assetPrice;

        }catch(JsonProcessingException e) {
            logger.error("JSON Failed parsing: " + e.getMessage());
            throw new PriceUnavailableException("Failed to parse API Response");
        }catch(ResourceAccessException e){
            throw new PriceUnavailableException("Unable to retrieve prices from API");
        }catch(Exception e){
            logger.error(e.getMessage()+"\n"+"-".repeat(10)+"\n");
            throw new PriceUnavailableException("Unexpected Error occurred: "+e.getMessage());
        }
    }
//
    public <T> T fetchMarketData(URI uri, ParameterizedTypeReference<T> responseType){
        logger.info("TRIGGERED API CALL\nURI: "+uri.toString());
         // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        HttpEntity<String> httpEntity = new HttpEntity<>(headers); // Adds the headers above into the httpEntity
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    httpEntity,
                    responseType
            );
            logger.info("Asset List Prices Updated");

            return response.getBody();
        }catch(ResourceAccessException e){
            throw new PriceUnavailableException("Unable to retrieve prices from API");
        }catch(Exception e){
            logger.error(e.getMessage()+"\n"+"-".repeat(10)+"\n");
            throw new PriceUnavailableException("Unexpected Error occurred: "+e.getMessage());
        }
    }
    @Scheduled(initialDelay = 0, fixedRate = 60000)
    public void refreshPriceData(){
        logger.info("CACHE REFRESHING");
        getTopAssets();
    }

    public String resolveAssetSymbol(String assetName){
        String normalisedName = assetName.toUpperCase().trim();
        if(symbolToId.containsKey(normalisedName))
            return symbolToId.get(normalisedName);
        return assetName.toLowerCase().trim();
    }

}
