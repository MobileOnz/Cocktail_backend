/**
 * ===================================================
 * K6 Spike Test - ONZ Cocktail API
 * ===================================================
 *
 * Purpose: Test sudden traffic spikes (e.g., viral event, marketing campaign)
 * Duration: 5 minutes
 * VUs: Sudden spike to 100 users
 *
 * Usage:
 *   k6 run spike.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const requestDuration = new Trend('request_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Normal traffic
    { duration: '30s', target: 100 },  // SPIKE!
    { duration: '2m', target: 100 },   // Stay at spike level
    { duration: '30s', target: 10 },   // Return to normal
    { duration: '1m', target: 0 },     // Cool down
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],  // More lenient during spike
    http_req_failed: ['rate<0.10'],     // Allow 10% error rate during spike
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/onz';

export default function () {
  const endpoints = [
    `${BASE_URL}/actuator/health`,
    `${BASE_URL}/api/v2/cocktails/names`,
    `${BASE_URL}/api/v2/cocktails?page=0&size=20`,
  ];

  // Random endpoint selection (realistic user behavior)
  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const res = http.get(endpoint);

  const success = check(res, {
    'status is 200 or 429': (r) => r.status === 200 || r.status === 429, // 429 = rate limited
    'response time < 3000ms': (r) => r.timings.duration < 3000,
  });

  errorRate.add(!success);
  requestDuration.add(res.timings.duration);

  sleep(0.5); // Shorter sleep during spike test
}

export function setup() {
  console.log('Starting SPIKE test...');
  console.log('WARNING: This will generate sudden high load');
  console.log(`Target: ${BASE_URL}`);
}
