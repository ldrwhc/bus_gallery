#ifndef PLATEASSISTANTBRIDGE_H
#define PLATEASSISTANTBRIDGE_H

#include <QString>

struct PlateRecognizeResult {
    bool ok = false;
    QString plate;
    double confidence = 0.0;
    QString source;
    QString error;
};

class PlateAssistantBridge {
public:
    /// Run HyperLPR3 OCR on an image, return recognized plate.
    static PlateRecognizeResult recognizeFromImage(const QString& imagePath, int timeoutMs = 45000);
};

#endif // PLATEASSISTANTBRIDGE_H
