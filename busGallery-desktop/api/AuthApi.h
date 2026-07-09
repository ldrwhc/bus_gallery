#ifndef AUTHAPI_H
#define AUTHAPI_H

#include <QObject>
#include <QPixmap>
#include "ApiClient.h"
#include "models/UserProfile.h"

struct LoginResult {
    QString token;
    UserProfile profile;
};

class AuthApi : public QObject
{
    Q_OBJECT
public:
    explicit AuthApi(ApiClient *client, QObject *parent = nullptr);

    void login(const QString &username, const QString &password,
               const QString &captchaId = "", const QString &captchaCode = "");
    void logout();
    void fetchCaptcha();

    // Session persistence
    void saveSession(const QString &token, const UserProfile &profile);
    bool restoreSession(QString &token, UserProfile &profile);
    void clearSession();

signals:
    void loginSuccess(const LoginResult &result);
    void loginError(const QString &code, const QString &message);
    void captchaReady(const QString &captchaId, const QPixmap &image);
    void captchaError(const QString &message);
    void loggedOut();

private:
    ApiClient *m_client;
};

#endif // AUTHAPI_H
