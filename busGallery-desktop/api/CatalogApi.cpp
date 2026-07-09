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
