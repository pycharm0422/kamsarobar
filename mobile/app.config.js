// Expo app configuration. Values that differ per environment come from environment variables
// (put them in a .env file - see .env.example).
const allowHttp = process.env.ALLOW_HTTP === 'true';

module.exports = {
  expo: {
    name: 'Kamsar o Bar',
    slug: 'kamsarobar',
    scheme: 'kamsarobar',
    version: '1.0.0',
    orientation: 'portrait',
    icon: './assets/icon.png',
    userInterfaceStyle: 'light',
    ios: {
      supportsTablet: true,
      bundleIdentifier: 'com.kamsarobar.app',
    },
    android: {
      package: 'com.kamsarobar.app',
      adaptiveIcon: {
        backgroundColor: '#0f766e',
        foregroundImage: './assets/android-icon-foreground.png',
        backgroundImage: './assets/android-icon-background.png',
        monochromeImage: './assets/android-icon-monochrome.png',
      },
      // Needed for push notifications on Android (downloaded from your Firebase project - see mobile/README.md).
      ...(process.env.GOOGLE_SERVICES_JSON ? { googleServicesFile: process.env.GOOGLE_SERVICES_JSON } : {}),
    },
    web: {
      favicon: './assets/favicon.png',
    },
    plugins: [
      'expo-router',
      'expo-secure-store',
      'expo-image',
      '@react-native-community/datetimepicker',
      [
        'expo-notifications',
        {
          icon: './assets/notification-icon.png',
          color: '#0f766e',
        },
      ],
      [
        'expo-image-picker',
        {
          photosPermission: 'Kamsar o Bar needs your photos so you can add them to your posts.',
          cameraPermission: 'Kamsar o Bar uses the camera so you can take photos for your posts.',
        },
      ],
      [
        'expo-splash-screen',
        {
          image: './assets/splash-icon.png',
          imageWidth: 160,
          resizeMode: 'contain',
          backgroundColor: '#0f766e',
        },
      ],
      [
        'expo-build-properties',
        {
          // Plain http:// is only for testing against a computer on your Wi-Fi. Production uses https.
          android: { usesCleartextTraffic: allowHttp },
        },
      ],
    ],
    experiments: {
      typedRoutes: false,
    },
    extra: {
      // Filled in by `eas init`; required for push notifications.
      eas: process.env.EAS_PROJECT_ID ? { projectId: process.env.EAS_PROJECT_ID } : undefined,
    },
  },
};
