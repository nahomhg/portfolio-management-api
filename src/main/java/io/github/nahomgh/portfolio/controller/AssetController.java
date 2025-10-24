package io.github.nahomgh.portfolio.controller;

import io.github.nahomgh.portfolio.service.PriceDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/v1/prices")
public class AssetController {

    private final PriceDataService priceDataService;

    public AssetController(PriceDataService priceDataService){
        this.priceDataService = priceDataService;
    }

    @GetMapping("{assetName}")
    public ResponseEntity<Map<String, Object>> getAssetPrices(@PathVariable String assetName){
        return ResponseEntity.ok(Map.of(
                "asset",assetName,
                "totalCost",priceDataService.getAssetPrice(assetName),
                "currency","USD"));
    }
}
