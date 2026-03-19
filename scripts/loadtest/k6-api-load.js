import http from 'k6/http';
import { check, sleep, group } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost/api';
const TOKEN = __ENV.TOKEN || '';

const VEHICLE_IDS = parseList(__ENV.VEHICLE_IDS);
const PLATES = parseList(__ENV.PLATES);
const COMPANY_IDS = parseList(__ENV.COMPANY_IDS);
const MODEL_IDS = parseList(__ENV.MODEL_IDS);
const BRAND_IDS = parseList(__ENV.BRAND_IDS);
const REGION_IDS = parseList(__ENV.REGION_IDS);

const AUTH_HEADERS = TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {};

export const options = {
  scenarios: {
    mixed: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.RAMP_UP || '30s', target: Number(__ENV.VUS_MAX || 100) },
        { duration: __ENV.STEADY || '2m', target: Number(__ENV.VUS_MAX || 100) },
        { duration: __ENV.RAMP_DOWN || '30s', target: 0 },
      ],
      gracefulStop: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1200'],
  },
};


function logIfError(res, name) {
  if (!res || res.status === 200) return;
  const body = res.body ? String(res.body).slice(0, 200) : '';
  console.error(`[${name}] status=${res.status} url=${res.url} body=${body}`);
}

export default function () {
  const r = Math.random();

  if (r < 0.45) {
    group('vehicles:list', () => {
      listVehicles();
    });
  } else if (r < 0.70) {
    group('comments:list', () => {
      commentsList();
    });
  } else if (r < 0.80) {
    group('favorites:summary', () => {
      favoriteSummary();
    });
  } else if (r < 0.88) {
    group('vehicles:detail', () => {
      vehicleDetail();
    });
  } else if (r < 0.92) {
    group('vehicles:plate', () => {
      vehicleByPlate();
    });
  } else if (r < 0.96) {
    group('snapshots:hot', () => {
      snapshotsHot();
    });
  } else if (r < 0.99) {
    group('snapshots:plate', () => {
      snapshotByPlate();
    });
  } else {
    group('catalogs', () => {
      catalogs();
    });
  }

  sleep(0.2);
}

function listVehicles() {
  const params = [];
  params.push('size=12');

  const regionId = pick(REGION_IDS);
  const companyId = pick(COMPANY_IDS);
  const brandId = pick(BRAND_IDS);
  const modelId = pick(MODEL_IDS);

  if (regionId) params.push(`regionId=${encodeURIComponent(regionId)}`);
  if (companyId) params.push(`companyId=${encodeURIComponent(companyId)}`);
  if (brandId) params.push(`brandId=${encodeURIComponent(brandId)}`);
  if (modelId) params.push(`modelId=${encodeURIComponent(modelId)}`);

  const query = params.join('&');
  const res = http.get(`${BASE_URL}/vehicles?${query}`);
  check(res, { 'vehicles:list 200': (r) => r.status === 200 });
  logIfError(res, 'vehicles:list');
}

function vehicleDetail() {
  const id = pick(VEHICLE_IDS);
  if (!id) return;
  const res = http.get(`${BASE_URL}/vehicles/${id}`);
  check(res, { 'vehicles:detail 200': (r) => r.status === 200 });
  logIfError(res, 'vehicles:detail');
}

function vehicleByPlate() {
  const plate = pick(PLATES);
  if (!plate) return;
  const res = http.get(`${BASE_URL}/vehicles/plate/${encodeURIComponent(plate)}`);
  check(res, { 'vehicles:plate 200': (r) => r.status === 200 });
  logIfError(res, 'vehicles:plate');
}

function commentsList() {
  const id = pick(VEHICLE_IDS);
  if (!id) return;
  const res = http.get(`${BASE_URL}/vehicles/${id}/comments?page=1&size=10`);
  check(res, { 'comments:list 200': (r) => r.status === 200 });
  logIfError(res, 'comments:list');
}

function favoriteSummary() {
  const id = pick(VEHICLE_IDS);
  if (!id) return;
  const res = http.get(`${BASE_URL}/favorites/${id}/summary`);
  check(res, { 'favorites:summary 200': (r) => r.status === 200 });
  logIfError(res, 'favorites:summary');
}

function favoriteToggle() {
  const id = pick(VEHICLE_IDS);
  if (!id || !TOKEN) return;
  const res = http.post(`${BASE_URL}/favorites/${id}/toggle`, null, { headers: AUTH_HEADERS });
  check(res, { 'favorites:toggle 200': (r) => r.status === 200 });
  logIfError(res, 'favorites:toggle');
}

function snapshotsHot() {
  const res = http.get(`${BASE_URL}/snapshots/hot?size=6`);
  check(res, { 'snapshots:hot 200': (r) => r.status === 200 });
  logIfError(res, 'snapshots:hot');
}

function snapshotByPlate() {
  const plate = pick(PLATES);
  if (!plate) return;
  const res = http.get(`${BASE_URL}/snapshots/plate/${encodeURIComponent(plate)}`);
  check(res, { 'snapshots:plate 200': (r) => r.status === 200 });
  logIfError(res, 'snapshots:plate');
}

function catalogs() {
  http.get(`${BASE_URL}/catalog/regions`);
  http.get(`${BASE_URL}/catalog/companies`);
  http.get(`${BASE_URL}/catalog/brands`);
  http.get(`${BASE_URL}/catalog/models`);
}

function parseList(value) {
  if (!value) return [];
  return value
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function pick(arr) {
  if (!arr || arr.length === 0) return null;
  const index = Math.floor(Math.random() * arr.length);
  return arr[index];
}
