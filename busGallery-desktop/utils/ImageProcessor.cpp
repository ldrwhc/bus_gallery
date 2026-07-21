#include "ImageProcessor.h"
#include "Config.h"
#include <QImage>
#include <QImageReader>
#include <QFileInfo>
#include <QDir>
#include <QDateTime>
#include <QtMath>

QString ImageProcessor::compressForUpload(const QString &filePath)
{
    // Quick dimension read without full decode
    QImageReader reader(filePath);
    reader.setAutoTransform(true);
    QSize size = reader.size();

    if (!size.isValid()) {
        // Fallback: try full load to get dimensions
        QImage img(filePath);
        if (img.isNull()) return filePath; // can't read at all, let server reject
        size = img.size();
    }

    const qint64 totalPixels = static_cast<qint64>(size.width()) * size.height();
    const bool overPixels  = totalPixels > Config::MAX_UPLOAD_PIXELS;
    const bool overWidth   = size.width()  > Config::MAX_UPLOAD_WIDTH;
    const bool overHeight  = size.height() > Config::MAX_UPLOAD_HEIGHT;

    // Already within all limits — no compression needed
    if (!overPixels && !overWidth && !overHeight)
        return filePath;

    // Load full image for scaling
    QImage img(filePath);
    if (img.isNull()) return filePath;

    // Compute uniform scale factor to satisfy all three limits
    qreal scale = 1.0;

    if (overPixels) {
        // scale = sqrt(maxPixels / currentPixels)
        scale = qMin(scale, qSqrt(static_cast<qreal>(Config::MAX_UPLOAD_PIXELS) / totalPixels));
    }
    if (size.width() > Config::MAX_UPLOAD_WIDTH)
        scale = qMin(scale, static_cast<qreal>(Config::MAX_UPLOAD_WIDTH) / size.width());
    if (size.height() > Config::MAX_UPLOAD_HEIGHT)
        scale = qMin(scale, static_cast<qreal>(Config::MAX_UPLOAD_HEIGHT) / size.height());

    QSize targetSize(
        qMax(1, qRound(size.width()  * scale)),
        qMax(1, qRound(size.height() * scale))
    );

    // Scale down with high-quality bicubic
    QImage scaled = img.scaled(targetSize, Qt::KeepAspectRatio, Qt::SmoothTransformation);

    // Save as JPEG to temp directory (always JPEG for consistent compression)
    QString tempPath = QDir::tempPath() + "/busgallery_upload_"
                       + QString::number(QDateTime::currentMSecsSinceEpoch()) + ".jpg";

    if (scaled.save(tempPath, "JPEG", Config::UPLOAD_JPEG_QUALITY))
        return tempPath;

    // If save failed, return original so server gives the normal error
    return filePath;
}
