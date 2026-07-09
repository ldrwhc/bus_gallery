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
    QString text() const;
    QString displayText() const;
    qint64 selectedId() const { return m_selectedId; }
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
