package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.service.SnapshotService;
import com.busgallery.busgallery.service.snapshot.SnapshotPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SnapshotController 类。
 */
@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
public class SnapshotController {

    private final SnapshotService snapshotService;

    /**
     * byPlate 鏂规硶銆?
     * @param plateNumber 鍙傛暟
     * @return 杩斿洖鍊?
     */
    @GetMapping("/plate/{plateNumber}")
    public SnapshotPayload byPlate(@PathVariable String plateNumber) {
        return snapshotService.getSnapshotByPlate(plateNumber);
    }

    /**
     * hot 鏂规硶銆?
     * @param size 鍙傛暟
     * @return 杩斿洖鍊?
     */
    @GetMapping("/hot")
    public List<SnapshotPayload> hot(@RequestParam(defaultValue = "6") int size) {
        return snapshotService.listHotSnapshots(size);
    }
}
