#include "ClearableDateEdit.h"
#include <QCalendarWidget>
#include <QMouseEvent>
#include <QKeyEvent>
#include <QLineEdit>

ClearableDateEdit::ClearableDateEdit(QWidget *parent)
    : QDateEdit(parent)
{
    setCalendarPopup(false);
    setReadOnly(true);
    setButtonSymbols(QAbstractSpinBox::NoButtons);
    lineEdit()->setReadOnly(true);
}

void ClearableDateEdit::mousePressEvent(QMouseEvent *event)
{
    if (event->button() == Qt::LeftButton && !m_popupVisible) {
        m_popupVisible = true;
        QCalendarWidget *cal = new QCalendarWidget();
        cal->setWindowFlags(Qt::Popup);
        cal->setAttribute(Qt::WA_DeleteOnClose);
        cal->setSelectedDate(date().isValid() ? date() : QDate::currentDate());
        cal->setMaximumDate(maximumDate());
        cal->setMinimumDate(minimumDate());

        connect(cal, &QCalendarWidget::clicked, this, [this, cal](const QDate &d) {
            setDate(d);
            cal->close();
            emit dateChanged(d);
            emit editingFinished();
        });
        // Click away closes popup, date stays unchanged
        QPoint pos = mapToGlobal(QPoint(0, height()));
        cal->move(pos);
        cal->show();
        return;
    }
    QDateEdit::mousePressEvent(event);
}

void ClearableDateEdit::keyPressEvent(QKeyEvent *event)
{
    if (event->key() == Qt::Key_Delete || event->key() == Qt::Key_Backspace) {
        clear();
        emit dateChanged(QDate());
        emit editingFinished();
        return;
    }
    QDateEdit::keyPressEvent(event);
}
