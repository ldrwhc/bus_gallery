#include "AutocompleteField.h"
#include "utils/ThemeManager.h"
#include <QVBoxLayout>
#include <QAbstractItemView>

AutocompleteField::AutocompleteField(const QString &placeholder, QWidget *parent)
    : QWidget(parent)
{
    auto *layout = new QVBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);
    layout->setSpacing(2);

    m_edit = new QLineEdit(this);
    m_edit->setPlaceholderText(placeholder);

    m_model = new QStringListModel(this);
    m_completer = new QCompleter(m_model, this);
    m_completer->setCaseSensitivity(Qt::CaseInsensitive);
    m_completer->setFilterMode(Qt::MatchContains);
    m_completer->setCompletionMode(QCompleter::PopupCompletion);
    m_completer->setMaxVisibleItems(10);
    m_edit->setCompleter(m_completer);

    m_statusLabel = new QLabel(this);
    m_statusLabel->setStyleSheet("font-size: 11px;");
    m_statusLabel->setProperty("secondary", true);

    layout->addWidget(m_edit);
    layout->addWidget(m_statusLabel);

    // Activated = user picked an item from the dropdown
    connect(m_completer, QOverload<const QString &>::of(&QCompleter::activated),
            this, &AutocompleteField::onCompleterActivated);

    // After editing stops (focus lost), evaluate match status
    connect(m_edit, &QLineEdit::editingFinished, this, [this]() {
        evaluateMatch();
    });

    // On each keystroke, just clear status silently — let the popup do the work
    connect(m_edit, &QLineEdit::textChanged, this, [this](const QString &text) {
        m_selectedId = 0;
        m_statusLabel->clear();
        if (text.isEmpty()) {
            emit valueChanged();
        }
    });
}

void AutocompleteField::setItems(const QList<CatalogItem> &items)
{
    m_items = items;
    QStringList names;
    m_nameToId.clear();
    for (const auto &item : items) {
        QString displayName = item.name;
        if (!item.extra.isEmpty())
            displayName = item.name + " (" + item.extra + ")";
        names << displayName;
        m_nameToId[displayName] = item.id;
        m_nameToId[item.name] = item.id;
    }
    m_model->setStringList(names);
}

void AutocompleteField::evaluateMatch()
{
    QString t = m_edit->text().trimmed();
    if (t.isEmpty()) {
        m_selectedId = 0;
        m_statusLabel->clear();
        emit valueChanged();
        return;
    }

    bool d = ThemeManager::instance().isDark();
    if (m_nameToId.contains(t)) {
        m_selectedId = m_nameToId[t];
        m_statusLabel->setText(QString::fromUtf8("已匹配: %1").arg(t));
        m_statusLabel->setStyleSheet(QString("color: %1; font-size: 11px;")
            .arg(d ? Dark::Success : Light::Success));
    } else {
        m_selectedId = 0;
        m_statusLabel->setText(QString::fromUtf8("系统暂无该项，提交后将自动创建"));
        m_statusLabel->setStyleSheet(QString("color: %1; font-size: 11px;")
            .arg(d ? Dark::Warning : Light::Warning));
    }
    emit valueChanged();
}

void AutocompleteField::onCompleterActivated(const QString &text)
{
    // Extract just the name from "Name (Extra)" format
    if (m_nameToId.contains(text)) {
        m_selectedId = m_nameToId[text];
    }
    // Evaluate match status after selection
    evaluateMatch();
}

QString AutocompleteField::text() const
{
    QString t = m_edit->text().trimmed();
    if (m_selectedId > 0 && t.contains(" (")) {
        int parenIdx = t.indexOf(" (");
        return t.left(parenIdx);
    }
    return t;
}

QString AutocompleteField::displayText() const
{
    return m_edit->text().trimmed();
}

void AutocompleteField::clear()
{
    m_edit->clear();
    m_selectedId = 0;
    m_statusLabel->clear();
}
