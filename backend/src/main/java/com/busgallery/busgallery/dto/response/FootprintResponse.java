package com.busgallery.busgallery.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FootprintResponse {
    private List<FootprintCityResponse> cities;
    private int reachedCities;
    private int totalCities;
    private int rank;
    private int totalUsers;
}
