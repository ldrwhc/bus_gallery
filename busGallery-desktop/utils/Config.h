#ifndef CONFIG_H
#define CONFIG_H

#include <QString>

namespace Config {

// Server
inline const QString SERVER_BASE = "https://192.144.227.251";
inline const QString API_BASE = SERVER_BASE + "/api";

// Auth endpoints (API_BASE already includes /api)
inline const QString AUTH_LOGIN = "/auth/login";
inline const QString AUTH_LOGOUT = "/auth/logout";
inline const QString AUTH_CAPTCHA = "/auth/captcha";

// Catalog endpoints
inline const QString CATALOG_REGIONS = "/regions";
inline const QString CATALOG_BRANDS = "/catalog/brands";
inline const QString CATALOG_MODELS = "/catalog/models";
inline const QString CATALOG_COMPANIES = "/companies";

// Upload endpoint
inline const QString UPLOAD_VEHICLE = "/upload";

// Route search (pre-load active routes for autocomplete)
inline const QString ROUTES_SEARCH = "/routes";

// Model vehicle config lookup
inline QString modelVehicles(qint64 modelId) {
    return QString("/models/%1/vehicles").arg(modelId);
}

// Limits
inline const qint64 MAX_FILE_SIZE = 15 * 1024 * 1024; // 15 MB
inline const QStringList SUPPORTED_EXTENSIONS = {"jpg", "jpeg", "png"};

// QSettings keys
inline const QString SETTING_TOKEN = "auth/token";
inline const QString SETTING_USERNAME = "auth/username";
inline const QString SETTING_DISPLAY_NAME = "auth/displayName";
inline const QString SETTING_ROLE = "auth/role";
inline const QString SETTING_REMEMBER = "auth/remember";
inline const QString SETTING_TRUST_SSL = "ssl/trustSelfSigned";

} // namespace Config

#endif // CONFIG_H
