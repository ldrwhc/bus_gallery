#include "RegionPicker.h"
#include "utils/ThemeManager.h"
#include "utils/ChinaRegions.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QLabel>

RegionPicker::RegionPicker(QWidget *parent)
    : QWidget(parent)
    , m_popup(nullptr)
{
    auto *layout = new QHBoxLayout(this);
    layout->setContentsMargins(0, 0, 0, 0);

    m_btn = new QPushButton(QString::fromUtf8("请选择省份 / 城市"), this);
    m_btn->setProperty("secondary", true);
    m_btn->setStyleSheet("QPushButton { text-align: left; }");
    layout->addWidget(m_btn);

    connect(m_btn, &QPushButton::clicked, this, &RegionPicker::showPopup);
}

void RegionPicker::setRegions(const QList<RegionNode> &regions)
{
    m_regions = regions;
    QList<RegionNode> provinces;
    for (const auto &r : regions) {
        if (r.level == 1 || r.regionType == "PROVINCE")
            provinces.append(r);
    }
    if (!provinces.isEmpty())
        m_regions = provinces;
}

void RegionPicker::setStaticRegions()
{
    m_regions.clear();
    for (const auto &p : CHINA_REGIONS) {
        RegionNode node;
        node.id = 0; // static data has no server ID
        node.name = p.name;
        node.level = 1;
        node.regionType = "PROVINCE";
        for (const auto &cityName : p.cities) {
            RegionNode city;
            city.id = 0;
            city.name = cityName;
            city.level = 2;
            city.regionType = "CITY";
            node.children.append(city);
        }
        m_regions.append(node);
    }
}

void RegionPicker::clear()
{
    m_regionId = 0;
    m_province.clear();
    m_city.clear();
    m_selectedProvince = -1;
    m_btn->setText(QString::fromUtf8("请选择省份 / 城市"));
}

void RegionPicker::showPopup()
{
    if (m_popup) {
        m_popup->close();
        m_popup->deleteLater();
        m_popup = nullptr;
    }

    m_popup = new QFrame(this, Qt::Popup);
    m_popup->setProperty("pickerpopup", true);

    auto *popupLayout = new QHBoxLayout(m_popup);
    popupLayout->setContentsMargins(4, 4, 4, 4);

    // Province list
    auto *leftPanel = new QVBoxLayout();
    auto *provLabel = new QLabel(QString::fromUtf8("省份"), m_popup);
    provLabel->setStyleSheet("font-weight: bold; padding: 4px;");
    m_provinceList = new QListWidget(m_popup);
    m_provinceList->setFixedWidth(140);
    for (int i = 0; i < m_regions.size(); ++i) {
        m_provinceList->addItem(m_regions[i].name);
    }
    leftPanel->addWidget(provLabel);
    leftPanel->addWidget(m_provinceList);

    // City list
    auto *rightPanel = new QVBoxLayout();
    auto *cityLabel = new QLabel(QString::fromUtf8("城市"), m_popup);
    cityLabel->setStyleSheet("font-weight: bold; padding: 4px;");
    m_cityList = new QListWidget(m_popup);
    m_cityList->setFixedWidth(140);
    rightPanel->addWidget(cityLabel);
    rightPanel->addWidget(m_cityList);

    popupLayout->addLayout(leftPanel);
    popupLayout->addLayout(rightPanel);

    connect(m_provinceList, &QListWidget::currentRowChanged, this, &RegionPicker::onProvinceClicked);
    connect(m_cityList, &QListWidget::currentRowChanged, this, &RegionPicker::onCityClicked);

    QPoint pos = m_btn->mapToGlobal(QPoint(0, m_btn->height() + 2));
    m_popup->move(pos);
    m_popup->resize(310, 320);
    m_popup->show();
}

void RegionPicker::onProvinceClicked(int row)
{
    if (row < 0 || row >= m_regions.size()) return;
    m_selectedProvince = row;
    m_cityList->clear();
    for (const auto &city : m_regions[row].children) {
        m_cityList->addItem(city.name);
    }
}

void RegionPicker::onCityClicked(int row)
{
    if (m_selectedProvince < 0 || m_selectedProvince >= m_regions.size()) return;
    const auto &province = m_regions[m_selectedProvince];
    if (row < 0 || row >= province.children.size()) return;

    const auto &city = province.children[row];
    m_regionId = city.id;
    m_province = province.name;
    m_city = city.name;
    m_btn->setText(m_province + " / " + m_city);

    if (m_popup) {
        m_popup->close();
        m_popup->deleteLater();
        m_popup = nullptr;
    }
    emit regionSelected(m_regionId, m_province, m_city);
}

bool RegionPicker::selectByCityName(const QString &cityName)
{
    if (cityName.isEmpty() || m_regions.isEmpty())
        return false;

    // Normalize: strip "市" suffix for matching
    QString target = cityName;
    if (target.endsWith(QString::fromUtf8("市")))
        target.chop(1);

    // 直辖市: province name == city name
    static const QStringList municipalities = {"北京", "上海", "天津", "重庆"};

    for (int pi = 0; pi < m_regions.size(); ++pi) {
        const auto &province = m_regions[pi];
        QString provBase = province.name;
        if (provBase.endsWith(QString::fromUtf8("市")) || provBase.endsWith(QString::fromUtf8("省")))
            provBase.chop(1);

        for (int ci = 0; ci < province.children.size(); ++ci) {
            const auto &city = province.children[ci];
            QString cityBase = city.name;
            if (cityBase.endsWith(QString::fromUtf8("市")))
                cityBase.chop(1);

            if (cityBase == target || city.name == cityName) {
                // For 直辖市, province should equal city
                m_selectedProvince = pi;
                if (municipalities.contains(provBase)) {
                    m_province = city.name;
                    m_city = city.name;
                } else {
                    m_province = province.name;
                    m_city = city.name;
                }
                m_regionId = city.id;
                m_btn->setText(m_province + " / " + m_city);
                emit regionSelected(m_regionId, m_province, m_city);
                return true;
            }
        }
    }
    return false;
}
