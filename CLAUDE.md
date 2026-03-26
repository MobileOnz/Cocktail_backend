# CLAUDE.md

ONZ Cocktail Backend - Spring Boot REST API for cocktail discovery with JWT authentication and social login.

## Tech Stack
- **Framework**: Spring Boot 3.4.1, Java 17, Gradle
- **Database**: PostgreSQL, QueryDSL 5 (Jakarta)
- **Auth**: JWT + OAuth2 (Kakao, Naver, Google, Apple)
- **Storage**: AWS S3
- **Context Path**: `/onz`

## Quick Commands

```bash
# Run application
./gradlew bootRun

# Regenerate QueryDSL Q-classes (after entity changes)
./gradlew clean compileJava

# Build (tests disabled)
./gradlew clean build
```

## Key Architecture Points

**Package Structure:**
```
common/auth/jwt/          # JWTFilter, JWTUtil, token stores
common/exception/         # Global exception handler
common/response/          # ResponseDto standard format
domain/cocktail/          # Cocktail controllers, services, repos, entities, DTOs
domain/member/            # Member management
```

**Response Format (All APIs):**
```json
{
  "code": 1,           // 1=success, -1=failure, -2=need refresh
  "msg": "...",
  "data": {...}
}
```

**Authentication:**
- JWT-based with **whitelist approach**: Endpoints are PUBLIC by default
- Protected endpoints must be registered in `JWTFilter.REQUIRE_AUTH_PATH_PATTERNS`
- Access tokens blacklisted on logout, refresh tokens cached (Caffeine)

**QueryDSL:**
- Q-classes in `build/generated/querydsl/`
- Custom repos use `JPAQueryFactory` for dynamic queries

## Critical Configuration

**JSON Naming Strategy:**
- **⚠️ IMPORTANT**: `spring.jackson.property-naming-strategy=SNAKE_CASE`
- All DTOs must use `@JsonProperty("snake_case")` for field mapping
- Request/Response JSON uses snake_case (e.g., `cocktail_ids`, not `cocktailIds`)

**API Paths:**
- V2 APIs: `/api/v2/**` and `/onz/api/v2/**` (with context-path)
- Admin: `/admin/**` (form-based login, credentials: `admin` / `admin1!`)
- Swagger: `/swagger-ui.html`

**Environment Variables (application.properties):**
- JWT: `JWT_SECRET`
- DB: `POSTGRE_url`, `POSTGRE_USERNAME`, `POSTGRE_PASSWORD`
- S3: `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY`, `S3_BUCKET`
- OAuth2: Client IDs/secrets for Kakao, Naver, Google, Apple

## API Development Checklist

**When creating or modifying an API endpoint, follow these steps:**

### 1. Create/Update DTOs
```java
// ⚠️ CRITICAL: Use @JsonProperty for snake_case
@JsonProperty("cocktail_ids")  // Request JSON: "cocktail_ids"
private List<Long> cocktailIds; // Java field: camelCase
```

### 2. Implement Service Logic
- Add business logic to appropriate service class
- Use `@Transactional` for write operations

### 3. Create Controller Endpoint
- Add `@RestController` endpoint with Swagger docs
- Use `ResponseDto<T>` for all responses

### 4. ⚠️ Register Protected Endpoint in JWTFilter
**If the API requires authentication:**

Open `JWTFilter.java` and add to `REQUIRE_AUTH_PATH_PATTERNS`:
```java
"^/api/v2/your/endpoint$",      // Without /onz
"^/onz/api/v2/your/endpoint$"   // With /onz context-path
```

**⚠️ WARNING**: By default, endpoints are **PUBLIC**. If you forget this step, your protected API will be accessible without authentication!

### 5. Test
- Use Swagger UI at `/swagger-ui.html` or Postman
- Verify authentication works (if protected)
- Check JSON snake_case format

