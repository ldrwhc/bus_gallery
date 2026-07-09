#include "AuthApi.h"
#include "utils/Config.h"
#include <QSettings>
#include <QByteArray>
#include <QBuffer>
#include <QPixmap>

AuthApi::AuthApi(ApiClient *client, QObject *parent)
    : QObject(parent), m_client(client)
{
}

void AuthApi::login(const QString &username, const QString &password,
                     const QString &captchaId, const QString &captchaCode)
{
    QJsonObject body;
    body["username"] = username;
    body["password"] = password;
    if (!captchaId.isEmpty()) {
        body["captchaId"] = captchaId;
        body["captchaCode"] = captchaCode;
    }

    m_client->post(Config::AUTH_LOGIN, body,
        [this](int /*status*/, const QJsonObject &json) {
            LoginResult result;
            result.token = json["token"].toString();
            result.profile = UserProfile::fromJson(json["profile"].toObject());
            emit loginSuccess(result);
        },
        [this](int /*status*/, const QString &code, const QString &message) {
            emit loginError(code, message);
        }
    );
}

void AuthApi::logout()
{
    m_client->post(Config::AUTH_LOGOUT, QJsonObject(),
        [this](int, const QJsonObject &) {
            clearSession();
            emit loggedOut();
        },
        [this](int, const QString &, const QString &) {
            // Even if logout fails, clear local session
            clearSession();
            emit loggedOut();
        }
    );
}

void AuthApi::fetchCaptcha()
{
    m_client->get(Config::AUTH_CAPTCHA + "?scene=login",
        [this](int, const QJsonObject &json) {
            QString captchaId = json["captchaId"].toString();
            QByteArray imageData = QByteArray::fromBase64(json["imageBase64"].toString().toUtf8());
            QPixmap pixmap;
            pixmap.loadFromData(imageData);
            emit captchaReady(captchaId, pixmap);
        },
        [this](int, const QString &, const QString &msg) {
            emit captchaError(msg);
        }
    );
}

void AuthApi::saveSession(const QString &token, const UserProfile &profile)
{
    QSettings settings;
    settings.setValue(Config::SETTING_TOKEN, token);
    settings.setValue(Config::SETTING_USERNAME, profile.username);
    settings.setValue(Config::SETTING_DISPLAY_NAME, profile.displayName);
    settings.setValue(Config::SETTING_ROLE, profile.role);
    settings.setValue(Config::SETTING_REMEMBER, true);
    m_client->setToken(token);
}

bool AuthApi::restoreSession(QString &token, UserProfile &profile)
{
    QSettings settings;
    if (!settings.value(Config::SETTING_REMEMBER, false).toBool())
        return false;

    token = settings.value(Config::SETTING_TOKEN).toString();
    if (token.isEmpty())
        return false;

    profile.username = settings.value(Config::SETTING_USERNAME).toString();
    profile.displayName = settings.value(Config::SETTING_DISPLAY_NAME).toString();
    profile.role = settings.value(Config::SETTING_ROLE).toString();
    m_client->setToken(token);
    return true;
}

void AuthApi::clearSession()
{
    QSettings settings;
    settings.remove(Config::SETTING_TOKEN);
    settings.remove(Config::SETTING_USERNAME);
    settings.remove(Config::SETTING_DISPLAY_NAME);
    settings.remove(Config::SETTING_ROLE);
    settings.setValue(Config::SETTING_REMEMBER, false);
    m_client->setToken("");
}
