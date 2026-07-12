#ifndef REGIONPICKER_H
#define REGIONPICKER_H

#include <QWidget>
#include <QPushButton>
#include <QListWidget>
#include <QFrame>
#include "models/CatalogItem.h"

class RegionPicker : public QWidget
{
    Q_OBJECT
public:
    explicit RegionPicker(QWidget *parent = nullptr);

    void setRegions(const QList<RegionNode> &regions);
    void setStaticRegions(); // built-in all-China data
    void clear();
    bool selectByCityName(const QString &cityName); // auto-select by city (handles 直辖市)

    qint64 regionId() const { return m_regionId; }
    QString provinceName() const { return m_province; }
    QString cityName() const { return m_city; }
    bool hasSelection() const { return !m_city.isEmpty(); }

signals:
    void regionSelected(qint64 id, const QString &province, const QString &city);

private:
    void showPopup();
    void onProvinceClicked(int row);
    void onCityClicked(int row);

    QPushButton *m_btn;
    QFrame *m_popup;
    QListWidget *m_provinceList;
    QListWidget *m_cityList;

    QList<RegionNode> m_regions;
    int m_selectedProvince = -1;
    qint64 m_regionId = 0;
    QString m_province;
    QString m_city;
};

#endif // REGIONPICKER_H
