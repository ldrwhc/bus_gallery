#include "UploadApi.h"
#include "utils/Config.h"

UploadApi::UploadApi(ApiClient *client, QObject *parent)
    : QObject(parent), m_client(client)
{
}

void UploadApi::uploadVehicle(const QString &filePath, const VehicleUpsertPayload &payload)
{
    QMap<QString, QString> textParts;
    textParts["payload"] = payload.toJsonString();

    m_client->postMultipart(Config::UPLOAD_VEHICLE, textParts, filePath, "file",
        [this](int, const QJsonObject &json) {
            UploadResult result;
            result.status = json["status"].toString();
            result.submissionId = json["submissionId"].toVariant().toLongLong();
            emit uploadSuccess(result);
        },
        [this](int, const QString &code, const QString &message) {
            emit uploadError(code, message);
        }
    );
}
