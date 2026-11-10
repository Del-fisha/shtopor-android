# СпецВзвод — Android-клиент

Мобильный клиент к Core REST API (`/api/mobile/**`).

## Вход

1. Логин и пароль — те же, что в веб-системе.
2. Адрес сервера по умолчанию: **http://www.vzvod-shtopor.ru** (публичный домен с пробросом на Core :8080).
3. Роль (командир / полицейский) определяется на сервере после входа.

## Сборка

```bash
cd android-app
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`
