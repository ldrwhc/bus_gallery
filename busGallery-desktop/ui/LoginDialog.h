#ifndef LOGINDIALOG_H
#define LOGINDIALOG_H

#include <QDialog>
#include <QLineEdit>
#include <QPushButton>
#include <QLabel>
#include <QCheckBox>
#include <QEvent>
#include "api/ApiClient.h"
#include "api/AuthApi.h"
#include "models/UserProfile.h"

class LoginDialog : public QDialog
{
    Q_OBJECT
public:
    explicit LoginDialog(ApiClient *apiClient, QWidget *parent = nullptr);

    QString token() const { return m_loginResult.token; }
    UserProfile profile() const { return m_loginResult.profile; }
    bool loginSuccess() const { return m_success; }

protected:
    bool eventFilter(QObject *obj, QEvent *event) override;

private slots:
    void onLogin();
    void onCaptchaReady(const QString &captchaId, const QPixmap &image);

private:
    ApiClient *m_client;
    AuthApi *m_auth;
    bool m_success = false;
    int m_loginAttempts = 0;
    QString m_captchaId;
    LoginResult m_loginResult;

    QLineEdit *m_usernameEdit;
    QLineEdit *m_passwordEdit;
    QLineEdit *m_captchaEdit;
    QLabel *m_captchaImage;
    QWidget *m_captchaWidget;
    QCheckBox *m_rememberCheck;
    QPushButton *m_loginBtn;
    QLabel *m_errorLabel;
    QLabel *m_serverLabel;
};

#endif // LOGINDIALOG_H
