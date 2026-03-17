# Load Testing Guide - ONZ Cocktail API

This directory contains K6 load testing scripts for the ONZ Cocktail API.

## Prerequisites

Install K6:
```bash
# macOS
brew install k6

# Windows
choco install k6

# Linux
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

## Test Scenarios

### 1. Baseline Test (`baseline.js`)
**Purpose**: Test normal traffic patterns
**Duration**: 5 minutes
**VUs**: 5 → 10 → 0

```bash
k6 run k6/baseline.js
```

**Use when**: Regular performance testing, before deployments

---

### 2. Spike Test (`spike.js`)
**Purpose**: Test sudden traffic spikes (viral event, marketing)
**Duration**: 5 minutes
**VUs**: 10 → 100 (spike) → 10 → 0

```bash
k6 run k6/spike.js
```

**Use when**: Before major launches, marketing campaigns

---

### 3. Stress Test (`stress.js`)
**Purpose**: Find the breaking point
**Duration**: 10 minutes
**VUs**: 20 → 50 → 100 → 150 → 200

```bash
k6 run k6/stress.js
```

**Use when**: Capacity planning, infrastructure sizing

---

### 4. Soak Test (`soak.js`)
**Purpose**: Find memory leaks and resource exhaustion
**Duration**: 1 hour (default)
**VUs**: Constant 20

```bash
k6 run k6/soak.js

# Extended soak test (2 hours)
k6 run --duration 2h k6/soak.js
```

**Use when**: Before major releases, quarterly testing

---

## Running Tests

### Basic Usage
```bash
# Run a test
k6 run k6/baseline.js

# Override base URL
k6 run -e BASE_URL=http://your-server:8080/onz k6/baseline.js

# Save results to JSON
k6 run --out json=results.json k6/baseline.js

# Run with custom VUs and duration
k6 run --vus 50 --duration 30s k6/baseline.js
```

### Monitoring During Tests

1. **Open Grafana**: http://localhost:3000
2. **Navigate to**: ONZ Cocktail API - Overview dashboard
3. **Watch metrics**:
   - HTTP Request Rate
   - Response Time (p95)
   - CPU Usage
   - Memory Usage
   - Error Rate

### Interpreting Results

#### Good Performance
```
✓ http_req_duration..........: avg=150ms  p(95)=300ms
✓ http_req_failed............: 0.50%
✓ http_reqs..................: 12000 (200/s)
```

#### Warning Signs
```
✗ http_req_duration..........: avg=800ms  p(95)=1500ms  ← Too slow
✗ http_req_failed............: 8.50%                    ← High error rate
```

#### Breaking Point Found
```
✗ http_req_duration..........: avg=5000ms p(95)=10000ms ← System overloaded
✗ http_req_failed............: 45.00%                   ← System failing
```

---

## Performance Targets

| Metric | Target | Maximum |
|--------|--------|---------|
| Response Time (p95) | < 500ms | < 1000ms |
| Error Rate | < 1% | < 5% |
| Throughput | > 100 req/s | N/A |
| CPU Usage | < 70% | < 90% |
| Memory Usage | < 70% | < 85% |

---

## Troubleshooting

### Test fails immediately
```
Error: API is not accessible
```
**Solution**: Make sure the API is running (`./gradlew bootRun`)

### High error rates
**Possible causes**:
- Database connection pool exhausted
- Memory limit reached
- Rate limiting triggered

**Check**:
1. Grafana → Memory Usage
2. Logs → `./logs/Onz-json.log`
3. Prometheus → `hikaricp_connections_active`

### Slow response times
**Check**:
1. Database query performance (enable `spring.jpa.show-sql=true`)
2. External API calls
3. N+1 query problems

---

## Best Practices

1. **Always run baseline first** to establish performance baseline
2. **Monitor during tests** using Grafana dashboards
3. **Run tests in isolation** (close other applications)
4. **Save results** for comparison over time
5. **Run soak tests** before major releases

## Next Steps

After load testing:
1. Review results in Grafana
2. Check logs for errors
3. Optimize slow endpoints
4. Adjust infrastructure if needed
5. Document performance improvements
