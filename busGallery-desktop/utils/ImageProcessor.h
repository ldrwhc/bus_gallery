#ifndef IMAGEPROCESSOR_H
#define IMAGEPROCESSOR_H

#include <QString>

namespace ImageProcessor {

// If the image exceeds server upload limits (24 MP or 8192×8192),
// scale it down proportionally, save as JPEG to a temp file, and
// return the temp path. Otherwise return the original path unchanged.
// Caller is responsible for deleting the temp file when done.
QString compressForUpload(const QString &filePath);

} // namespace ImageProcessor

#endif // IMAGEPROCESSOR_H
