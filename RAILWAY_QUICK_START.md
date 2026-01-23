# Railway Quick Start Guide

## 🚀 Deploy in 5 Minutes

### 1. Push Your Code
```bash
git add .
git commit -m "Ready for Railway"
git push origin feature/deploy
```

### 2. Create Railway Project
1. Go to https://railway.app
2. Click **"New Project"** → **"Deploy from GitHub repo"**
3. Select your repo and `feature/deploy` branch

### 3. Add Database
1. Click **"+ New"** → **"Database"** → **"Add PostgreSQL"**
2. Railway creates it automatically

### 4. Link Database
1. Go to your web service → **"Variables"** tab
2. Click **"Add Reference"** → Select your PostgreSQL database
3. Railway auto-sets database variables ✅

### 5. Set Environment Variables
In your web service → **"Variables"** tab, add:

| Variable | Value                    |
|----------|--------------------------|
| `JWT_SECRET` | Your JWT Secret          |
| `MAIL_HOST` | `smtp-relay.brevo.com`   |
| `MAIL_PORT` | `587`                    |
| `MAIL_USERNAME` | Your Brevo email         |
| `MAIL_PASSWORD` | Your Brevo SMTP key      |
| `MAIL_FROM` | `noreply@yourdomain.com` |

### 6. Get Your API URL
1. Go to service → **"Settings"** → **"Domains"**
2. Your URL: `https://your-app-name.up.railway.app`
3. **Share with frontend team!** 🎉

---

## 📧 Need Email Setup?

1. Sign up: https://www.brevo.com
2. Get SMTP key from **Settings** → **SMTP & API**
3. Use credentials in Railway variables

---

## ✅ Test Your Deployment

```bash
# Health check
curl https://your-app-name.up.railway.app/health

# Test registration
curl -X POST https://your-app-name.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test123456"}'
```

---

## 🆘 Issues?

- **Build fails**: Check Dockerfile and logs
- **Won't start**: Check environment variables
- **Database error**: Verify database is linked
- **CORS errors**: Update `SecurityConfig.java` with frontend URL

See `RAILWAY_DEPLOYMENT.md` for detailed guide.

