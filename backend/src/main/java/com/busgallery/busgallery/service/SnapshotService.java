package com.busgallery.busgallery.service;

import com.busgallery.busgallery.service.snapshot.SnapshotPayload;
import java.util.List;

/**
 * SnapshotService 接口。
 */
public interface SnapshotService {
    SnapshotPayload getSnapshotByPlate(String plateNumber);
    List<SnapshotPayload> listHotSnapshots(int limit);
}