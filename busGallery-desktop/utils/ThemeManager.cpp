#include "ThemeManager.h"
#include <QSettings>
#include <QStyle>

#ifdef Q_OS_WIN
#include <Windows.h>
#endif

ThemeManager &ThemeManager::instance()
{
    static ThemeManager inst;
    return inst;
}

ThemeManager::ThemeManager()
{
    refreshFromSystem();
}

void ThemeManager::refreshFromSystem()
{
#ifdef Q_OS_WIN
    // Read Windows app theme from registry
    QSettings reg(
        "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
        QSettings::NativeFormat);
    m_dark = reg.value("AppsUseLightTheme", 1).toInt() == 0;
#else
    m_dark = false;
#endif
}

void ThemeManager::setDark(bool dark)
{
    if (m_dark != dark) {
        m_dark = dark;
        emit themeChanged(dark);
    }
}

QString ThemeManager::color(const QString &lightColor, const QString &darkColor)
{
    return instance().isDark() ? darkColor : lightColor;
}

QPalette ThemeManager::palette(bool dark)
{
    QPalette p;
    if (dark) {
        p.setColor(QPalette::Window,          QColor(Dark::WindowBg));
        p.setColor(QPalette::WindowText,      QColor(Dark::TextPrimary));
        p.setColor(QPalette::Base,            QColor(Dark::BaseBg));
        p.setColor(QPalette::AlternateBase,   QColor(Dark::AltBg));
        p.setColor(QPalette::ToolTipBase,     QColor(Dark::BaseBg));
        p.setColor(QPalette::ToolTipText,     QColor(Dark::TextPrimary));
        p.setColor(QPalette::Text,            QColor(Dark::TextPrimary));
        p.setColor(QPalette::Button,          QColor(Dark::BaseBg));
        p.setColor(QPalette::ButtonText,      QColor(Dark::TextPrimary));
        p.setColor(QPalette::BrightText,      QColor(Dark::Error));
        p.setColor(QPalette::Link,            QColor(Dark::Accent));
        p.setColor(QPalette::Highlight,       QColor(Dark::Accent));
        p.setColor(QPalette::HighlightedText, QColor("#ffffff"));
        p.setColor(QPalette::PlaceholderText, QColor(Dark::TextTertiary));
        p.setColor(QPalette::Mid,             QColor(Dark::Border));
        p.setColor(QPalette::Midlight,        QColor(Dark::BorderLight));
        p.setColor(QPalette::Dark,            QColor("#555555"));
        p.setColor(QPalette::Shadow,          QColor("#111111"));

        // Disabled colors
        p.setColor(QPalette::Disabled, QPalette::WindowText, QColor(Dark::DisabledText));
        p.setColor(QPalette::Disabled, QPalette::Text,       QColor(Dark::DisabledText));
        p.setColor(QPalette::Disabled, QPalette::ButtonText, QColor(Dark::DisabledText));
        p.setColor(QPalette::Disabled, QPalette::Base,       QColor(Dark::DisabledBg));
        p.setColor(QPalette::Disabled, QPalette::Button,     QColor(Dark::DisabledBg));
    } else {
        p.setColor(QPalette::Window,          QColor(Light::WindowBg));
        p.setColor(QPalette::WindowText,      QColor(Light::TextPrimary));
        p.setColor(QPalette::Base,            QColor(Light::BaseBg));
        p.setColor(QPalette::AlternateBase,   QColor(Light::AltBg));
        p.setColor(QPalette::ToolTipBase,     QColor(Light::AltBg));
        p.setColor(QPalette::ToolTipText,     QColor(Light::TextPrimary));
        p.setColor(QPalette::Text,            QColor(Light::TextPrimary));
        p.setColor(QPalette::Button,          QColor(Light::BaseBg));
        p.setColor(QPalette::ButtonText,      QColor(Light::TextPrimary));
        p.setColor(QPalette::BrightText,      QColor(Light::Error));
        p.setColor(QPalette::Link,            QColor(Light::Accent));
        p.setColor(QPalette::Highlight,       QColor(Light::Accent));
        p.setColor(QPalette::HighlightedText, QColor("#ffffff"));
        p.setColor(QPalette::PlaceholderText, QColor(Light::TextTertiary));
        p.setColor(QPalette::Mid,             QColor(Light::Border));
        p.setColor(QPalette::Midlight,        QColor(Light::BorderLight));
        p.setColor(QPalette::Dark,            QColor("#bfbfbf"));
        p.setColor(QPalette::Shadow,          QColor("#aaaaaa"));

        // Disabled colors
        p.setColor(QPalette::Disabled, QPalette::WindowText, QColor(Light::DisabledText));
        p.setColor(QPalette::Disabled, QPalette::Text,       QColor(Light::DisabledText));
        p.setColor(QPalette::Disabled, QPalette::ButtonText, QColor(Light::DisabledText));
        p.setColor(QPalette::Disabled, QPalette::Base,       QColor(Light::DisabledBg));
        p.setColor(QPalette::Disabled, QPalette::Button,     QColor(Light::DisabledBg));
    }
    return p;
}

