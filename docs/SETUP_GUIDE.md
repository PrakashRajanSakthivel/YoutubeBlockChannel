# YouTubeWhitelist — Setup Guide

This guide will walk you through setting up **YouTubeWhitelist** with your own Google API credentials. This is required for Google Sign-In to work. It takes about **5 minutes**.

---

## Why Do I Need This?

YouTubeWhitelist uses the YouTube Data API to fetch videos and Google Sign-In for authentication. Google requires each user to have their own API credentials. Your credentials are stored **securely on your device only** — they are never shared or sent anywhere except to Google.

---

## Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Sign in with your Google account
3. Click **"Select a project"** at the top → **"New Project"**
4. Name it anything (e.g., `YouTubeWhitelist`) → Click **"Create"**
5. Make sure your new project is selected at the top

## Step 2: Enable the YouTube Data API

1. In the Google Cloud Console, go to **APIs & Services** → **Library**
   - Or go directly to: https://console.cloud.google.com/apis/library
2. Search for **"YouTube Data API v3"**
3. Click on it → Click **"Enable"**

## Step 3: Create OAuth Credentials

1. Go to **APIs & Services** → **Credentials**
   - Or go directly to: https://console.cloud.google.com/apis/credentials
2. Click **"+ Create Credentials"** → **"OAuth client ID"**
3. If prompted to configure the consent screen first:
   - Click **"Configure Consent Screen"**
   - Choose **"External"** → Click **"Create"**
   - Fill in the required fields:
     - **App name**: `YouTubeWhitelist` (or whatever you like)
     - **User support email**: your email
     - **Developer contact email**: your email
   - Click **"Save and Continue"** through the remaining steps (Scopes, Test users)
   - On the **"Test users"** page, click **"+ Add Users"** and add your own email address
   - Click **"Save and Continue"** → **"Back to Dashboard"**
4. Now go back to **Credentials** → **"+ Create Credentials"** → **"OAuth client ID"**
5. For **Application type**, choose **"Web application"**
6. Name it anything (e.g., `YouTubeWhitelist Android`)
7. Under **"Authorized redirect URIs"**, add:
   ```
   http://127.0.0.1
   ```
8. Click **"Create"**
9. **Copy the Client ID and Client Secret** — you'll need these in the next step!

> **Important**: The Client ID looks like: `123456789-abcdefg.apps.googleusercontent.com`
> The Client Secret looks like: `GOCSPX-abcdefghijklmnop`

## Step 4: Enter Credentials in the App

1. Open **YouTubeWhitelist** on your device
2. On the first launch, you'll see the **API Credentials** screen automatically
3. Paste your **Google Client ID** and **Google Client Secret**
4. The **YouTube API Key** field is optional (a built-in key is provided)
5. Tap **"Save & Continue"**
6. You'll be taken to the **Sign In** screen — sign in with your Google account

### Already set up? Need to change credentials later?

1. Enter **Parent Mode** (tap the lock icon → enter your PIN)
2. Scroll down to **"API Credentials"**
3. Update your credentials and tap **"Save & Continue"**

---

## Step 5 (Optional): Create Your Own YouTube API Key

The app comes with a built-in YouTube API Key, but it has a shared daily quota. If you experience errors loading videos, you can create your own:

1. In Google Cloud Console, go to **Credentials**
2. Click **"+ Create Credentials"** → **"API Key"**
3. Copy the key
4. (Recommended) Click **"Restrict Key"**:
   - Under **"API restrictions"**, select **"Restrict key"**
   - Choose **"YouTube Data API v3"** only
   - Click **"Save"**
5. Paste the key in the app's **"YouTube API Key"** field

---

## Troubleshooting

### "Sign-in failed" or "Token exchange failed"
- Make sure you selected **"Web application"** (not "Android") as the OAuth client type
- Make sure `http://127.0.0.1` is in the **Authorized redirect URIs**
- Make sure you added your Google account as a **Test user** in the OAuth consent screen

### "Access denied" or "403"
- Make sure the **YouTube Data API v3** is enabled for your project
- Check that your API key is not restricted to the wrong API

### Videos not loading
- You may have exceeded the daily API quota (10,000 units/day for free)
- Create your own YouTube API Key (Step 5) for a dedicated quota

### "App not verified" warning during sign-in
- This is normal for personal projects. Click **"Advanced"** → **"Go to YouTubeWhitelist (unsafe)"**
- This message appears because the app hasn't been verified by Google (it's your personal project)

---

## Security Notes

- Your credentials are encrypted with **AES-256** and stored **only on your device**
- They are never transmitted to any server except Google's authentication servers
- The app is **open source** — you can verify the code yourself on GitHub
- No analytics, no tracking, no data collection

---

## Need Help?

- Create an issue on [GitHub](https://github.com/PrakashRajanSakthivel/YouTubeWhitelist/issues)
- Include error messages and steps to reproduce
