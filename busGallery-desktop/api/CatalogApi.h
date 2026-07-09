#ifndef CATALOGAPI_H
#define CATALOGAPI_H

#include <QObject>
#include <QJsonArray>
#include "ApiClient.h"
#include "models/CatalogItem.h"

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

signals:
    void regionsReady(const QList<RegionNode> &regions);
    void brandsReady(const QList<CatalogItem> &brands);
    void modelsReady(const QList<CatalogItem> &models);
    void companiesReady(const QList<CatalogItem> &companies);
    void modelVehiclesReady(qint64 modelId, const QJsonArray &vehicles);
    void catalogError(const QString &endpoint, const QString &message);

private:
    ApiClient *m_client;
};

#endif // CATALOGAPI_H
