#include <QApplication>
#include <QFont>
#include <QStyleFactory>
#include <QSslSocket>
#include <QMessageBox>
#include <QTimer>
#include "utils/ThemeManager.h"
#include "api/ApiClient.h"
#include "ui/MainWindow.h"

int main(int argc, char *argv[])
{
    QApplication app(argc, argv);

    app.setApplicationName("BusGallery Desktop");
    app.setOrganizationName("BusGallery");
    app.setStyle(QStyleFactory::create("Fusion"));

    // Set Chinese font
    QFont defaultFont("Microsoft YaHei", 10);
    app.setFont(defaultFont);

    // Init theme manager
    auto &theme = ThemeManager::instance();

    // Apply palette-based theming
    auto applyTheme = [&app]() {
        bool dark = ThemeManager::instance().isDark();
        app.setPalette(ThemeManager::palette(dark));
        app.setStyleSheet(ThemeManager::instance().stylesheet());
    };
    applyTheme();

    // Watch for Windows theme changes (poll every 2s)
    auto *themeTimer = new QTimer(&app);
    QObject::connect(themeTimer, &QTimer::timeout, [&theme, applyTheme]() {
        bool wasDark = theme.isDark();
        theme.refreshFromSystem();
        if (theme.isDark() != wasDark) {
            applyTheme();
        }
    });
    themeTimer->start(2000);

    QObject::connect(&theme, &ThemeManager::themeChanged, [applyTheme](bool) {
        applyTheme();
    });

    // Check SSL
    if (!QSslSocket::supportsSsl()) {
        QMessageBox::warning(nullptr, "SSL",
            QString::fromUtf8("系统不支持 SSL，可能无法连接服务器。\n请安装 OpenSSL 库。"));
    }

    ApiClient apiClient;
    MainWindow mainWindow(&apiClient);
    mainWindow.show();

    return app.exec();
}
