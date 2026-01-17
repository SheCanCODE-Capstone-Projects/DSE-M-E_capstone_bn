# JWT REST API Security Configuration - Best Practices

## Overview
This document outlines the Spring Security configuration for JWT-based REST APIs that return proper JSON responses instead of HTML redirects.

## Key Components

### 1. AuthEntryPointJwt (401 Unauthorized)
**Purpose**: Handles unauthenticated requests to protected endpoints
**Response**: JSON with 401 status code

```json
{
  "timestamp": "2024-01-17T08:15:30.123Z",
  "status": 401,
  "error": "Unauthorized", 
  "message": "Authentication required. Please provide a valid JWT token.",
  "path": "/api/courses"
}
```

### 2. CustomAccessDeniedHandler (403 Forbidden)
**Purpose**: Handles authenticated requests with insufficient permissions
**Response**: JSON with 403 status code

```json
{
  "timestamp": "2024-01-17T08:15:30.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. You don't have permission to access this resource.",
  "path": "/api/admin/users"
}
```

## Security Configuration Best Practices

### 1. Disable Form Login for REST APIs
```java
.formLogin(AbstractHttpConfigurer::disable) // No HTML login pages
```

### 2. Use Stateless Session Management
```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

### 3. Configure Custom Exception Handlers
```java
.exceptionHandling(exceptions -> exceptions
    .authenticationEntryPoint(authEntryPointJwt)
    .accessDeniedHandler(customAccessDeniedHandler)
)
```

### 4. Proper CORS Configuration
```java
configuration.setAllowedOriginPatterns(Arrays.asList(
    "http://localhost:3000",
    "https://your-frontend-domain.com"
));
configuration.setAllowedMethods(Arrays.asList(
    "GET", "POST", "PUT", "DELETE", "OPTIONS"
));
configuration.setAllowedHeaders(Arrays.asList(
    "Authorization", "Content-Type", "X-Requested-With"
));
```

## API Testing Examples

### 1. Test Unauthorized Access (401)
```bash
curl -X GET http://localhost:8088/api/courses
```
**Expected Response**:
```json
{
  "timestamp": "2024-01-17T08:15:30.123Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication required. Please provide a valid JWT token.",
  "path": "/api/courses"
}
```

### 2. Test with Valid JWT Token
```bash
curl -X GET http://localhost:8088/api/courses \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```
**Expected Response**: Course data (200 OK)

### 3. Test Insufficient Permissions (403)
```bash
curl -X GET http://localhost:8088/api/admin/users \
  -H "Authorization: Bearer <facilitator_token>"
```
**Expected Response**:
```json
{
  "timestamp": "2024-01-17T08:15:30.123Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. You don't have permission to access this resource.",
  "path": "/api/admin/users"
}
```

## Swagger/OpenAPI Integration

### 1. Security Scheme Configuration
```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
        .components(new Components()
            .addSecuritySchemes("Bearer Authentication", 
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
            )
        );
}
```

### 2. Swagger UI Testing
1. Navigate to `http://localhost:8088/swagger-ui.html`
2. Click "Authorize" button
3. Enter: `Bearer <your_jwt_token>`
4. Test endpoints directly from Swagger UI

## Frontend Integration Best Practices

### 1. Axios Interceptor Example
```javascript
// Request interceptor to add JWT token
axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle auth errors
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Redirect to login
      localStorage.removeItem('jwt_token');
      window.location.href = '/login';
    } else if (error.response?.status === 403) {
      // Show access denied message
      alert('Access denied. You don\'t have permission for this action.');
    }
    return Promise.reject(error);
  }
);
```

### 2. React Error Handling
```javascript
const handleApiCall = async () => {
  try {
    const response = await api.get('/api/courses');
    setCourses(response.data);
  } catch (error) {
    if (error.response?.status === 401) {
      setError('Please log in to access this resource');
      // Redirect to login
    } else if (error.response?.status === 403) {
      setError('You don\'t have permission to view courses');
    } else {
      setError('An error occurred while fetching courses');
    }
  }
};
```

## Security Headers

### 1. Additional Security Headers
```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.headers(headers -> headers
        .frameOptions().deny()
        .contentTypeOptions().and()
        .httpStrictTransportSecurity(hsts -> hsts
            .maxAgeInSeconds(31536000)
            .includeSubdomains(true)
        )
    );
    return http.build();
}
```

## Monitoring and Logging

### 1. Security Event Logging
```java
@EventListener
public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
    log.info("Authentication successful for user: {}", 
        event.getAuthentication().getName());
}

@EventListener  
public void handleAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    log.warn("Authentication failed: {}", event.getException().getMessage());
}
```

### 2. Request Logging
```java
@Component
public class RequestLoggingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        log.info("Request: {} {} from {}", 
            httpRequest.getMethod(), 
            httpRequest.getRequestURI(),
            httpRequest.getRemoteAddr());
        chain.doFilter(request, response);
    }
}
```

## Common Issues and Solutions

### 1. CORS Preflight Issues
**Problem**: OPTIONS requests failing
**Solution**: Explicitly allow OPTIONS method
```java
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
```

### 2. JWT Token Format
**Problem**: Token not recognized
**Solution**: Ensure proper Bearer format
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 3. Session Management
**Problem**: Unexpected session creation
**Solution**: Use STATELESS session policy
```java
.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
```

This configuration ensures your REST API behaves correctly with JSON responses for all authentication and authorization scenarios.