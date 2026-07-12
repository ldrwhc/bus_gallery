#ifndef AUTOCOMPLETEFIELD_H
#define AUTOCOMPLETEFIELD_H

#include <QWidget>
#include <QLineEdit>
#include <QCompleter>
#include <QStringListModel>
#include <QLabel>
#include "models/CatalogItem.h"

class AutocompleteField : public QWidget
{
    Q_OBJECT
public:
    explicit AutocompleteField(const QString &placeholder, QWidget *parent = nullptr);

    void setItems(const QList<CatalogItem> &items);
    void setText(const QString &text);
    QString text() const;
    QString displayText() const;
    qint64 selectedId() const { return m_selectedId; }
    QString selectedExtra() const;  // extra field of the matched item
    QList<CatalogItem> items() const { return m_items; }
    bool setTextFuzzy(const QString &text); // try exact first, then fuzzy contains
    void clear();

signals:
    void valueChanged();

private:
    void onCompleterActivated(const QString &text);
    void evaluateMatch();

    QLineEdit *m_edit;
    QCompleter *m_completer;
    QStringListModel *m_model;
    QLabel *m_statusLabel;
    QList<CatalogItem> m_items;
    qint64 m_selectedId = 0;
    QMap<QString, qint64> m_nameToId;
};

#endif // AUTOCOMPLETEFIELD_H
