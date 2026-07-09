#ifndef APICLIENT_H
#define APICLIENT_H

#include <QObject>
#include <QNetworkAccessManager>
#include <QNetworkReply>
#include <QNetworkRequest>
#include <QJsonDocument>
#include <QJsonObject>
#include <QJsonArray>
#include <QSettings>
#include <QHttpMultiPart>
#include <QUuid>
#include <functional>

class ApiClient : public QObject
{
    Q_OBJECT
public:
    explicit ApiClient(QObject *parent = nullptr);

    void setToken(const QString &token);
    QString token() const { return m_token; }
    void setVerifySsl(bool verify);

    using ObjCallback = std::function<void(int statusCode, const QJsonObject &json)>;
    using ArrCallback = std::function<void(int statusCode, const QJsonArray &arr)>;
    using ErrorCallback = std::function<void(int statusCode, const QString &code, const QString &message)>;

    void get(const QString &path, ObjCallback onSuccess, ErrorCallback onError = nullptr);
    void getArray(const QString &path, ArrCallback onSuccess, ErrorCallback onError = nullptr);
    void post(const QString &path, const QJsonObject &body, ObjCallback onSuccess, ErrorCallback onError = nullptr);
    void postMultipart(const QString &path, const QMap<QString, QString> &textParts,
                       const QString &filePath, const QString &fileFieldName,
                       ObjCallback onSuccess, ErrorCallback onError = nullptr);

signals:
    void authExpired();

private:
    void handleObjReply(QNetworkReply *reply, ObjCallback onSuccess, ErrorCallback onError);
    void handleArrReply(QNetworkReply *reply, ArrCallback onSuccess, ErrorCallback onError);
    QNetworkRequest buildRequest(const QString &path) const;

    QNetworkAccessManager *m_nam;
    QString m_token;
    bool m_verifySsl = false;
};

#endif // APICLIENT_H
