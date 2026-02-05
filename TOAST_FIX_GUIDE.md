# Fix Continuous Toast Notifications - Troubleshooting Guide

## Common Causes and Solutions

### 1. **Infinite Re-renders in React/Vue/Angular**

**Problem:** Toast triggers on every component render
```javascript
// ❌ Wrong - triggers on every render
function MyComponent() {
  showToast("Message"); // This runs on every render
  return <div>Content</div>;
}
```

**Solution:** Use useEffect or proper lifecycle methods
```javascript
// ✅ Correct - triggers only when needed
function MyComponent() {
  useEffect(() => {
    showToast("Message");
  }, []); // Empty dependency array
  
  return <div>Content</div>;
}
```

### 2. **API Call in Wrong Location**

**Problem:** API call in component body causes continuous requests
```javascript
// ❌ Wrong
function MyComponent() {
  const response = fetch('/api/data'); // Runs on every render
  if (response.error) showToast("Error");
}
```

**Solution:** Move API calls to useEffect or event handlers
```javascript
// ✅ Correct
function MyComponent() {
  useEffect(() => {
    fetch('/api/data')
      .then(response => response.json())
      .catch(error => showToast("Error"));
  }, []);
}
```

### 3. **Missing Dependencies in useEffect**

**Problem:** Missing dependencies cause continuous execution
```javascript
// ❌ Wrong
useEffect(() => {
  if (someCondition) {
    showToast("Message");
  }
}, []); // Missing 'someCondition' in dependencies
```

**Solution:** Include all dependencies
```javascript
// ✅ Correct
useEffect(() => {
  if (someCondition) {
    showToast("Message");
  }
}, [someCondition]); // Include all dependencies
```

### 4. **Toast Library Configuration Issues**

**Problem:** Toast library not properly configured for duplicates

**Solution for React-Toastify:**
```javascript
import { toast } from 'react-toastify';

// Prevent duplicate toasts
const showToast = (message) => {
  if (!toast.isActive(message)) {
    toast(message, { toastId: message });
  }
};
```

**Solution for other libraries:**
```javascript
// Check if toast already exists before showing
let currentToast = null;

const showToast = (message) => {
  if (currentToast) {
    currentToast.close(); // Close existing toast
  }
  currentToast = toast(message);
};
```

### 5. **Event Listener Issues**

**Problem:** Multiple event listeners attached
```javascript
// ❌ Wrong - adds listener on every render
function MyComponent() {
  window.addEventListener('error', () => showToast("Error"));
}
```

**Solution:** Proper cleanup
```javascript
// ✅ Correct
useEffect(() => {
  const handleError = () => showToast("Error");
  window.addEventListener('error', handleError);
  
  return () => {
    window.removeEventListener('error', handleError);
  };
}, []);
```

### 6. **State Update Loops**

**Problem:** State updates trigger more state updates
```javascript
// ❌ Wrong
const [data, setData] = useState(null);

useEffect(() => {
  setData(newData); // This might trigger another useEffect
  showToast("Data updated");
}, [data]); // This creates a loop
```

**Solution:** Use proper conditions
```javascript
// ✅ Correct
useEffect(() => {
  if (!data) {
    fetchData().then(newData => {
      setData(newData);
      showToast("Data loaded");
    });
  }
}, []); // Only run once
```

## Quick Fixes to Try:

### 1. **Add Toast ID/Key**
```javascript
// React-Toastify
toast("Message", { toastId: "unique-id" });

// Custom implementation
const toastShown = new Set();
const showToast = (message) => {
  if (!toastShown.has(message)) {
    toastShown.add(message);
    toast(message);
    setTimeout(() => toastShown.delete(message), 5000);
  }
};
```

### 2. **Debounce Toast Function**
```javascript
import { debounce } from 'lodash';

const debouncedToast = debounce((message) => {
  toast(message);
}, 300);
```

### 3. **Use Ref to Track Toast State**
```javascript
const toastShownRef = useRef(false);

useEffect(() => {
  if (!toastShownRef.current) {
    showToast("Message");
    toastShownRef.current = true;
  }
}, []);
```

### 4. **Clear Previous Toasts**
```javascript
// Before showing new toast
toast.dismiss(); // Clear all toasts
toast("New message");
```

## Backend Considerations (Spring Boot)

If the issue is from your Spring Boot API:

### 1. **Check Response Headers**
Ensure your API doesn't send duplicate responses:
```java
@PostMapping("/api/data")
public ResponseEntity<String> handleRequest() {
    // Ensure single response
    return ResponseEntity.ok("Success message");
}
```

### 2. **Avoid Multiple Notifications**
```java
// In your service layer
public void processData() {
    try {
        // Process data
        notificationService.sendOnce("SUCCESS", "Data processed");
    } catch (Exception e) {
        notificationService.sendOnce("ERROR", "Processing failed");
    }
}
```

## Debugging Steps:

1. **Check Browser Console** - Look for errors or warnings
2. **Check Network Tab** - See if API calls are being made repeatedly
3. **Add Console Logs** - Track when toast function is called
4. **Use React DevTools** - Check component re-renders
5. **Check Toast Library Documentation** - Verify proper usage

## Example Fix for Common Scenario:

```javascript
// Before (causing continuous toasts)
function LoginComponent() {
  const [error, setError] = useState(null);
  
  if (error) {
    toast.error(error); // Runs on every render when error exists
  }
  
  return <LoginForm onError={setError} />;
}

// After (fixed)
function LoginComponent() {
  const [error, setError] = useState(null);
  
  useEffect(() => {
    if (error) {
      toast.error(error);
      setError(null); // Clear error after showing toast
    }
  }, [error]);
  
  return <LoginForm onError={setError} />;
}
```

## Need More Help?

If none of these solutions work, please share:
1. Your frontend framework (React, Vue, Angular, etc.)
2. Toast library you're using
3. The specific code where toast is being called
4. Browser console errors (if any)