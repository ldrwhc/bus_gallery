#include "ApiClient.h"
#include "utils/Config.h"
#include <QFile>
#include <QFileInfo>
#include <QMimeDatabase>

ApiClient::ApiClient(QObject *parent)
    : QObject(parent)
    , m_nam(new QNetworkAccessManager(this))
{
    QSettings settings;
    if (settings.value(Config::SETTING_TRUST_SSL, false).toBool()) {
        m_verifySsl = false;
    }
}

void ApiClient::setToken(const QString &token) { m_token = token; }

void ApiClient::setVerifySsl(bool verify)
{
    m_verifySsl = verify;
    QSettings settings;
    settings.setValue(Config::SETTING_TRUST_SSL, !verify);
}

QNetworkRequest ApiClient::buildRequest(const QString &path) const
{
    QUrl url(Config::API_BASE + path);
    QNetworkRequest req(url);
    req.setHeader(QNetworkRequest::ContentTypeHeader, "application/json;charset=utf-8");
    if (!m_token.isEmpty())
        req.setRawHeader("Authorization", ("Bearer " + m_token).toUtf8());
    // Ignore SSL errors for self-signed cert
    if (!m_verifySsl) {
        QSslConfiguration sslConf = req.sslConfiguration();
        sslConf.setPeerVerifyMode(QSslSocket::VerifyNone);
        req.setSslConfiguration(sslConf);
    }
    return req;
}

void ApiClient::get(const QString &path, ObjCallback onSuccess, ErrorCallback onError)
{
    QNetworkRequest req = buildRequest(path);
    req.setHeader(QNetworkRequest::ContentTypeHeader, QVariant());
    QNetworkReply *reply = m_nam->get(req);
    handleObjReply(reply, onSuccess, onError);
}

void ApiClient::getArray(const QString &path, ArrCallback onSuccess, ErrorCallback onError)
{
    QNetworkRequest req = buildRequest(path);
    req.setHeader(QNetworkRequest::ContentTypeHeader, QVariant());
    QNetworkReply *reply = m_nam->get(req);
    handleArrReply(reply, onSuccess, onError);
}

void ApiClient::post(const QString &path, const QJsonObject &body, ObjCallback onSuccess, ErrorCallback onError)
{
    QNetworkRequest req = buildRequest(path);
    QByteArray data = QJsonDocument(body).toJson(QJsonDocument::Compact);
    QNetworkReply *reply = m_nam->post(req, data);
    handleObjReply(reply, onSuccess, onError);
}

void ApiClient::postMultipart(const QString &path,
                               const QMap<QString, QString> &textParts,
                               const QString &filePath,
                               const QString &fileFieldName,
                               ObjCallback onSuccess, ErrorCallback onError)
{
    QUrl url(Config::API_BASE + path);
    QNetworkRequest req(url);
    if (!m_token.isEmpty())
        req.setRawHeader("Authorization", ("Bearer " + m_token).toUtf8());
    req.setRawHeader("Idempotency-Key", QUuid::createUuid().toString(QUuid::WithoutBraces).toUtf8());
    if (!m_verifySsl) {
        QSslConfiguration sslConf = req.sslConfiguration();
        sslConf.setPeerVerifyMode(QSslSocket::VerifyNone);
        req.setSslConfiguration(sslConf);
    }

    QHttpMultiPart *multiPart = new QHttpMultiPart(QHttpMultiPart::FormDataType);

    for (auto it = textParts.begin(); it != textParts.end(); ++it) {
        QHttpPart textPart;
        textPart.setHeader(QNetworkRequest::ContentDispositionHeader,
                           QString("form-data; name=\"%1\"").arg(it.key()));
        textPart.setBody(it.value().toUtf8());
        multiPart->append(textPart);
    }

    QFile *file = new QFile(filePath);
    if (!file->open(QIODevice::ReadOnly)) {
        delete multiPart;
        delete file;
        if (onError) onError(0, "A0400", "无法打开文件: " + filePath);
        return;
    }

    QMimeDatabase mimeDb;
    QString mimeType = mimeDb.mimeTypeForFile(filePath).name();

    QHttpPart filePart;
    filePart.setHeader(QNetworkRequest::ContentTypeHeader, mimeType);
    filePart.setHeader(QNetworkRequest::ContentDispositionHeader,
                       QString("form-data; name=\"%1\"; filename=\"%2\"")
                           .arg(fileFieldName, QFileInfo(filePath).fileName()));
    filePart.setBodyDevice(file);
    file->setParent(multiPart);
    multiPart->append(filePart);

    QNetworkReply *reply = m_nam->post(req, multiPart);
    multiPart->setParent(reply);
    handleObjReply(reply, onSuccess, onError);
}

void ApiClient::handleObjReply(QNetworkReply *reply, ObjCallback onSuccess, ErrorCallback onError)
{
    connect(reply, &QNetworkReply::finished, this, [this, reply, onSuccess, onError]() {
        reply->deleteLater();
        int statusCode = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        QByteArray body = reply->readAll();
        QJsonDocument doc = QJsonDocument::fromJson(body);
        QJsonObject json = doc.isObject() ? doc.object() : QJsonObject();

        if (statusCode == 401) {
            emit authExpired();
            if (onError) onError(statusCode, "A0401", "会话已过期，请重新登录");
            return;
        }

        if (statusCode >= 200 && statusCode < 300) {
            if (onSuccess) onSuccess(statusCode, json);
        } else {
            QString code = json.value("code").toString();
            QString message = json.value("message").toString();
            if (message.isEmpty()) message = reply->errorString();
            if (onError) onError(statusCode, code, message);
        }
    });
}

void ApiClient::handleArrReply(QNetworkReply *reply, ArrCallback onSuccess, ErrorCallback onError)
{
    connect(reply, &QNetworkReply::finished, this, [this, reply, onSuccess, onError]() {
        reply->deleteLater();
        int statusCode = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
        QByteArray body = reply->readAll();
        QJsonDocument doc = QJsonDocument::fromJson(body);

        if (statusCode == 401) {
            emit authExpired();
            if (onError) onError(statusCode, "A0401", "会话已过期，请重新登录");
            return;
        }

        if (statusCode >= 200 && statusCode < 300) {
            if (onSuccess && doc.isArray())
                onSuccess(statusCode, doc.array());
            else if (onSuccess)
                onSuccess(statusCode, QJsonArray());
        } else {
            QJsonObject json = doc.isObject() ? doc.object() : QJsonObject();
            QString code = json.value("code").toString();
            QString message = json.value("message").toString();
            if (message.isEmpty()) message = reply->errorString();
            if (onError) onError(statusCode, code, message);
        }
    });
}
