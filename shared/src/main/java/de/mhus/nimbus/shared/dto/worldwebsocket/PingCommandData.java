package de.mhus.nimbus.shared.dto.worldwebsocket;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PingCommandData {
    @JsonProperty("timestamp")
    private Long timestamp;
}
