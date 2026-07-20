package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.dto.response.StatsResponse;
import com.busgallery.busgallery.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping
    public StatsResponse getStats() {
        return statsService.getStats();
    }
}
