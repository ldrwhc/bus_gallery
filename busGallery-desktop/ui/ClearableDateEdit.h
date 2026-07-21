#ifndef CLEARABLEDATEEDIT_H
#define CLEARABLEDATEEDIT_H

#include <QDateEdit>

class ClearableDateEdit : public QDateEdit {
    Q_OBJECT

public:
    explicit ClearableDateEdit(QWidget *parent = nullptr);

protected:
    void mousePressEvent(QMouseEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;

private:
    bool m_popupVisible = false;
};

#endif // CLEARABLEDATEEDIT_H
