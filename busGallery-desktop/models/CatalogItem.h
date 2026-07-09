#ifndef CATALOGITEM_H
#define CATALOGITEM_H

#include <QString>
#include <QList>
#include <QJsonObject>
#include <QJsonArray>

struct CatalogItem {
    qint64 id = 0;
    QString name;
    QString extra; // brandName for model, regionName for company, chnName for brand

    static CatalogItem fromJson(const QJsonObject &obj) {
        CatalogItem item;
        item.id = obj["id"].toVariant().toLongLong();
        item.name = obj["name"].toString();
        if (obj.contains("brandName"))
            item.extra = obj["brandName"].toString();
        else if (obj.contains("chnName"))
            item.extra = obj["chnName"].toString();
        else if (obj.contains("regionName"))
            item.extra = obj["regionName"].toString();
        return item;
    }
};

struct RegionNode {
    qint64 id = 0;
    QString name;
    qint64 parentId = 0;
    qint64 provinceId = 0;
    int level = 0;
    QString regionType;
    QList<RegionNode> children;

    static RegionNode fromJson(const QJsonObject &obj) {
        RegionNode node;
        node.id = obj["id"].toVariant().toLongLong();
        node.name = obj["name"].toString();
        node.parentId = obj["parentId"].toVariant().toLongLong();
        node.provinceId = obj["provinceId"].toVariant().toLongLong();
        node.level = obj["level"].toInt();
        node.regionType = obj["regionType"].toString();
        if (obj.contains("children")) {
            for (const auto &child : obj["children"].toArray()) {
                node.children.append(fromJson(child.toObject()));
            }
        }
        return node;
    }
};

#endif // CATALOGITEM_H
