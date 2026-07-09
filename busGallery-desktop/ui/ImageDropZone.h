#ifndef IMAGEDROPZONE_H
#define IMAGEDROPZONE_H

#include <QWidget>
#include <QLabel>
#include <QPushButton>
#include <QPixmap>
#include <QDragEnterEvent>
#include <QDropEvent>

class ImageDropZone : public QWidget
{
    Q_OBJECT
public:
    explicit ImageDropZone(QWidget *parent = nullptr);
    QString selectedFilePath() const { return m_filePath; }
    bool hasImage() const { return !m_filePath.isEmpty(); }
    void clear();

signals:
    void imageSelected(const QString &filePath);
    void imageCleared();

protected:
    void dragEnterEvent(QDragEnterEvent *event) override;
    void dragMoveEvent(QDragMoveEvent *event) override;
    void dropEvent(QDropEvent *event) override;
    void paintEvent(QPaintEvent *event) override;

private:
    void selectImage();
    void loadPreview(const QString &filePath);

    QLabel *m_previewLabel;
    QLabel *m_hintLabel;
    QPushButton *m_selectBtn;
    QPushButton *m_clearBtn;
    QString m_filePath;
    QPixmap m_preview;
};

#endif // IMAGEDROPZONE_H
