#ifndef UPLOADAPI_H
#define UPLOADAPI_H

#include <QObject>
#include "ApiClient.h"
#include "models/VehiclePayload.h"

struct UploadResult {
    QString status;      // "APPROVED" or "PENDING"
    qint64 submissionId = 0;
};

class UploadApi : public QObject
{
    Q_OBJECT
public:
    explicit UploadApi(ApiClient *client, QObject *parent = nullptr);

    void uploadVehicle(const QString &filePath, const VehicleUpsertPayload &payload);

signals:
    void uploadSuccess(const UploadResult &result);
    void uploadError(const QString &code, const QString &message);

private:
    ApiClient *m_client;
};

#endif // UPLOADAPI_H
