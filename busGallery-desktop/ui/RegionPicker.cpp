#include "RegionPicker.h"
#include "utils/ThemeManager.h"
#include "utils/ChinaRegions.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QLabel>
#include <QMap>

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

// ---- License plate → region mapping --------------------------------------

// Province abbreviation → province full name
static const QMap<QString, QString> PLATE_PROVINCE = {
    {"京","北京市"}, {"津","天津市"}, {"沪","上海市"}, {"渝","重庆市"},
    {"冀","河北省"}, {"晋","山西省"}, {"蒙","内蒙古"}, {"辽","辽宁省"},
    {"吉","吉林省"}, {"黑","黑龙江省"}, {"苏","江苏省"}, {"浙","浙江省"},
    {"皖","安徽省"}, {"闽","福建省"}, {"赣","江西省"}, {"鲁","山东省"},
    {"豫","河南省"}, {"鄂","湖北省"}, {"湘","湖南省"}, {"粤","广东省"},
    {"桂","广西"}, {"琼","海南省"}, {"川","四川省"},
    {"贵","贵州省"}, {"黔","贵州省"}, {"云","云南省"}, {"滇","云南省"},
    {"藏","西藏"}, {"陕","陕西省"}, {"秦","陕西省"},
    {"甘","甘肃省"}, {"陇","甘肃省"}, {"青","青海省"},
    {"宁","宁夏"}, {"新","新疆"}
};

