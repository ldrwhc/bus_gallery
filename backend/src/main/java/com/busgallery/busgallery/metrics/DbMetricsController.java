package com.busgallery.busgallery.metrics;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class DbMetricsController {

    private final DbQueryMetrics dbQueryMetrics;

    @GetMapping("/db")
    public Map<String, Object> db() {
        return dbQueryMetrics.snapshot();
    }
}
