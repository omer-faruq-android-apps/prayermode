# Docker ile Prayer Mode Uygulamasını Derleme

Bu kılavuz, Prayer Mode Android uygulamasını Docker kullanarak, bilgisayarınıza Android SDK veya Gradle kurmadan nasıl derleyeceğinizi gösterir.

## Gereksinimler

- **Docker Desktop** (Windows için)
  - İndirme: https://www.docker.com/products/docker-desktop/
  - Docker Desktop'ın çalışır durumda olması gerekir

## Hızlı Başlangıç

### 1. Debug APK Derlemek

En basit yöntem, hazır scripti çift tıklamaktır:

```
docker-build.bat
```

Bu script:
- Docker image'ını oluşturur
- Debug APK'yı derler
- APK'yı `app\build\outputs\apk\debug\app-debug.apk` konumuna kaydeder

### 2. Release APK Derlemek

Release (imzalı) APK için:

```
docker-build-release.bat
```

**Not:** Release build için `keystore.properties` dosyası gereklidir.

### 3. Build Dosyalarını Temizlemek

```
docker-clean.bat
```

## Manuel Komutlar

Script kullanmak istemiyorsanız, manuel olarak şu komutları kullanabilirsiniz:

### Docker Image Oluşturma

```bash
docker build -t prayermode-builder .
```

### Debug APK Derleme

```bash
docker run --rm -v "%CD%:/workspace" prayermode-builder ./gradlew --no-daemon assembleDebug
```

### Release APK Derleme

```bash
docker run --rm -v "%CD%:/workspace" prayermode-builder ./gradlew --no-daemon assembleRelease
```

### Temizleme

```bash
docker run --rm -v "%CD%:/workspace" prayermode-builder ./gradlew --no-daemon clean
```

## Çıktı Konumları

- **Debug APK:** `app\build\outputs\apk\debug\app-debug.apk`
- **Release APK:** `app\build\outputs\apk\release\app-release.apk`

## Dockerfile Detayları

Dockerfile şunları içerir:
- **Base Image:** Eclipse Temurin 17 JDK (Jammy)
- **Android SDK:** Command-line tools (latest)
- **Build Tools:** 34.0.0
- **Platform:** Android 34 (API Level 34)

## Sorun Giderme

### Docker çalışmıyor hatası

```
ERROR: Docker is not running!
```

**Çözüm:** Docker Desktop'ı başlatın ve tekrar deneyin.

### Build hatası

```
ERROR: Failed to build APK!
```

**Çözüm:** 
1. `docker-clean.bat` ile temizleyin
2. Tekrar derlemeyi deneyin
3. Hata devam ederse, Docker image'ını yeniden oluşturun:
   ```bash
   docker rmi prayermode-builder
   docker build -t prayermode-builder .
   ```

### Keystore hatası (Release build)

```
WARNING: keystore.properties not found!
```

**Çözüm:** Release build için signing yapılandırması gereklidir. Debug build kullanın veya `keystore.properties` dosyasını oluşturun.

## Avantajlar

✅ Android SDK kurulumu gerektirmez  
✅ Gradle kurulumu gerektirmez  
✅ Tutarlı build ortamı (her yerde aynı şekilde çalışır)  
✅ Sistem temiz kalır  
✅ Farklı projeler için farklı Android SDK versiyonları kullanabilirsiniz  

## Notlar

- İlk build biraz uzun sürebilir (Docker image indirme + Gradle dependencies)
- Sonraki build'ler çok daha hızlı olacaktır (cache sayesinde)
- Build dosyaları `build/` klasöründe saklanır ve `.dockerignore` ile Docker'a kopyalanmaz
