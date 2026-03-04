/**
 * ===================================================
 * K6 Stress Test - ONZ Cocktail API
 * ===================================================
 *
 * Purpose: Find the breaking point of the system
 * Duration: 10 minutes
 * VUs: Gradually increase until system breaks
 *
 * Usage:
 *   k6 run stress.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Counter } from 'k6/metrics';

const errorRate = new Rate('errors');
const failures = new Counter('failures');

export const options = {
  stages: [
    { duration: '2m', target: 20 },   // Warm up
    { duration: '2m', target: 50 },   // Increase load
    { duration: '2m', target: 100 },  // Push harder
    { duration: '2m', target: 150 },  // Keep pushing
    { duration: '2m', target: 200 },  // Find the limit
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],  // Very lenient
    errors: ['rate<0.50'],              // Allow up to 50% errors (we're finding limits)
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/onz';

export default function () {
  const res = http.get(`${BASE_URL}/api/v2/cocktails?page=0&size=20`);

  const success = check(res, {
    'status is 2xx or 4xx': (r) => r.status >= 200 && r.status < 500,
  });

  if (!success) {
    failures.add(1);
  }

  errorRate.add(!success);
  sleep(1);
}

export function setup() {
  console.log('Starting STRESS test...');
  console.log('WARNING: This will push the system to its limits');
  console.log('Monitor CPU, Memory, and Database connections!');
  console.log(`Target: ${BASE_URL}`);
}

export function teardown() {
  console.log('\nStress test completed.');
  console.log('Check Grafana for system resource usage during the test.');
}
