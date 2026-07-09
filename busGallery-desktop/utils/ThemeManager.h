#ifndef THEMEMANAGER_H
#define THEMEMANAGER_H

#include <QObject>
#include <QString>
#include <QColor>
#include <QApplication>
#include <QPalette>
#include <QStyleFactory>

// Light theme colors
namespace Light {
    inline const QString WindowBg      = "#f5f5f5";
    inline const QString BaseBg        = "#ffffff";
    inline const QString AltBg         = "#fafafa";
    inline const QString TextPrimary   = "#262626";
    inline const QString TextSecondary = "#595959";
    inline const QString TextTertiary  = "#8c8c8c";
    inline const QString Border        = "#d9d9d9";
    inline const QString BorderLight   = "#e8e8e8";
    inline const QString Divider       = "#f0f0f0";
    inline const QString Accent        = "#1677ff";
    inline const QString AccentHover   = "#4096ff";
    inline const QString AccentPress   = "#0958d9";
    inline const QString Success       = "#52c41a";
    inline const QString Warning       = "#faad14";
    inline const QString Error         = "#ff4d4f";
    inline const QString DropBg        = "#fafafa";
    inline const QString DropBorder    = "#bfbfbf";
    inline const QString DisabledBg    = "#f5f5f5";
    inline const QString DisabledText  = "#bfbfbf";
}

// Dark theme colors
namespace Dark {
    inline const QString WindowBg      = "#1f1f1f";
    inline const QString BaseBg        = "#2d2d2d";
    inline const QString AltBg         = "#262626";
    inline const QString TextPrimary   = "#e8e8e8";
    inline const QString TextSecondary = "#b0b0b0";
    inline const QString TextTertiary  = "#707070";
    inline const QString Border        = "#434343";
    inline const QString BorderLight   = "#383838";
    inline const QString Divider       = "#303030";
    inline const QString Accent        = "#1677ff";
    inline const QString AccentHover   = "#4096ff";
    inline const QString AccentPress   = "#0958d9";
    inline const QString Success       = "#49aa19";
    inline const QString Warning       = "#d89614";
    inline const QString Error         = "#dc4446";
    inline const QString DropBg        = "#262626";
    inline const QString DropBorder    = "#555555";
    inline const QString DisabledBg    = "#2d2d2d";
    inline const QString DisabledText  = "#555555";
}

class ThemeManager : public QObject
{
    Q_OBJECT
public:
    static ThemeManager &instance();

    bool isDark() const { return m_dark; }
    void setDark(bool dark);
    void refreshFromSystem();

    // Get themed color by name
    static QString color(const QString &lightColor, const QString &darkColor);

    // Generate the full application stylesheet
    QString stylesheet() const;

    // Generate a dark-mode aware QPalette
    static QPalette palette(bool dark);

signals:
    void themeChanged(bool dark);

private:
    ThemeManager();
    bool m_dark = false;
};

#endif // THEMEMANAGER_H
