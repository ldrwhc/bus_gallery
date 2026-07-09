#ifndef VEHICLEPAYLOAD_H
#define VEHICLEPAYLOAD_H

#include <QString>
#include <QJsonObject>
#include <QJsonDocument>
#include <QDate>

struct VehicleConfigPayload {
    qint64 brandId = 0;
    qint64 modelId = 0;
    QString motor;
    QString engine;
    QString fuelType;
    QString stepType;
    QString transmissionSystem;
    QString suspension;
    QString axle;
    QString otherConfigs;

    QJsonObject toJson() const {
        QJsonObject obj;
        if (brandId > 0) obj["brandId"] = brandId;
        if (modelId > 0) obj["modelId"] = modelId;
        if (!motor.isEmpty()) obj["motor"] = motor;
        if (!engine.isEmpty()) obj["engine"] = engine;
        if (!fuelType.isEmpty()) obj["fuelType"] = fuelType;
        if (!stepType.isEmpty()) obj["stepType"] = stepType;
        if (!transmissionSystem.isEmpty()) obj["transmissionSystem"] = transmissionSystem;
        if (!suspension.isEmpty()) obj["suspension"] = suspension;
        if (!axle.isEmpty()) obj["axle"] = axle;
        if (!otherConfigs.isEmpty()) obj["otherConfigs"] = otherConfigs;
        return obj;
    }
};

struct VehicleUpsertPayload {
    QString plateNumber;       // required
    QString customNumber;
    qint64 brandId = 0;
    QString brandName;
    qint64 modelId = 0;
    QString modelName;         // required if modelId == 0
    qint64 companyId = 0;
    QString companyName;       // required if companyId == 0
    qint64 regionId = 0;
    QString regionProvince;
    QString regionCity;        // required if regionId == 0
    QString factoryDate;       // YYYY-MM-DD
    QString launchDate;
    bool airConditioned = true;
    QString source = "用户上传";
    QString remark;
    VehicleConfigPayload config;

    QString toJsonString() const {
        QJsonObject obj;
        obj["plateNumber"] = plateNumber;
        if (!customNumber.isEmpty()) obj["customNumber"] = customNumber;
        if (brandId > 0) obj["brandId"] = brandId;
        if (!brandName.isEmpty()) obj["brandName"] = brandName;
        if (modelId > 0) obj["modelId"] = modelId;
        if (!modelName.isEmpty()) obj["modelName"] = modelName;
        if (companyId > 0) obj["companyId"] = companyId;
        if (!companyName.isEmpty()) obj["companyName"] = companyName;
        if (regionId > 0) obj["regionId"] = regionId;
        if (!regionProvince.isEmpty()) obj["regionProvince"] = regionProvince;
        if (!regionCity.isEmpty()) obj["regionCity"] = regionCity;
        if (!factoryDate.isEmpty()) obj["factoryDate"] = factoryDate;
        if (!launchDate.isEmpty()) obj["launchDate"] = launchDate;
        obj["airConditioned"] = airConditioned;
        if (!source.isEmpty()) obj["source"] = source;
        if (!remark.isEmpty()) obj["remark"] = remark;

        QJsonObject cfg = config.toJson();
        if (!cfg.isEmpty()) obj["config"] = cfg;

        QJsonDocument doc(obj);
        return QString::fromUtf8(doc.toJson(QJsonDocument::Compact));
    }

    bool isValid() const {
        if (plateNumber.trimmed().isEmpty()) return false;
        if (modelId <= 0 && modelName.trimmed().isEmpty()) return false;
        if (companyId <= 0 && companyName.trimmed().isEmpty()) return false;
        if (regionId <= 0 && regionCity.trimmed().isEmpty()) return false;
        return true;
    }
};

#endif // VEHICLEPAYLOAD_H
