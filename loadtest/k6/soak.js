/**
 * ===================================================
 * K6 Soak Test - ONZ Cocktail API
 * ===================================================
 *
 * Purpose: Test system stability over extended period (find memory leaks, resource exhaustion)
 * Duration: 1 hour
 * VUs: Constant moderate load
 *
 * Usage:
 *   k6 run soak.js
 *   k6 run --duration 2h soak.js  # Override to 2 hours
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const memoryTrend = new Trend('memory_usage');

export const options = {
  stages: [
    { duration: '5m', target: 20 },   // Ramp up
    { duration: '50m', target: 20 },  // Stay constant (soak)
    { duration: '5m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'],
    http_req_failed: ['rate<0.05'],
    errors: ['rate<0.05'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/onz';

export default function () {
  // Simulate realistic user journey
  userJourney();
  sleep(2); // Realistic think time
}

/**
 * Realistic user journey through the API
 */
function userJourney() {
  // Step 1: Health check
  http.get(`${BASE_URL}/actuator/health`);

  sleep(1);

  // Step 2: Browse cocktails
  const listRes = http.get(`${BASE_URL}/api/v2/cocktails/names`);
  check(listRes, {
    'list cocktails successful': (r) => r.status === 200,
  });

  sleep(2);

  // Step 3: Search with filters
  const searchRes = http.get(`${BASE_URL}/api/v2/cocktails?page=0&size=20`);
  check(searchRes, {
    'search successful': (r) => r.status === 200,
  });

  sleep(1);

  // Step 4: Check metrics endpoint (simulate monitoring)
  const metricsRes = http.get(`${BASE_URL}/actuator/prometheus`);
  check(metricsRes, {
    'metrics available': (r) => r.status === 200,
  });

  // Parse memory metrics if available
  if (metricsRes.status === 200) {
    const body = metricsRes.body;
    const match = body.match(/jvm_memory_used_bytes{.*area="heap".*} ([\d.]+)/);
    if (match) {
      memoryTrend.add(parseFloat(match[1]));
    }
  }
}

export function setup() {
  console.log('Starting SOAK test...');
  console.log('Duration: 1 hour');
  console.log('This will test for memory leaks and resource exhaustion');
  console.log(`Target: ${BASE_URL}\n`);

  // Initial metrics check
  const res = http.get(`${BASE_URL}/actuator/health`);
  if (res.status !== 200) {
    throw new Error('API not accessible');
  }

  return { startTime: new Date() };
}

export function teardown(data) {
  const endTime = new Date();
  const durationMinutes = (endTime - data.startTime) / 1000 / 60;

  console.log(`\n${'='.repeat(50)}`);
  console.log('SOAK TEST COMPLETED');
  console.log(`${'='.repeat(50)}`);
  console.log(`Duration: ${durationMinutes.toFixed(2)} minutes`);
  console.log('\nNext steps:');
  console.log('1. Check Grafana for memory trends');
  console.log('2. Look for memory leaks (increasing heap usage)');
  console.log('3. Check for connection pool exhaustion');
  console.log('4. Review logs for any errors or warnings');
  console.log(`${'='.repeat(50)}\n`);
}
