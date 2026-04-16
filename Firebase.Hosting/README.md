# Firebase Hosting

Hosts the Tyche public website (privacy policy, deep links configuration).

## Prerequisites

Install the Firebase CLI: https://firebase.google.com/docs/cli

## Login

```bash
firebase login
```

To re-authenticate if your session has expired:

```bash
firebase login --reauth
```

## Deploy

From this directory (`Firebase.Hosting/`):

```bash
firebase deploy --only hosting
```

The site will be available at: https://tyche-588ce.web.app
