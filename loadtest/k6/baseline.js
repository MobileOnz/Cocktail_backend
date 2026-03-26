/**
 * ===================================================
 * K6 Baseline Load Test - ONZ Cocktail API
 * ===================================================
 *
 * Purpose: Test normal traffic patterns
 * Duration: 5 minutes
 * VUs: Ramp up to 10 virtual users
 *
 * Usage:
 *   k6 run baseline.js
 *   k6 run --out json=results.json baseline.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const successRate = new Rate('success');
const requestDuration = new Trend('request_duration');
const requestCount = new Counter('request_count');

// Test configuration
export const options = {
  stages: [
    { duration: '1m', target: 5 },   // Ramp up to 5 VUs
    { duration: '3m', target: 10 },  // Stay at 10 VUs
    { duration: '1m', target: 0 },   // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.05'],    // Error rate should be less than 5%
    errors: ['rate<0.05'],
  },
};

// Base URL - Change this based on environment
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/onz';

/**
 * Main test scenario
 */
export default function () {
  // Test 1: Health check
  testHealthCheck();

  // Test 2: Get cocktail names (public endpoint)
  testGetCocktailNames();

  // Test 3: Search cocktails
  testSearchCocktails();

  // Sleep between iterations (realistic user behavior)
  sleep(1);
}

/**
 * Test: Health check endpoint
 */
function testHealthCheck() {
  const res = http.get(`${BASE_URL}/actuator/health`);

  const success = check(res, {
    'health check status is 200': (r) => r.status === 200,
    'health check response has status': (r) => r.json('status') !== undefined,
  });

  errorRate.add(!success);
  successRate.add(success);
  requestDuration.add(res.timings.duration);
  requestCount.add(1);
}

/**
 * Test: Get cocktail names
 */
function testGetCocktailNames() {
  const res = http.get(`${BASE_URL}/api/v2/cocktails/names`);

  const success = check(res, {
    'cocktail names status is 200': (r) => r.status === 200,
    'cocktail names has data': (r) => {
      const body = r.json();
      return body && body.data && Array.isArray(body.data);
    },
  });

  errorRate.add(!success);
  successRate.add(success);
  requestDuration.add(res.timings.duration);
  requestCount.add(1);
}

/**
 * Test: Search cocktails with filters
 */
function testSearchCocktails() {
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const res = http.get(`${BASE_URL}/api/v2/cocktails?page=0&size=20`, params);

  const success = check(res, {
    'search status is 200': (r) => r.status === 200,
    'search has cocktails': (r) => {
      const body = r.json();
      return body && body.data;
    },
    'response time < 1000ms': (r) => r.timings.duration < 1000,
  });

  errorRate.add(!success);
  successRate.add(success);
  requestDuration.add(res.timings.duration);
  requestCount.add(1);
}

/**
 * Setup function - runs once before test
 */
export function setup() {
  console.log('Starting baseline load test...');
  console.log(`Target: ${BASE_URL}`);

  // Verify API is accessible
  const res = http.get(`${BASE_URL}/actuator/health`);
  if (res.status !== 200) {
    throw new Error('API is not accessible. Please check if the service is running.');
  }

  return { startTime: new Date() };
}

/**
 * Teardown function - runs once after test
 */
export function teardown(data) {
  const endTime = new Date();
  const duration = (endTime - data.startTime) / 1000;
  console.log(`\nTest completed in ${duration.toFixed(2)} seconds`);
}
