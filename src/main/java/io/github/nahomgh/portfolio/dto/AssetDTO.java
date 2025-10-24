package io.github.nahomgh.portfolio.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record AssetDTO (@JsonProperty("id") String assetName, @JsonProperty("symbol") String symbol, @JsonProperty("current_price") BigDecimal price){
}
