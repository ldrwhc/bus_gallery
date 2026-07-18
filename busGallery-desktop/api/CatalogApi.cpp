#include "CatalogApi.h"
#include "utils/Config.h"

CatalogApi::CatalogApi(ApiClient *client, QObject *parent)
    : QObject(parent), m_client(client) {}

void CatalogApi::fetchModelVehicles(qint64 modelId)
{
    m_client->getArray(Config::modelVehicles(modelId),
        [this, modelId](int, const QJsonArray &arr) {
            emit modelVehiclesReady(modelId, arr);
        },
        [this](int, const QString &, const QString &msg) {
            emit catalogError("modelVehicles", msg);
        });
}

void CatalogApi::fetchRegions()
{
    m_client->getArray(Config::CATALOG_REGIONS,
        [this](int, const QJsonArray &arr) {
            QList<RegionNode> regions;
            for (const auto &val : arr)
                regions.append(RegionNode::fromJson(val.toObject()));
            emit regionsReady(regions);
        },
        [this](int, const QString &, const QString &msg) {
            emit catalogError("regions", msg);
        });
}

void CatalogApi::fetchBrands()
{
    m_client->getArray(Config::CATALOG_BRANDS,
        [this](int, const QJsonArray &arr) {
            QList<CatalogItem> items;
            for (const auto &val : arr)
                items.append(CatalogItem::fromJson(val.toObject()));
            emit brandsReady(items);
        },
        [this](int, const QString &, const QString &msg) {
            emit catalogError("brands", msg);
        });
}

void CatalogApi::fetchModels()
{
    m_client->getArray(Config::CATALOG_MODELS,
        [this](int, const QJsonArray &arr) {
            QList<CatalogItem> items;
            for (const auto &val : arr)
                items.append(CatalogItem::fromJson(val.toObject()));
            emit modelsReady(items);
        },
        [this](int, const QString &, const QString &msg) {
            emit catalogError("models", msg);
        });
}

void CatalogApi::fetchCompanies()
{
    m_client->getArray(Config::CATALOG_COMPANIES,
        [this](int, const QJsonArray &arr) {
            QList<CatalogItem> items;
            for (const auto &val : arr)
                items.append(CatalogItem::fromJson(val.toObject()));
            emit companiesReady(items);
        },
        [this](int, const QString &, const QString &msg) {
            emit catalogError("companies", msg);
        });
}

void CatalogApi::fetchRoutes()
{
    // Pre-load active routes for autocomplete (up to 500)
    QString path = Config::ROUTES_SEARCH + "?size=500&isActive=true";
    m_client->get(path,
        [this](int, const QJsonObject &json) {
            QList<CatalogItem> items;
            QList<RouteInfo> routeData;
            QJsonArray records = json["records"].toArray();
            for (const auto &val : records) {
                QJsonObject obj = val.toObject();
                qint64 id = obj["id"].toVariant().toLongLong();

                // Autocomplete items
                CatalogItem item;
                item.id = id;
                item.name = obj["routeNumber"].toString();
                QString startStop = obj["startStop"].toString();
                QString endStop = obj["endStop"].toString();
                if (!startStop.isEmpty() || !endStop.isEmpty()) {
                    item.extra = startStop + " - " + endStop;
                }
                items.append(item);

                // Full route data for auto-fill
                RouteInfo ri;
                ri.id = id;
                ri.routeNumber = obj["routeNumber"].toString();
                ri.startStop = startStop;
                ri.endStop = endStop;
                ri.downStartStop = obj["downStartStop"].toString();
                ri.downEndStop = obj["downEndStop"].toString();
                ri.routeType = obj["routeType"].toString();
                ri.subType = obj["subType"].toString();
                ri.isLoop = obj["isLoop"].toBool(false);
                ri.isActive = obj["isActive"].toBool(true);
                ri.lineLengthKm = obj["lineLengthKm"].toVariant().toDouble();
                ri.ticketType = obj["ticketType"].toString();
                ri.ticketPrice = obj["ticketPrice"].toString();
                ri.operatingHours = obj["operatingHours"].toString();
                ri.firstOperated = obj["firstOperated"].toString();
                ri.lastOperated = obj["lastOperated"].toString();
                ri.remark = obj["remark"].toString();
                ri.parentRouteId = obj["parentRouteId"].toVariant().toLongLong();
                // Resolve nested region/company objects
                QJsonObject regionObj = obj["region"].toObject();
                if (!regionObj.isEmpty()) ri.regionId = regionObj["id"].toVariant().toLongLong();
                QJsonObject companyObj = obj["company"].toObject();
                if (!companyObj.isEmpty()) ri.companyId = companyObj["id"].toVariant().toLongLong();
                routeData.append(ri);
            }
            emit routesReady(items);
            emit routesDataReady(routeData);
        },
        [this](int, const QString &, const QString &msg) {
            emit catalogError("routes", msg);
        });
}
