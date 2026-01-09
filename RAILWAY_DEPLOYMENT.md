# Railway Deployment Guide

This guide will help you deploy your DSE Backend to Railway for your frontend team to access.

## 🚀 Quick Start

Railway makes deployment super simple! Follow these steps:

### Step 1: Push Your Code to Git

Make sure all your changes are committed and pushed:

```bash
git add .
git commit -m "Ready for Railway deployment"
git push origin feature/deploy
```

### Step 2: Sign Up for Railway

1. Go to: https://railway.app
2. Click **"Start a New Project"**
3. Sign up with GitHub (recommended) or email

### Step 3: Create New Project

1. Click **"New Project"**
2. Select **"Deploy from GitHub repo"**
3. Connect your repository
4. Select the `feature/deploy` branch

### Step 4: Add PostgreSQL Database

1. In your Railway project, click **"+ New"**
2. Select **"Database"** → **"Add PostgreSQL"**
3. Railway will automatically create a PostgreSQL database
4. **Note the connection details** (you'll see them in the database service)

### Step 5: Configure Environment Variables

1. Go to your **Web Service** (the one that deployed from your repo)
2. Click on the service → **"Variables"** tab
3. Click **"+ New Variable"** and add these:

#### Required Variables:

| Variable Name | Value                          | Notes |
|--------------|--------------------------------|-------|
| `JWT_SECRET` | Your JWT Secret                | Use the one we generated |
| `MAIL_HOST` | `smtp-relay.brevo.com`         | Your email service |
| `MAIL_PORT` | `587`                          | SMTP port |
| `MAIL_USERNAME` | `your-brevo-email@example.com` | Your Brevo email |
| `MAIL_PASSWORD` | `your-smtp-key`                | Brevo SMTP key |
| `MAIL_FROM` | `noreply@yourdomain.com`       | Sender email |

#### Database Variables (Auto-set by Railway):

Railway automatically sets these when you link the database:
- `DATABASE_URL` - Full connection string
- `PGHOST` - Database host
- `PGPORT` - Database port
- `PGUSER` - Database user
- `PGPASSWORD` - Database password
- `PGDATABASE` - Database name

**To link database:**
1. In your web service → **"Variables"** tab
2. Click **"Add Reference"**
3. Select your PostgreSQL database
4. Railway will auto-populate database variables

### Step 6: Deploy

Railway will automatically:
1. Detect your `Dockerfile`
2. Build your application
3. Deploy it
4. Give you a public URL

**Monitor the deployment:**
- Go to your service → **"Deployments"** tab
- Watch the build logs
- Wait for "Deploy successful"

### Step 7: Get Your API URL

1. Go to your web service
2. Click **"Settings"** tab
3. Under **"Domains"**, you'll see your public URL
4. Example: `https://dse-backend-production.up.railway.app`

**Share this URL with your frontend team!**

---

## 📧 Email Setup (Brevo)

If you haven't set up Brevo yet:

1. **Sign up**: https://www.brevo.com (free tier: 300 emails/day)
2. **Get SMTP credentials**:
   - Go to **Settings** → **SMTP & API** → **SMTP**
   - Click **"Generate"** to create SMTP key
   - Save: Host, Port, Username, Password
3. **Add to Railway**: Use the credentials in environment variables

---

## ✅ Verify Deployment

### 1. Test Health Endpoint

```bash
curl https://your-app-name.up.railway.app/health
```

Should return a success response.

### 2. Test Registration

```bash
curl -X POST https://your-app-name.up.railway.app/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "test123456"
  }'
```

### 3. Check Logs

- Go to your service → **"Deployments"** tab
- Click on the latest deployment
- View **"Build Logs"** and **"Deploy Logs"**

---

## 🔧 Troubleshooting

### Build Fails

- **Check**: Dockerfile is correct
- **Check**: Java 21 is specified
- **Solution**: Review build logs in Railway dashboard

### Application Won't Start

- **Check**: All environment variables are set
- **Check**: Database is linked
- **Check**: Database connection string is correct
- **Solution**: Review deploy logs

### Database Connection Error

- **Check**: Database service is running
- **Check**: Database is linked to web service
- **Check**: `DATABASE_URL` or `DB_URL` is set
- **Solution**: Re-link database in Variables tab

### CORS Errors (Frontend Issues)

- **Current config**: Allows `http://localhost:3000`
- **For production**: Update `SecurityConfig.java` with your frontend URL
- **Solution**: Add your frontend domain to CORS allowed origins

### Email Not Sending

- **Check**: Brevo SMTP credentials are correct
- **Check**: `MAIL_FROM` is set
- **Check**: Brevo account is verified
- **Solution**: Test SMTP credentials in Brevo dashboard

---

## 📋 Environment Variables Reference

| Variable | Required | Description | Example                  |
|----------|----------|-------------|--------------------------|
| `JWT_SECRET` | ✅ Yes | Secret for JWT tokens | `Your JWT Secret         |
| `DATABASE_URL` | ✅ Yes | PostgreSQL connection string | Auto-set by Railway      |
| `MAIL_HOST` | ✅ Yes | SMTP server | `smtp-relay.brevo.com`   |
| `MAIL_PORT` | ✅ Yes | SMTP port | `587`                    |
| `MAIL_USERNAME` | ✅ Yes | SMTP username | Your Brevo email         |
| `MAIL_PASSWORD` | ✅ Yes | SMTP password | Your Brevo SMTP key      |
| `MAIL_FROM` | ✅ Yes | Sender email | `noreply@yourdomain.com` |
| `PORT` | ❌ No | Server port (Railway sets automatically) | Auto-set                 |

---

## 🎯 Quick Checklist

Before deploying:
- [ ] Code pushed to `feature/deploy` branch
- [ ] Railway account created
- [ ] Brevo account created (for email)

During deployment:
- [ ] Project created on Railway
- [ ] PostgreSQL database added
- [ ] Web service deployed
- [ ] Database linked to web service
- [ ] All environment variables set

After deployment:
- [ ] Build successful
- [ ] Service started
- [ ] Health endpoint works
- [ ] Test registration works
- [ ] API URL shared with frontend team

---

## 🚀 Railway Features

### Free Tier Includes:
- ✅ 500 hours of usage/month
- ✅ $5 credit monthly
- ✅ PostgreSQL database
- ✅ Automatic deployments
- ✅ Custom domains
- ✅ Environment variables
- ✅ Build logs

### Auto-Deploy:
- Railway automatically deploys when you push to your branch
- No manual deployment needed!

### Custom Domain:
- You can add a custom domain in Settings → Domains
- Railway provides SSL certificates automatically

---

## 📞 Need Help?

1. Check Railway service logs
2. Check Railway build logs
3. Verify all environment variables
4. Test database connectivity
5. Review this guide's troubleshooting section

---

## 🎉 Next Steps

After successful deployment:

1. ✅ Test all API endpoints
2. ✅ Share API URL with frontend team
3. ✅ Update CORS if frontend is on different domain
4. ✅ Monitor application performance
5. ✅ Set up custom domain (optional)

Your backend is now live and accessible to your frontend team! 🚀

