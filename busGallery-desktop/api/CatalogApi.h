#ifndef CATALOGAPI_H
#define CATALOGAPI_H

#include <QObject>
#include <QJsonArray>
#include "ApiClient.h"
#include "models/CatalogItem.h"
#include "models/VehiclePayload.h"

class CatalogApi : public QObject
{
    Q_OBJECT
public:
    explicit CatalogApi(ApiClient *client, QObject *parent = nullptr);

    void fetchRegions();
    void fetchBrands();
    void fetchModels();
    void fetchCompanies();
    void fetchModelVehicles(qint64 modelId);
    void fetchRoutes();

signals:
    void regionsReady(const QList<RegionNode> &regions);
    void brandsReady(const QList<CatalogItem> &brands);
    void modelsReady(const QList<CatalogItem> &models);
    void companiesReady(const QList<CatalogItem> &companies);
    void modelVehiclesReady(qint64 modelId, const QJsonArray &vehicles);
    void routesReady(const QList<CatalogItem> &routes);
    void routesDataReady(const QList<RouteInfo> &routes);
    void catalogError(const QString &endpoint, const QString &message);

private:
    ApiClient *m_client;
};

#endif // CATALOGAPI_H