QString ThemeManager::stylesheet() const
{
    bool d = m_dark;
    QString wb  = d ? Dark::WindowBg    : Light::WindowBg;
    QString bb  = d ? Dark::BaseBg      : Light::BaseBg;
    QString ab  = d ? Dark::AltBg       : Light::AltBg;
    QString tp  = d ? Dark::TextPrimary : Light::TextPrimary;
    QString ts  = d ? Dark::TextSecondary : Light::TextSecondary;
    QString tt  = d ? Dark::TextTertiary : Light::TextTertiary;
    QString bd  = d ? Dark::Border      : Light::Border;
    QString bl  = d ? Dark::BorderLight : Light::BorderLight;
    QString dv  = d ? Dark::Divider     : Light::Divider;
    QString ac  = d ? Dark::Accent      : Light::Accent;
    QString ach = d ? Dark::AccentHover : Light::AccentHover;
    QString acp = d ? Dark::AccentPress : Light::AccentPress;
    QString er  = d ? Dark::Error       : Light::Error;
    QString db  = d ? Dark::DropBg      : Light::DropBg;
    QString dbr = d ? Dark::DropBorder  : Light::DropBorder;

    return QString(R"(
        /* ---- Global ---- */
        QMainWindow { background: %1; }
        QDialog    { background: %2; }
        QScrollArea { background: %2; border: none; }
        QStatusBar { background: %3; border-top: 1px solid %5; color: %6; }
        QToolBar   { background: %2; border-bottom: 1px solid %5; spacing: 6px; }
        QMenuBar   { background: %2; color: %4; }
        QMenuBar::item:selected { background: %1; }
        QMenu      { background: %2; color: %4; border: 1px solid %5; }
        QMenu::item:selected { background: %7; }

        /* ---- Text inputs ---- */
        QLineEdit, QTextEdit, QPlainTextEdit {
            padding: 6px 10px; border: 1px solid %5; border-radius: 4px;
            background: %2; color: %4; selection-background: %7;
        }
        QLineEdit:focus, QTextEdit:focus, QPlainTextEdit:focus {
            border-color: %7;
        }

        /* ---- Combo boxes ---- */
        QComboBox {
            padding: 6px 10px; border: 1px solid %5; border-radius: 4px;
            background: %2; color: %4;
        }
        QComboBox:focus { border-color: %7; }
        QComboBox QAbstractItemView {
            background: %2; color: %4; border: 1px solid %5;
            selection-background: %7; selection-color: white;
        }
        QComboBox::drop-down { border: none; width: 20px; }

        /* ---- List widgets ---- */
        QListWidget, QListView, QTreeView, QTableView {
            background: %2; color: %4; border: 1px solid %5;
            alternate-background-color: %3;
        }
        QListWidget::item:selected, QListView::item:selected,
        QTreeView::item:selected, QTableView::item:selected {
            background: %7; color: white;
        }

        /* ---- DateEdit ---- */
        QDateEdit, QDateTimeEdit, QSpinBox, QDoubleSpinBox {
            padding: 6px 10px; border: 1px solid %5; border-radius: 4px;
            background: %2; color: %4;
        }
        QDateEdit:focus, QDateTimeEdit:focus, QSpinBox:focus, QDoubleSpinBox:focus {
            border-color: %7;
        }

        /* ---- Checkboxes / Radio buttons ---- */
        QCheckBox, QRadioButton { color: %4; spacing: 6px; }
        QCheckBox::indicator, QRadioButton::indicator {
            width: 16px; height: 16px;
        }

        /* ---- Progress bar ---- */
        QProgressBar {
            border: 1px solid %5; border-radius: 4px; background: %1;
            text-align: center; color: %4;
        }
        QProgressBar::chunk { background: %7; border-radius: 3px; }

        /* ---- Scroll bars ---- */
        QScrollBar:vertical {
            background: %1; width: 8px; border: none;
        }
        QScrollBar::handle:vertical {
            background: %5; border-radius: 4px; min-height: 20px;
        }
        QScrollBar::handle:vertical:hover { background: %6; }
        QScrollBar::add-line:vertical, QScrollBar::sub-line:vertical { height: 0px; }
        QScrollBar:horizontal {
            background: %1; height: 8px; border: none;
        }
        QScrollBar::handle:horizontal {
            background: %5; border-radius: 4px; min-width: 20px;
        }
        QScrollBar::handle:horizontal:hover { background: %6; }
        QScrollBar::add-line:horizontal, QScrollBar::sub-line:horizontal { width: 0px; }

        /* ---- Tab widget ---- */
        QTabWidget::pane { border: 1px solid %5; background: %2; }
        QTabBar::tab {
            padding: 6px 16px; border: 1px solid %5; background: %1; color: %4;
        }
        QTabBar::tab:selected { background: %2; border-bottom-color: %2; }
        QTabBar::tab:hover { background: %3; }

        /* ---- GroupBox ---- */
        QGroupBox { color: %4; border: 1px solid %5; border-radius: 4px; margin-top: 8px; }
        QGroupBox::title { subcontrol-origin: margin; left: 10px; padding: 0 4px; }

        /* ---- Labels ---- */
        QLabel { color: %4; }

        /* ---- Splitter ---- */
        QSplitter::handle { background: %5; }

        /* ---- Tool tips ---- */
        QToolTip { background: %2; color: %4; border: 1px solid %5; }

        /* ---- Custom: primary button ---- */
        QPushButton[primary="true"] {
            background: %7; color: white; border: none; border-radius: 4px;
            font-weight: bold; padding: 8px 24px;
        }
        QPushButton[primary="true"]:hover  { background: %8; }
        QPushButton[primary="true"]:pressed { background: %9; }
        QPushButton[primary="true"]:disabled { background: %5; color: %6; }

        /* ---- Custom: secondary / outline button ---- */
        QPushButton[secondary="true"] {
            background: transparent; color: %4; border: 1px solid %5;
            border-radius: 4px; padding: 6px 16px;
        }
        QPushButton[secondary="true"]:hover { border-color: %7; color: %7; }

        /* ---- Custom: error label ---- */
        QLabel[error="true"] { color: %10; font-size: 12px; }

        /* ---- Custom: secondary text label ---- */
        QLabel[secondary="true"] { color: %11; font-size: 12px; }

        /* ---- Custom: divider frame ---- */
        QFrame[divider="true"] { color: %14; max-height: 1px; }

        /* ---- Custom: upload drop zone ---- */
        QLabel[dropzone="true"] {
            border: 2px dashed %15; border-radius: 8px; background: %16;
        }
        QLabel[dropzonehint="true"] { color: %11; font-size: 13px; }

        /* ---- Custom: section header ---- */
        QLabel[section="true"] {
            font-weight: bold; font-size: 13px; color: %4;
            border-bottom: 1px solid %13; padding-bottom: 4px;
        }

        /* ---- Custom: region picker popup ---- */
        QFrame[pickerpopup="true"] {
            background: %2; border: 1px solid %5; border-radius: 4px;
        }

        /* ---- Custom: captcha image ---- */
        QLabel[captcha="true"] {
            border: 1px solid %5; background: %3;
        }
    )")
    .arg(wb, bb, ab, tp, bl, ts, ac,
         ac, ach, acp, er, tt, bd, dv, db, dbr);
}
