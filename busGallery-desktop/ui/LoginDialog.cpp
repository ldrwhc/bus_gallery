#include "LoginDialog.h"
#include "utils/Config.h"
#include "utils/ThemeManager.h"
#include <QVBoxLayout>
#include <QFormLayout>
#include <QHBoxLayout>
#include <QApplication>
#include <QFont>
#include <QFrame>
#include <QMouseEvent>

LoginDialog::LoginDialog(ApiClient *apiClient, QWidget *parent)
    : QDialog(parent)
    , m_client(apiClient)
    , m_auth(new AuthApi(apiClient, this))
{
    setWindowTitle(QString::fromUtf8("登录 - Bus Gallery"));
    setFixedSize(420, 440);

    auto *mainLayout = new QVBoxLayout(this);
    mainLayout->setSpacing(12);
    mainLayout->setContentsMargins(40, 30, 40, 30);

    // Title
    auto *titleLabel = new QLabel(QString::fromUtf8("Bus Gallery 图片上传工具"), this);
    QFont titleFont;
    titleFont.setPointSize(16);
    titleFont.setBold(true);
    titleLabel->setFont(titleFont);
    titleLabel->setAlignment(Qt::AlignCenter);
    mainLayout->addWidget(titleLabel);

    // Server info
    m_serverLabel = new QLabel(QString::fromUtf8("服务器: ") + Config::SERVER_BASE, this);
    m_serverLabel->setAlignment(Qt::AlignCenter);
    m_serverLabel->setProperty("secondary", true);
    mainLayout->addWidget(m_serverLabel);

    mainLayout->addSpacing(10);

    // Form
    auto *formLayout = new QFormLayout();
    formLayout->setSpacing(8);

    m_usernameEdit = new QLineEdit(this);
    m_usernameEdit->setPlaceholderText(QString::fromUtf8("请输入用户名"));
    m_usernameEdit->setMinimumHeight(34);
    formLayout->addRow(QString::fromUtf8("用户名:"), m_usernameEdit);

    m_passwordEdit = new QLineEdit(this);
    m_passwordEdit->setPlaceholderText(QString::fromUtf8("请输入密码"));
    m_passwordEdit->setEchoMode(QLineEdit::Password);
    m_passwordEdit->setMinimumHeight(34);
    formLayout->addRow(QString::fromUtf8("密  码:"), m_passwordEdit);

    mainLayout->addLayout(formLayout);

    // Captcha (initially hidden)
    m_captchaWidget = new QWidget(this);
    auto *captchaLayout = new QVBoxLayout(m_captchaWidget);
    captchaLayout->setContentsMargins(0, 4, 0, 0);
    m_captchaImage = new QLabel(this);
    m_captchaImage->setAlignment(Qt::AlignCenter);
    m_captchaImage->setMinimumHeight(60);
    m_captchaImage->setCursor(Qt::PointingHandCursor);
    m_captchaEdit = new QLineEdit(this);
    m_captchaEdit->setPlaceholderText(QString::fromUtf8("请输入验证码"));
    m_captchaEdit->setMinimumHeight(34);
    captchaLayout->addWidget(m_captchaImage);
    captchaLayout->addWidget(m_captchaEdit);
    m_captchaWidget->setVisible(false);
    mainLayout->addWidget(m_captchaWidget);

    // Click captcha image to refresh
    m_captchaImage->installEventFilter(this);
    connect(m_auth, &AuthApi::captchaReady, this, &LoginDialog::onCaptchaReady);

    // Remember me
    m_rememberCheck = new QCheckBox(QString::fromUtf8("记住登录状态"), this);
    mainLayout->addWidget(m_rememberCheck);

    // Error label
    m_errorLabel = new QLabel(this);
    m_errorLabel->setProperty("error", true);
    m_errorLabel->setWordWrap(true);
    m_errorLabel->setVisible(false);
    mainLayout->addWidget(m_errorLabel);

    // Login button
    m_loginBtn = new QPushButton(QString::fromUtf8("登  录"), this);
    m_loginBtn->setMinimumHeight(38);
    m_loginBtn->setProperty("primary", true);
    mainLayout->addWidget(m_loginBtn);

    // Separator
    auto *sep = new QFrame(this);
    sep->setFrameShape(QFrame::HLine);
    sep->setProperty("divider", true);
    mainLayout->addWidget(sep);

    // Cancel button
    auto *cancelBtn = new QPushButton(QString::fromUtf8("取消"), this);
    cancelBtn->setMinimumHeight(34);
    cancelBtn->setProperty("secondary", true);
    mainLayout->addWidget(cancelBtn);

    // Connections
    connect(m_loginBtn, &QPushButton::clicked, this, &LoginDialog::onLogin);
    connect(cancelBtn, &QPushButton::clicked, this, &QDialog::reject);
    connect(m_passwordEdit, &QLineEdit::returnPressed, this, &LoginDialog::onLogin);

    connect(m_auth, &AuthApi::loginSuccess, this, [this](const LoginResult &result) {
        m_success = true;
        m_loginResult = result;
        if (m_rememberCheck->isChecked()) {
            m_auth->saveSession(result.token, result.profile);
        }
        accept();
    });

    connect(m_auth, &AuthApi::loginError, this, [this](const QString &code, const QString &message) {
        m_loginAttempts++;
        m_errorLabel->setText(message);
        m_errorLabel->setVisible(true);
        m_loginBtn->setEnabled(true);
        if (m_loginAttempts >= 3 || message.contains(QString::fromUtf8("验证码"))
            || message.contains("captcha")) {
            m_auth->fetchCaptcha();
        }
    });

    // Restore saved username
    QSettings settings;
    QString savedUser = settings.value(Config::SETTING_USERNAME).toString();
    if (!savedUser.isEmpty())
        m_usernameEdit->setText(savedUser);
}

void LoginDialog::onLogin()
{
    QString username = m_usernameEdit->text().trimmed();
    QString password = m_passwordEdit->text();

    if (username.isEmpty() || password.isEmpty()) {
        m_errorLabel->setText(QString::fromUtf8("请输入用户名和密码"));
        m_errorLabel->setVisible(true);
        return;
    }

    m_errorLabel->setVisible(false);
    m_loginBtn->setEnabled(false);
    m_loginBtn->setText(QString::fromUtf8("登录中..."));

    QString captchaCode = m_captchaEdit->text().trimmed();
    m_auth->login(username, password, m_captchaId, captchaCode);
}

void LoginDialog::onCaptchaReady(const QString &captchaId, const QPixmap &image)
{
    m_captchaId = captchaId;
    // Scale captcha image maintaining aspect ratio
    m_captchaImage->setPixmap(image.scaledToWidth(300, Qt::SmoothTransformation));
    m_captchaWidget->setVisible(true);
    m_captchaEdit->clear();
    m_captchaEdit->setFocus();
    m_captchaImage->setToolTip(QString::fromUtf8("点击刷新验证码"));
}

bool LoginDialog::eventFilter(QObject *obj, QEvent *event)
{
    if (obj == m_captchaImage && event->type() == QEvent::MouseButtonPress) {
        m_auth->fetchCaptcha();
        return true;
    }
    return QDialog::eventFilter(obj, event);
}
