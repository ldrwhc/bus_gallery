#include "ImageDropZone.h"
#include "utils/Config.h"
#include "utils/ThemeManager.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QFileDialog>
#include <QFileInfo>
#include <QMimeData>
#include <QPainter>
#include <QMessageBox>

ImageDropZone::ImageDropZone(QWidget *parent)
    : QWidget(parent)
{
    setAcceptDrops(true);
    setMinimumHeight(200);
    setMaximumHeight(280);

    auto *layout = new QVBoxLayout(this);

    m_previewLabel = new QLabel(this);
    m_previewLabel->setAlignment(Qt::AlignCenter);
    m_previewLabel->setMinimumHeight(150);
    m_previewLabel->setProperty("dropzone", true);

    m_hintLabel = new QLabel(
        QString::fromUtf8("将图片拖拽到这里，或点击下方按钮选择\n仅支持 JPG / PNG，文件不超过 15 MB"),
        this);
    m_hintLabel->setAlignment(Qt::AlignCenter);
    m_hintLabel->setProperty("dropzonehint", true);

    auto *btnLayout = new QHBoxLayout();
    m_selectBtn = new QPushButton(QString::fromUtf8("选择图片"), this);
    m_selectBtn->setProperty("primary", true);
    m_clearBtn = new QPushButton(QString::fromUtf8("清除"), this);
    m_clearBtn->setProperty("secondary", true);
    m_clearBtn->setVisible(false);

    btnLayout->addStretch();
    btnLayout->addWidget(m_selectBtn);
    btnLayout->addWidget(m_clearBtn);
    btnLayout->addStretch();

    layout->addWidget(m_previewLabel);
    layout->addWidget(m_hintLabel);
    layout->addLayout(btnLayout);

    connect(m_selectBtn, &QPushButton::clicked, this, &ImageDropZone::selectImage);
    connect(m_clearBtn, &QPushButton::clicked, this, &ImageDropZone::clear);
}

void ImageDropZone::selectImage()
{
    QString filters = QString::fromUtf8("图片文件 (*.jpg *.jpeg *.png);;所有文件 (*.*)");
    QString filePath = QFileDialog::getOpenFileName(this, QString::fromUtf8("选择车辆图片"),
                                                     QString(), filters);
    if (!filePath.isEmpty()) {
        QFileInfo info(filePath);
        if (info.size() > Config::MAX_FILE_SIZE) {
            QMessageBox::warning(this, QString::fromUtf8("文件过大"),
                QString::fromUtf8("文件大小 %1 MB，超过限制 15 MB\n请压缩图片后重试")
                    .arg(info.size() / (1024.0 * 1024.0), 0, 'f', 1));
            return;
        }
        loadPreview(filePath);
    }
}

void ImageDropZone::loadPreview(const QString &filePath)
{
    QPixmap pixmap(filePath);
    if (pixmap.isNull()) {
        QMessageBox::warning(this, QString::fromUtf8("格式错误"),
                             QString::fromUtf8("无法加载图片，请确认文件格式正确"));
        return;
    }
    m_filePath = filePath;
    m_preview = pixmap.scaled(m_previewLabel->size(), Qt::KeepAspectRatio,
                               Qt::SmoothTransformation);
    m_previewLabel->setPixmap(m_preview);
    m_previewLabel->setProperty("dropzone", false);
    m_previewLabel->setStyleSheet(QString("QLabel { border: 2px solid %1; border-radius: 8px; }")
        .arg(ThemeManager::color(Light::Border, Dark::Border)));
    m_hintLabel->setText(QFileInfo(filePath).fileName());
    m_clearBtn->setVisible(true);
    emit imageSelected(filePath);
}

void ImageDropZone::clear()
{
    m_filePath.clear();
    m_preview = QPixmap();
    m_previewLabel->clear();
    m_previewLabel->setProperty("dropzone", true);
    m_previewLabel->setStyleSheet({});
    m_hintLabel->setText(
        QString::fromUtf8("将图片拖拽到这里，或点击下方按钮选择\n仅支持 JPG / PNG，文件不超过 15 MB"));
    m_clearBtn->setVisible(false);
    emit imageCleared();
}

void ImageDropZone::dragEnterEvent(QDragEnterEvent *event)
{
    if (event->mimeData()->hasUrls())
        event->acceptProposedAction();
}

void ImageDropZone::dragMoveEvent(QDragMoveEvent *event)
{
    event->acceptProposedAction();
}

void ImageDropZone::dropEvent(QDropEvent *event)
{
    const QMimeData *mimeData = event->mimeData();
    if (mimeData->hasUrls()) {
        for (const QUrl &url : mimeData->urls()) {
            if (url.isLocalFile()) {
                QString path = url.toLocalFile();
                QString ext = QFileInfo(path).suffix().toLower();
                if (Config::SUPPORTED_EXTENSIONS.contains(ext)) {
                    QFileInfo info(path);
                    if (info.size() > Config::MAX_FILE_SIZE) {
                        QMessageBox::warning(this, QString::fromUtf8("文件过大"),
                            QString::fromUtf8("文件超过 15 MB 限制，请压缩后重试"));
                        return;
                    }
                    loadPreview(path);
                    return;
                }
            }
        }
    }
}

void ImageDropZone::paintEvent(QPaintEvent *event)
{
    QWidget::paintEvent(event);
}
