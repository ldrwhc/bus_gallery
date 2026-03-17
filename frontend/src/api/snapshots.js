import request from './axiosInstance';

// axios baseURL already /api
export const fetchHotSnapshots = (size = 6) => request.get('/snapshots/hot', { params: { size } });
export const fetchSnapshotByPlate = (plate) => request.get(`/snapshots/plate/${encodeURIComponent(plate)}`);