---

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `customOAuth2User is null` | Endpoint not in JWTFilter whitelist | Add path to `REQUIRE_AUTH_PATH_PATTERNS` |
| JSON field is `null` | snake_case mismatch | Add `@JsonProperty("snake_case")` to DTO |
| QueryDSL Q-classes not found | Entity changed, not recompiled | Run `./gradlew compileJava` |
| 401 Unauthorized on protected endpoint | Missing/invalid JWT token | Check `Authorization: Bearer {token}` header |

---

## Entity & QueryDSL Workflow

**Adding new entity:**
1. Create entity class in `domain/*/entity/` (extend `BaseTimeEntity` if needed)
2. Run `./gradlew compileJava` to generate Q-classes
3. Create custom repository if complex queries needed:
   - Interface in `repository/custom/`
   - Implementation in `repository/Impl/` using `JPAQueryFactory`

---

## 🔒 Security: Preventing Credential Exposure

**⚠️ CRITICAL: Never commit sensitive information to Git**

### Files That Must NOT Contain Real Credentials

❌ **Never put real secrets in these files:**
- `CLAUDE.md` - This is committed to Git!
- `application.properties` - Use placeholders only
- `application.yml` - Use placeholders only
- Any `.md` files
- Any files tracked by Git

✅ **Real credentials should only be in:**
- `application-local.properties` (add to .gitignore)
- `.env` files (add to .gitignore)
- Environment variables on server
- Secret management systems (AWS Secrets Manager, etc.)

### Pre-Commit Checklist

**Before committing any changes, verify:**

```bash
# 1. Check for hardcoded secrets (run from project root)
grep -r "JWT_SECRET=.*[a-zA-Z0-9]" src/ CLAUDE.md README.md || echo "✓ No JWT secrets found"
grep -r "PASSWORD=.*[^$]" src/ CLAUDE.md README.md || echo "✓ No passwords found"
grep -r "_KEY=.*[A-Z0-9]\{20,\}" src/ CLAUDE.md README.md || echo "✓ No API keys found"

# 2. Verify .gitignore includes sensitive files
grep -E "application-local|\.env" .gitignore || echo "⚠️  Add sensitive files to .gitignore!"

# 3. Check staged files don't include secrets
git diff --cached
```

### Safe Credential Format Examples

**✅ SAFE (placeholders):**
```properties
JWT_SECRET=your-super-secret-key-for-local-dev-at-least-32-chars-long
AWS_S3_ACCESS_KEY=YOUR_ACCESS_KEY_HERE
POSTGRE_PASSWORD=${POSTGRE_PASSWORD}
```

**❌ DANGEROUS (real secrets example - DO NOT USE):**
```properties
# These look like real secrets - NEVER commit patterns like these!
JWT_SECRET=3k9s8djf0923jf0s9d8jf0s9d8jf0s9d
AWS_S3_ACCESS_KEY=AKXXXXXXXXXXXXXXXXXX  # Real key starts with AKIA + 16 chars
POSTGRE_PASSWORD=MyRealPassword123!
```

### Automated Validation Script

**Run before every commit:**

```bash
# Make executable (first time only)
chmod +x scripts/check-secrets.sh

# Run validation
./scripts/check-secrets.sh
```

**What it checks:**
- ✓ AWS access keys in staged files
- ✓ Real passwords and secrets in CLAUDE.md
- ✓ Long secret values in application.properties
- ✓ Required patterns in .gitignore

**Set up as Git pre-commit hook (optional):**
```bash
# Create pre-commit hook
cat > .git/hooks/pre-commit << 'EOF'
#!/bin/bash
./scripts/check-secrets.sh
EOF

chmod +x .git/hooks/pre-commit
```

Now Git will automatically check for secrets before every commit!

### If You Accidentally Committed Secrets

**DO NOT just remove it in the next commit!** The secret is still in Git history.

1. **Immediately rotate the exposed credential** (change password, regenerate key)
2. **Remove from Git history:**
   ```bash
   # Use git-filter-repo or BFG Repo-Cleaner
   git filter-repo --path application.properties --invert-paths
   git push --force
   ```
3. **Update .gitignore** to prevent future accidents
4. **Inform your team** about the rotation