// 2-char plate prefix → {province, city}
// Province-first-char + city-letter → city name
static const QMap<QString, QPair<QString,QString>> PLATE_CITY = {
    // Beijing
    {"京A", {"北京市","北京市"}}, {"京B", {"北京市","北京市"}}, {"京C", {"北京市","北京市"}},
    {"京D", {"北京市","北京市"}}, {"京E", {"北京市","北京市"}}, {"京F", {"北京市","北京市"}},
    {"京G", {"北京市","北京市"}}, {"京H", {"北京市","北京市"}}, {"京J", {"北京市","北京市"}},
    {"京K", {"北京市","北京市"}}, {"京L", {"北京市","北京市"}}, {"京M", {"北京市","北京市"}},
    {"京N", {"北京市","北京市"}}, {"京P", {"北京市","北京市"}}, {"京Q", {"北京市","北京市"}},
    {"京R", {"北京市","北京市"}}, {"京S", {"北京市","北京市"}}, {"京T", {"北京市","北京市"}},
    {"京U", {"北京市","北京市"}}, {"京V", {"北京市","北京市"}}, {"京W", {"北京市","北京市"}},
    {"京Y", {"北京市","北京市"}},
    // Tianjin
    {"津A", {"天津市","天津市"}}, {"津B", {"天津市","天津市"}}, {"津C", {"天津市","天津市"}},
    {"津D", {"天津市","天津市"}}, {"津E", {"天津市","天津市"}}, {"津F", {"天津市","天津市"}},
    {"津G", {"天津市","天津市"}}, {"津H", {"天津市","天津市"}}, {"津J", {"天津市","天津市"}},
    {"津K", {"天津市","天津市"}}, {"津L", {"天津市","天津市"}}, {"津M", {"天津市","天津市"}},
    {"津N", {"天津市","天津市"}}, {"津Q", {"天津市","天津市"}}, {"津R", {"天津市","天津市"}},
    // Shanghai
    {"沪A", {"上海市","上海市"}}, {"沪B", {"上海市","上海市"}}, {"沪C", {"上海市","上海市"}},
    {"沪D", {"上海市","上海市"}}, {"沪E", {"上海市","上海市"}}, {"沪F", {"上海市","上海市"}},
    {"沪G", {"上海市","上海市"}}, {"沪H", {"上海市","上海市"}}, {"沪J", {"上海市","上海市"}},
    {"沪K", {"上海市","上海市"}}, {"沪L", {"上海市","上海市"}}, {"沪M", {"上海市","上海市"}},
    {"沪N", {"上海市","上海市"}}, {"沪R", {"上海市","上海市"}},
    // Chongqing
    {"渝A", {"重庆市","重庆市"}}, {"渝B", {"重庆市","重庆市"}}, {"渝C", {"重庆市","重庆市"}},
    {"渝D", {"重庆市","重庆市"}}, {"渝F", {"重庆市","重庆市"}}, {"渝G", {"重庆市","重庆市"}},
    {"渝H", {"重庆市","重庆市"}},
    // Hebei
    {"冀A", {"河北省","石家庄市"}}, {"冀B", {"河北省","唐山市"}}, {"冀C", {"河北省","秦皇岛市"}},
    {"冀D", {"河北省","邯郸市"}}, {"冀E", {"河北省","邢台市"}}, {"冀F", {"河北省","保定市"}},
    {"冀G", {"河北省","张家口市"}}, {"冀H", {"河北省","承德市"}}, {"冀J", {"河北省","沧州市"}},
    {"冀R", {"河北省","廊坊市"}}, {"冀T", {"河北省","衡水市"}},
    // Shanxi
    {"晋A", {"山西省","太原市"}}, {"晋B", {"山西省","大同市"}}, {"晋C", {"山西省","阳泉市"}},
    {"晋D", {"山西省","长治市"}}, {"晋E", {"山西省","晋城市"}}, {"晋F", {"山西省","朔州市"}},
    {"晋H", {"山西省","忻州市"}}, {"晋J", {"山西省","吕梁市"}}, {"晋K", {"山西省","晋中市"}},
    {"晋L", {"山西省","临汾市"}}, {"晋M", {"山西省","运城市"}},
    // Inner Mongolia
    {"蒙A", {"内蒙古","呼和浩特市"}}, {"蒙B", {"内蒙古","包头市"}}, {"蒙C", {"内蒙古","乌海市"}},
    {"蒙D", {"内蒙古","赤峰市"}}, {"蒙E", {"内蒙古","呼伦贝尔市"}}, {"蒙F", {"内蒙古","兴安盟"}},
    {"蒙G", {"内蒙古","通辽市"}}, {"蒙H", {"内蒙古","锡林郭勒盟"}}, {"蒙J", {"内蒙古","乌兰察布市"}},
    {"蒙K", {"内蒙古","鄂尔多斯市"}}, {"蒙L", {"内蒙古","巴彦淖尔市"}}, {"蒙M", {"内蒙古","阿拉善盟"}},
    // Liaoning
    {"辽A", {"辽宁省","沈阳市"}}, {"辽B", {"辽宁省","大连市"}}, {"辽C", {"辽宁省","鞍山市"}},
    {"辽D", {"辽宁省","抚顺市"}}, {"辽E", {"辽宁省","本溪市"}}, {"辽F", {"辽宁省","丹东市"}},
    {"辽G", {"辽宁省","锦州市"}}, {"辽H", {"辽宁省","营口市"}}, {"辽J", {"辽宁省","阜新市"}},
    {"辽K", {"辽宁省","辽阳市"}}, {"辽L", {"辽宁省","盘锦市"}}, {"辽M", {"辽宁省","铁岭市"}},
    {"辽N", {"辽宁省","朝阳市"}}, {"辽P", {"辽宁省","葫芦岛市"}},
    // Jilin
    {"吉A", {"吉林省","长春市"}}, {"吉B", {"吉林省","吉林市"}}, {"吉C", {"吉林省","四平市"}},
    {"吉D", {"吉林省","辽源市"}}, {"吉E", {"吉林省","通化市"}}, {"吉F", {"吉林省","白山市"}},
    {"吉G", {"吉林省","白城市"}}, {"吉H", {"吉林省","延边州"}}, {"吉J", {"吉林省","松原市"}},
    // Heilongjiang
    {"黑A", {"黑龙江省","哈尔滨市"}}, {"黑B", {"黑龙江省","齐齐哈尔市"}}, {"黑C", {"黑龙江省","牡丹江市"}},
    {"黑D", {"黑龙江省","佳木斯市"}}, {"黑E", {"黑龙江省","大庆市"}}, {"黑F", {"黑龙江省","伊春市"}},
    {"黑G", {"黑龙江省","鸡西市"}}, {"黑H", {"黑龙江省","鹤岗市"}}, {"黑J", {"黑龙江省","双鸭山市"}},
    {"黑K", {"黑龙江省","七台河市"}}, {"黑L", {"黑龙江省","绥化市"}}, {"黑M", {"黑龙江省","黑河市"}},
    {"黑N", {"黑龙江省","大兴安岭地区"}},
    // Jiangsu
    {"苏A", {"江苏省","南京市"}}, {"苏B", {"江苏省","无锡市"}}, {"苏C", {"江苏省","徐州市"}},
    {"苏D", {"江苏省","常州市"}}, {"苏E", {"江苏省","苏州市"}}, {"苏F", {"江苏省","南通市"}},
    {"苏G", {"江苏省","连云港市"}}, {"苏H", {"江苏省","淮安市"}}, {"苏J", {"江苏省","盐城市"}},
    {"苏K", {"江苏省","扬州市"}}, {"苏L", {"江苏省","镇江市"}}, {"苏M", {"江苏省","泰州市"}},
    {"苏N", {"江苏省","宿迁市"}},
    // Zhejiang
    {"浙A", {"浙江省","杭州市"}}, {"浙B", {"浙江省","宁波市"}}, {"浙C", {"浙江省","温州市"}},
    {"浙D", {"浙江省","绍兴市"}}, {"浙E", {"浙江省","湖州市"}}, {"浙F", {"浙江省","嘉兴市"}},
    {"浙G", {"浙江省","金华市"}}, {"浙H", {"浙江省","衢州市"}}, {"浙J", {"浙江省","台州市"}},
    {"浙K", {"浙江省","丽水市"}}, {"浙L", {"浙江省","舟山市"}},
    // Anhui
    {"皖A", {"安徽省","合肥市"}}, {"皖B", {"安徽省","芜湖市"}}, {"皖C", {"安徽省","蚌埠市"}},
    {"皖D", {"安徽省","淮南市"}}, {"皖E", {"安徽省","马鞍山市"}}, {"皖F", {"安徽省","淮北市"}},
    {"皖G", {"安徽省","铜陵市"}}, {"皖H", {"安徽省","安庆市"}}, {"皖J", {"安徽省","黄山市"}},
    {"皖K", {"安徽省","阜阳市"}}, {"皖L", {"安徽省","宿州市"}}, {"皖M", {"安徽省","滁州市"}},
    {"皖N", {"安徽省","六安市"}}, {"皖P", {"安徽省","宣城市"}}, {"皖R", {"安徽省","池州市"}},
    {"皖S", {"安徽省","亳州市"}},
    // Fujian
    {"闽A", {"福建省","福州市"}}, {"闽B", {"福建省","莆田市"}}, {"闽C", {"福建省","泉州市"}},
    {"闽D", {"福建省","厦门市"}}, {"闽E", {"福建省","漳州市"}}, {"闽F", {"福建省","龙岩市"}},
    {"闽G", {"福建省","三明市"}}, {"闽H", {"福建省","南平市"}}, {"闽J", {"福建省","宁德市"}},
    // Jiangxi
    {"赣A", {"江西省","南昌市"}}, {"赣B", {"江西省","赣州市"}}, {"赣C", {"江西省","宜春市"}},
    {"赣D", {"江西省","吉安市"}}, {"赣E", {"江西省","上饶市"}}, {"赣F", {"江西省","抚州市"}},
    {"赣G", {"江西省","九江市"}}, {"赣H", {"江西省","景德镇市"}}, {"赣J", {"江西省","萍乡市"}},
    {"赣K", {"江西省","新余市"}}, {"赣L", {"江西省","鹰潭市"}},
    // Shandong
    {"鲁A", {"山东省","济南市"}}, {"鲁B", {"山东省","青岛市"}}, {"鲁C", {"山东省","淄博市"}},
    {"鲁D", {"山东省","枣庄市"}}, {"鲁E", {"山东省","东营市"}}, {"鲁F", {"山东省","烟台市"}},
    {"鲁G", {"山东省","潍坊市"}}, {"鲁H", {"山东省","济宁市"}}, {"鲁J", {"山东省","泰安市"}},
    {"鲁K", {"山东省","威海市"}}, {"鲁L", {"山东省","日照市"}}, {"鲁M", {"山东省","滨州市"}},
    {"鲁N", {"山东省","德州市"}}, {"鲁P", {"山东省","聊城市"}}, {"鲁Q", {"山东省","临沂市"}},
    {"鲁R", {"山东省","菏泽市"}}, {"鲁S", {"山东省","莱芜市"}},
    // Henan
    {"豫A", {"河南省","郑州市"}}, {"豫B", {"河南省","开封市"}}, {"豫C", {"河南省","洛阳市"}},
    {"豫D", {"河南省","平顶山市"}}, {"豫E", {"河南省","安阳市"}}, {"豫F", {"河南省","鹤壁市"}},
    {"豫G", {"河南省","新乡市"}}, {"豫H", {"河南省","焦作市"}}, {"豫J", {"河南省","濮阳市"}},
    {"豫K", {"河南省","许昌市"}}, {"豫L", {"河南省","漯河市"}}, {"豫M", {"河南省","三门峡市"}},
    {"豫N", {"河南省","商丘市"}}, {"豫P", {"河南省","周口市"}}, {"豫Q", {"河南省","驻马店市"}},
    {"豫R", {"河南省","南阳市"}}, {"豫S", {"河南省","信阳市"}}, {"豫U", {"河南省","济源市"}},
    // Hubei
    {"鄂A", {"湖北省","武汉市"}}, {"鄂B", {"湖北省","黄石市"}}, {"鄂C", {"湖北省","十堰市"}},
    {"鄂D", {"湖北省","荆州市"}}, {"鄂E", {"湖北省","宜昌市"}}, {"鄂F", {"湖北省","襄阳市"}},
    {"鄂G", {"湖北省","鄂州市"}}, {"鄂H", {"湖北省","荆门市"}}, {"鄂J", {"湖北省","黄冈市"}},
    {"鄂K", {"湖北省","孝感市"}}, {"鄂L", {"湖北省","咸宁市"}}, {"鄂M", {"湖北省","仙桃市"}},
    {"鄂N", {"湖北省","潜江市"}}, {"鄂P", {"湖北省","神农架林区"}}, {"鄂Q", {"湖北省","恩施州"}},
    {"鄂R", {"湖北省","天门市"}}, {"鄂S", {"湖北省","随州市"}},
    // Hunan
    {"湘A", {"湖南省","长沙市"}}, {"湘B", {"湖南省","株洲市"}}, {"湘C", {"湖南省","湘潭市"}},
    {"湘D", {"湖南省","衡阳市"}}, {"湘E", {"湖南省","邵阳市"}}, {"湘F", {"湖南省","岳阳市"}},
    {"湘G", {"湖南省","张家界市"}}, {"湘H", {"湖南省","益阳市"}}, {"湘J", {"湖南省","常德市"}},
    {"湘K", {"湖南省","娄底市"}}, {"湘L", {"湖南省","郴州市"}}, {"湘M", {"湖南省","永州市"}},
    {"湘N", {"湖南省","怀化市"}}, {"湘U", {"湖南省","湘西州"}},
    // Guangdong
    {"粤A", {"广东省","广州市"}}, {"粤B", {"广东省","深圳市"}}, {"粤C", {"广东省","珠海市"}},
    {"粤D", {"广东省","汕头市"}}, {"粤E", {"广东省","佛山市"}}, {"粤F", {"广东省","韶关市"}},
    {"粤G", {"广东省","湛江市"}}, {"粤H", {"广东省","肇庆市"}}, {"粤J", {"广东省","江门市"}},
    {"粤K", {"广东省","茂名市"}}, {"粤L", {"广东省","惠州市"}}, {"粤M", {"广东省","梅州市"}},
    {"粤N", {"广东省","汕尾市"}}, {"粤P", {"广东省","河源市"}}, {"粤Q", {"广东省","阳江市"}},
    {"粤R", {"广东省","清远市"}}, {"粤S", {"广东省","东莞市"}}, {"粤T", {"广东省","中山市"}},
    {"粤U", {"广东省","潮州市"}}, {"粤V", {"广东省","揭阳市"}}, {"粤W", {"广东省","云浮市"}},
    // Guangxi
    {"桂A", {"广西","南宁市"}}, {"桂B", {"广西","柳州市"}}, {"桂C", {"广西","桂林市"}},
    {"桂D", {"广西","梧州市"}}, {"桂E", {"广西","北海市"}}, {"桂F", {"广西","崇左市"}},
    {"桂G", {"广西","来宾市"}}, {"桂H", {"广西","桂林市"}}, {"桂J", {"广西","贺州市"}},
    {"桂K", {"广西","玉林市"}}, {"桂L", {"广西","百色市"}}, {"桂M", {"广西","河池市"}},
    {"桂N", {"广西","钦州市"}}, {"桂P", {"广西","防城港市"}}, {"桂R", {"广西","贵港市"}},
    // Hainan
    {"琼A", {"海南省","海口市"}}, {"琼B", {"海南省","三亚市"}}, {"琼C", {"海南省","琼海市"}},
    {"琼D", {"海南省","五指山市"}}, {"琼E", {"海南省","洋浦经济开发区"}}, {"琼F", {"海南省","儋州市"}},
    // Sichuan
    {"川A", {"四川省","成都市"}}, {"川B", {"四川省","绵阳市"}}, {"川C", {"四川省","自贡市"}},
    {"川D", {"四川省","攀枝花市"}}, {"川E", {"四川省","泸州市"}}, {"川F", {"四川省","德阳市"}},
    {"川G", {"四川省","成都市"}}, {"川H", {"四川省","广元市"}}, {"川J", {"四川省","遂宁市"}},
    {"川K", {"四川省","内江市"}}, {"川L", {"四川省","乐山市"}}, {"川M", {"四川省","资阳市"}},
    {"川Q", {"四川省","宜宾市"}}, {"川R", {"四川省","南充市"}}, {"川S", {"四川省","达州市"}},
    {"川T", {"四川省","雅安市"}}, {"川U", {"四川省","阿坝州"}}, {"川V", {"四川省","甘孜州"}},
    {"川W", {"四川省","凉山州"}}, {"川X", {"四川省","广安市"}}, {"川Y", {"四川省","巴中市"}},
    {"川Z", {"四川省","眉山市"}},
    // Guizhou
    {"贵A", {"贵州省","贵阳市"}}, {"贵B", {"贵州省","六盘水市"}}, {"贵C", {"贵州省","遵义市"}},
    {"贵D", {"贵州省","铜仁市"}}, {"贵E", {"贵州省","黔西南州"}}, {"贵F", {"贵州省","毕节市"}},
    {"贵G", {"贵州省","安顺市"}}, {"贵H", {"贵州省","黔东南州"}}, {"贵J", {"贵州省","黔南州"}},
    // Yunnan
    {"云A", {"云南省","昆明市"}}, {"云C", {"云南省","昭通市"}}, {"云D", {"云南省","曲靖市"}},
    {"云E", {"云南省","楚雄州"}}, {"云F", {"云南省","玉溪市"}}, {"云G", {"云南省","红河州"}},
    {"云H", {"云南省","文山州"}}, {"云J", {"云南省","普洱市"}}, {"云K", {"云南省","西双版纳州"}},
    {"云L", {"云南省","大理州"}}, {"云M", {"云南省","保山市"}}, {"云N", {"云南省","德宏州"}},
    {"云P", {"云南省","丽江市"}}, {"云Q", {"云南省","怒江州"}}, {"云R", {"云南省","迪庆州"}},
    {"云S", {"云南省","临沧市"}},
    // Xizang
    {"藏A", {"西藏","拉萨市"}}, {"藏B", {"西藏","昌都市"}}, {"藏C", {"西藏","山南市"}},
    {"藏D", {"西藏","日喀则市"}}, {"藏E", {"西藏","那曲市"}}, {"藏F", {"西藏","阿里地区"}},
    {"藏G", {"西藏","林芝市"}},
    // Shaanxi
    {"陕A", {"陕西省","西安市"}}, {"陕B", {"陕西省","铜川市"}}, {"陕C", {"陕西省","宝鸡市"}},
    {"陕D", {"陕西省","咸阳市"}}, {"陕E", {"陕西省","渭南市"}}, {"陕F", {"陕西省","汉中市"}},
    {"陕G", {"陕西省","安康市"}}, {"陕H", {"陕西省","商洛市"}}, {"陕J", {"陕西省","延安市"}},
    {"陕K", {"陕西省","榆林市"}}, {"陕V", {"陕西省","杨凌示范区"}},
    // Gansu
    {"甘A", {"甘肃省","兰州市"}}, {"甘B", {"甘肃省","嘉峪关市"}}, {"甘C", {"甘肃省","金昌市"}},
    {"甘D", {"甘肃省","白银市"}}, {"甘E", {"甘肃省","天水市"}}, {"甘F", {"甘肃省","酒泉市"}},
    {"甘G", {"甘肃省","张掖市"}}, {"甘H", {"甘肃省","武威市"}}, {"甘J", {"甘肃省","定西市"}},
    {"甘K", {"甘肃省","陇南市"}}, {"甘L", {"甘肃省","平凉市"}}, {"甘M", {"甘肃省","庆阳市"}},
    {"甘N", {"甘肃省","临夏州"}}, {"甘P", {"甘肃省","甘南州"}},
    // Qinghai
    {"青A", {"青海省","西宁市"}}, {"青B", {"青海省","海东市"}}, {"青C", {"青海省","海北州"}},
    {"青D", {"青海省","黄南州"}}, {"青E", {"青海省","海南州"}}, {"青F", {"青海省","果洛州"}},
    {"青G", {"青海省","玉树州"}}, {"青H", {"青海省","海西州"}},
    // Ningxia
    {"宁A", {"宁夏","银川市"}}, {"宁B", {"宁夏","石嘴山市"}}, {"宁C", {"宁夏","吴忠市"}},
    {"宁D", {"宁夏","固原市"}}, {"宁E", {"宁夏","中卫市"}},
    // Xinjiang
    {"新A", {"新疆","乌鲁木齐市"}}, {"新B", {"新疆","昌吉州"}}, {"新C", {"新疆","石河子市"}},
    {"新D", {"新疆","奎屯市"}}, {"新E", {"新疆","博尔塔拉州"}}, {"新F", {"新疆","伊犁州"}},
    {"新G", {"新疆","塔城地区"}}, {"新H", {"新疆","阿勒泰地区"}}, {"新J", {"新疆","克拉玛依市"}},
    {"新K", {"新疆","吐鲁番市"}}, {"新L", {"新疆","哈密市"}}, {"新M", {"新疆","巴音郭楞州"}},
    {"新N", {"新疆","阿克苏地区"}}, {"新P", {"新疆","克孜勒苏州"}}, {"新Q", {"新疆","喀什地区"}},
    {"新R", {"新疆","和田地区"}}
};

bool RegionPicker::selectByPlateNumber(const QString &plate)
{
    if (plate.length() < 2 || m_regions.isEmpty())
        return false;

    // Extract first 2 characters (after removing any spaces/dots)
    QString clean = plate;
    clean.remove(QChar(0x00B7)); // middle dot
    clean = clean.simplified().remove(' ');
    if (clean.length() < 2) return false;

    QString prefix = clean.left(2);

    // Look up in the mapping
    auto it = PLATE_CITY.find(prefix);
    if (it == PLATE_CITY.end()) {
        // Try just province lookup — some plates like 军/警 aren't covered
        QString provAbbr = clean.left(1);
        auto pit = PLATE_PROVINCE.find(provAbbr);
        if (pit != PLATE_PROVINCE.end()) {
            // Only province known, try selectByCityName with province capital
            // (Province capital cities in PLATE_CITY are always the *A codes)
            QString capitalKey = provAbbr + "A";
            auto cit = PLATE_CITY.find(capitalKey);
            if (cit != PLATE_CITY.end()) {
                return selectByCityName(cit.value().second);
            }
        }
        return false;
    }

    const auto &info = it.value();
    return selectByCityName(info.second);
}
