#include "MainWindow.h"
#include "ui/ClearableDateEdit.h"
#include "ui/LoginDialog.h"
#include "utils/Config.h"
#include "utils/ThemeManager.h"
#include "utils/ChinaRegions.h"
#include "utils/ImageProcessor.h"
#include <QMenuBar>
#include <QStatusBar>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QFormLayout>
#include <QScrollArea>
#include <QMessageBox>
#include <QApplication>
#include <QFont>
#include <QFrame>
#include <QToolBar>
#include <QJsonDocument>
#include <QJsonObject>
#include <QFile>
#include <QDir>
#include <QDateTime>
#include <QTimer>
#include <QCoreApplication>
#include <QDataStream>
#include <QNetworkCookieJar>
#include <QSslConfiguration>
#include <QSslSocket>
#include <QInputDialog>
#include <QDesktopServices>
#include <QtConcurrent>
#include <QFutureWatcher>
#include "api/PlateAssistantBridge.h"

static QString draftPath();

MainWindow::MainWindow(ApiClient *client, QWidget *parent)
    : QMainWindow(parent)
    , m_client(client)
    , m_auth(new AuthApi(client, this))
    , m_catalog(new CatalogApi(client, this))
    , m_upload(new UploadApi(client, this))
{
    setWindowTitle(QString::fromUtf8("Bus Gallery - 车辆图片上传工具"));
    resize(800, 700);
    setMinimumSize(640, 560);

    setupUi();

    // Connect signals
    connect(m_client, &ApiClient::authExpired, this, &MainWindow::onSessionExpired);

    connect(m_auth, &AuthApi::loginSuccess, this, &MainWindow::onLoginSuccess);

    connect(m_catalog, &CatalogApi::brandsReady, [this](const QList<CatalogItem> &items) {
        m_brandField->setItems(items);
    });
    connect(m_catalog, &CatalogApi::modelsReady, [this](const QList<CatalogItem> &items) {
        m_modelField->setItems(items);
    });
    connect(m_catalog, &CatalogApi::companiesReady, [this](const QList<CatalogItem> &items) {
        m_companyField->setItems(items);
    });

    // Route autocomplete data
    connect(m_catalog, &CatalogApi::routesReady, [this](const QList<CatalogItem> &items) {
        m_routesList = items;
        refreshRouteFields();
    });
    // Full route data for auto-fill when a route is selected
    connect(m_catalog, &CatalogApi::routesDataReady, [this](const QList<RouteInfo> &routes) {
        m_routesData.clear();
        for (const auto &r : routes) {
            m_routesData[r.id] = r;
        }
        // Auto-fill any existing rows that already have a matched route
        for (auto &rw : m_routeRows) {
            qint64 routeId = rw.routeField->selectedId();
            if (routeId > 0 && m_routesData.contains(routeId)) {
                const RouteInfo &info = m_routesData[routeId];
                rw.startStopEdit->setText(info.startStop);
                rw.endStopEdit->setText(info.endStop);
            }
        }
    });

    // Model-to-config auto-fill removed: config varies by individual vehicle
    // (different companies may order the same model with different specs).
    // Use the "爬取参数" button to scrape per-vehicle data from buspedia.

    connect(m_upload, &UploadApi::uploadSuccess, this, [this](const UploadResult &result) {
        m_submitBtn->setEnabled(true);
        m_progress->setVisible(false);
        // Clean up compressed temp file if any
        if (!m_compressedFilePath.isEmpty()) {
            QFile::remove(m_compressedFilePath);
            m_compressedFilePath.clear();
        }
        // Clear draft on success
        QFile::remove(draftPath());
        QString msg;
        if (result.status == "APPROVED") {
            msg = QString::fromUtf8("上传成功！车辆已直接发布。");
        } else {
            msg = QString::fromUtf8("上传成功！\n资料已提交审核，审核编号: %1\n审核通过后将自动发布。")
                .arg(result.submissionId);
        }
        QMessageBox::information(this, result.status == "APPROVED"
            ? QString::fromUtf8("上传成功")
            : QString::fromUtf8("已提交审核"), msg);
        resetForm();
        m_progressLabel->setText(msg);

        // Refresh catalog so newly created brands/models/companies/routes appear immediately
        m_catalog->fetchBrands();
        m_catalog->fetchModels();
        m_catalog->fetchCompanies();
        m_catalog->fetchRoutes();
    });

    connect(m_upload, &UploadApi::uploadError, this, [this](const QString &code, const QString &msg) {
        m_submitBtn->setEnabled(true);
        m_progress->setVisible(false);
        // Clean up compressed temp file if any
        if (!m_compressedFilePath.isEmpty()) {
            QFile::remove(m_compressedFilePath);
            m_compressedFilePath.clear();
        }
        m_progressLabel->setProperty("error", true);
        m_progressLabel->setStyleSheet({});
        bool d = ThemeManager::instance().isDark();
        m_progressLabel->setStyleSheet(QString("color: %1;").arg(d ? Dark::Error : Light::Error));
        m_progressLabel->setText(QString::fromUtf8("上传失败: ") + msg);
        QMessageBox::warning(this, QString::fromUtf8("上传失败"), msg);
    });

    // Set up buspedia network manager with cookie jar & SSL
    m_buspediaNam = new QNetworkAccessManager(this);
    m_buspediaNam->setCookieJar(new QNetworkCookieJar(m_buspediaNam));
    m_buspediaNam->setCache(nullptr);  // Disable cache — always fetch fresh data
    // Allow self-signed certs for resilience
    QSslConfiguration sslConf = QSslConfiguration::defaultConfiguration();
    sslConf.setPeerVerifyMode(QSslSocket::VerifyNone);
    QSslConfiguration::setDefaultConfiguration(sslConf);

    // Try restore session
    QString token;
    UserProfile savedProfile;
    if (m_auth->restoreSession(token, savedProfile)) {
        m_profile = savedProfile;
        m_stack->setCurrentIndex(1);
        updateUserInfo();
        loadCatalog();
    }
}

void MainWindow::setupUi()
{
    // Menu
    auto *fileMenu = menuBar()->addMenu(QString::fromUtf8("文件"));
    fileMenu->addAction(QString::fromUtf8("登录"), this, &MainWindow::showLogin);
    fileMenu->addAction(QString::fromUtf8("退出登录"), this, &MainWindow::onLogout);
    fileMenu->addSeparator();
    fileMenu->addAction(QString::fromUtf8("退出程序"), qApp, &QApplication::quit);

    // Toolbar
    auto *toolbar = addToolBar(QString::fromUtf8("用户"));
    toolbar->setMovable(false);
    m_statusUser = new QLabel(QString::fromUtf8("未登录"));
    QFont boldFont = m_statusUser->font();
    boldFont.setBold(true);
    m_statusUser->setFont(boldFont);
    toolbar->addWidget(m_statusUser);
    toolbar->addSeparator();
    m_statusConn = new QLabel("");
    toolbar->addWidget(m_statusConn);

    // Central stacked widget
    m_stack = new QStackedWidget(this);
    setCentralWidget(m_stack);

    // Page 0: Login prompt
    m_loginPrompt = new QWidget(this);
    auto *promptLayout = new QVBoxLayout(m_loginPrompt);
    promptLayout->addStretch();
    auto *promptLabel = new QLabel(QString::fromUtf8("请先登录以使用上传功能"), m_loginPrompt);
    promptLabel->setAlignment(Qt::AlignCenter);
    promptLabel->setStyleSheet("font-size: 18px;");
    promptLabel->setProperty("secondary", true);
    auto *loginBtn = new QPushButton(QString::fromUtf8("前往登录"), m_loginPrompt);
    loginBtn->setProperty("primary", true);
    loginBtn->setFixedWidth(160);
    connect(loginBtn, &QPushButton::clicked, this, &MainWindow::showLogin);
    auto *btnContainer = new QHBoxLayout();
    btnContainer->addStretch();
    btnContainer->addWidget(loginBtn);
    btnContainer->addStretch();
    promptLayout->addWidget(promptLabel);
    promptLayout->addSpacing(10);
    promptLayout->addLayout(btnContainer);
    promptLayout->addStretch();
    m_stack->addWidget(m_loginPrompt);

    // Page 1: Upload form
    m_uploadPage = new QWidget(this);
    setupUploadForm(m_uploadPage);
    m_stack->addWidget(m_uploadPage);

    m_stack->setCurrentIndex(0);
    statusBar()->showMessage(QString::fromUtf8("就绪"));
}

void MainWindow::setupUploadForm(QWidget *page)
{
    auto *outerLayout = new QVBoxLayout(page);
    outerLayout->setContentsMargins(10, 10, 10, 10);

    auto *scrollArea = new QScrollArea(page);
    scrollArea->setWidgetResizable(true);
    scrollArea->setFrameShape(QFrame::NoFrame);

    auto *formWidget = new QWidget(scrollArea);
    auto *layout = new QVBoxLayout(formWidget);
    layout->setSpacing(16);

    // ---- Section: Image ----
    auto *sectionImage = new QLabel(QString::fromUtf8("车辆图片 *"));
    sectionImage->setProperty("section", true);
    layout->addWidget(sectionImage);

    m_imageDrop = new ImageDropZone(formWidget);
    layout->addWidget(m_imageDrop);

    // ---- Section: Basic Info ----
    auto *sectionBasic = new QLabel(QString::fromUtf8("基本信息"));
    sectionBasic->setProperty("section", true);
    layout->addWidget(sectionBasic);

    auto *basicGrid = new QFormLayout();
    basicGrid->setSpacing(8);

    auto *plateRow = new QHBoxLayout();
    m_plateEdit = new QLineEdit(formWidget);
    m_plateEdit->setPlaceholderText(QString::fromUtf8("请输入车牌号（必填）"));
    plateRow->addWidget(m_plateEdit, 1);
    m_buspediaBtn = new QPushButton(QString::fromUtf8("爬取参数"), formWidget);
    m_buspediaBtn->setFixedWidth(90);
    m_buspediaBtn->setToolTip(QString::fromUtf8("从 buspedia.top 爬取车辆参数并自动填充"));
    m_buspediaBtn->setProperty("accent", true);
    connect(m_buspediaBtn, &QPushButton::clicked, this, &MainWindow::fetchFromBuspedia);
    plateRow->addWidget(m_buspediaBtn);

    m_aiRecognizeBtn = new QPushButton(QString::fromUtf8("AI识别"), formWidget);
    m_aiRecognizeBtn->setFixedWidth(72);
    m_aiRecognizeBtn->setToolTip(QString::fromUtf8("AI 识别图片中的车牌号（需安装 HyperLPR）"));
    m_aiRecognizeBtn->setProperty("accent", true);
    connect(m_aiRecognizeBtn, &QPushButton::clicked, this, &MainWindow::onAIRecognize);
    plateRow->addWidget(m_aiRecognizeBtn);
    auto *plateLabel = new QLabel(QString::fromUtf8("车牌号 <span style='color:red'>*</span>"));
    plateLabel->setTextFormat(Qt::RichText);
    basicGrid->addRow(plateLabel, plateRow);

    m_customNumEdit = new QLineEdit(formWidget);
    m_customNumEdit->setPlaceholderText(QString::fromUtf8("请输入自编号（选填）"));
    basicGrid->addRow(QString::fromUtf8("自编号"), m_customNumEdit);

    layout->addLayout(basicGrid);

    // Brand, Model, Company
    auto *catalogGrid = new QFormLayout();
    catalogGrid->setSpacing(8);

    m_brandField = new AutocompleteField(QString::fromUtf8("搜索或输入品牌名称..."), formWidget);
    catalogGrid->addRow(QString::fromUtf8("品牌"), m_brandField);

    m_modelField = new AutocompleteField(QString::fromUtf8("搜索或输入车型名称...（必填）"), formWidget);
    auto *modelLabel = new QLabel(QString::fromUtf8("车型 <span style='color:red'>*</span>"));
    modelLabel->setTextFormat(Qt::RichText);
    catalogGrid->addRow(modelLabel, m_modelField);

    m_companyField = new AutocompleteField(QString::fromUtf8("搜索或输入公司名称..."), formWidget);
    catalogGrid->addRow(QString::fromUtf8("运营公司"), m_companyField);

    layout->addLayout(catalogGrid);

    // Region picker
    auto *regionLabel = new QLabel(QString::fromUtf8("地区 <span style='color:red'>*</span>"));
    regionLabel->setTextFormat(Qt::RichText);
    layout->addWidget(regionLabel);
    m_regionPicker = new RegionPicker(formWidget);
    layout->addWidget(m_regionPicker);

    // ---- Section: Details ----
    auto *sectionDetail = new QLabel(QString::fromUtf8("详细信息"));
    sectionDetail->setProperty("section", true);
    layout->addWidget(sectionDetail);

    auto *detailGrid = new QFormLayout();
    detailGrid->setSpacing(8);

    m_factoryDate = new ClearableDateEdit(formWidget);
    m_factoryDate->setDisplayFormat("yyyy-MM");
    m_factoryDate->setSpecialValueText(QString::fromUtf8("未知"));
    m_factoryDate->clear();
    m_factoryDate->setMinimumDate(QDate(1950, 1, 1));
    detailGrid->addRow(QString::fromUtf8("出厂日期"), m_factoryDate);

    m_launchDate = new ClearableDateEdit(formWidget);
    m_launchDate->setDisplayFormat("yyyy-MM");
    m_launchDate->setSpecialValueText(QString::fromUtf8("未知"));
    m_launchDate->clear();
    m_launchDate->setMinimumDate(QDate(1950, 1, 1));
    detailGrid->addRow(QString::fromUtf8("上线日期"), m_launchDate);

    m_airConditioned = new QCheckBox(QString::fromUtf8("有空调"), formWidget);
    m_airConditioned->setChecked(true);
    detailGrid->addRow(QString::fromUtf8("空调"), m_airConditioned);

    layout->addLayout(detailGrid);

    // Fuel type & config
    auto *configGrid = new QFormLayout();
    configGrid->setSpacing(8);

    m_fuelType = new QComboBox(formWidget);
    // Must match frontend/src/utils/fuel.js FUEL_OPTIONS exactly
    m_fuelType->addItems({QString(),
                          QString::fromUtf8("柴油"),
                          QString::fromUtf8("纯电"),
                          QString::fromUtf8("柴油+电"),
                          QString::fromUtf8("压缩天然气"),
                          QString::fromUtf8("压缩天然气+电"),
                          QString::fromUtf8("液化天然气"),
                          QString::fromUtf8("液化天然气+电"),
                          QString::fromUtf8("压缩氢气+电")});
    configGrid->addRow(QString::fromUtf8("燃料类型"), m_fuelType);

    m_engineEdit = new QLineEdit(formWidget);
    m_engineEdit->setPlaceholderText(QString::fromUtf8("发动机型号"));
    configGrid->addRow(QString::fromUtf8("发动机"), m_engineEdit);

    m_motorEdit = new QLineEdit(formWidget);
    m_motorEdit->setPlaceholderText(QString::fromUtf8("电机型号"));
    configGrid->addRow(QString::fromUtf8("电机"), m_motorEdit);

    m_transmissionEdit = new QLineEdit(formWidget);
    m_transmissionEdit->setPlaceholderText(QString::fromUtf8("变速系统"));
    configGrid->addRow(QString::fromUtf8("变速系统"), m_transmissionEdit);

    m_suspensionEdit = new QLineEdit(formWidget);
    m_suspensionEdit->setPlaceholderText(QString::fromUtf8("悬挂类型"));
    configGrid->addRow(QString::fromUtf8("悬挂"), m_suspensionEdit);

    m_axleEdit = new QLineEdit(formWidget);
    m_axleEdit->setPlaceholderText(QString::fromUtf8("车桥型号"));
    configGrid->addRow(QString::fromUtf8("车桥"), m_axleEdit);

    m_stepTypeEdit = new QLineEdit(formWidget);
    m_stepTypeEdit->setPlaceholderText(QString::fromUtf8("踏步类型"));
    configGrid->addRow(QString::fromUtf8("踏步"), m_stepTypeEdit);

    layout->addLayout(configGrid);

    connect(m_fuelType, QOverload<int>::of(&QComboBox::currentIndexChanged),
            this, &MainWindow::onFuelTypeChanged);

    // ---- Section: Associated Routes ----
    auto *sectionRoute = new QLabel(QString::fromUtf8("关联线路"));
    sectionRoute->setProperty("section", true);
    layout->addWidget(sectionRoute);

    m_routesContainer = new QWidget(formWidget);
    m_routesLayout = new QVBoxLayout(m_routesContainer);
    m_routesLayout->setContentsMargins(0, 0, 0, 0);
    m_routesLayout->setSpacing(6);
    layout->addWidget(m_routesContainer);

    m_addRouteBtn = new QPushButton(QString::fromUtf8("+ 添加关联线路"), formWidget);
    m_addRouteBtn->setProperty("secondary", true);
    m_addRouteBtn->setFixedWidth(160);
    connect(m_addRouteBtn, &QPushButton::clicked, this, [this]() { addRouteRow(); });
    layout->addWidget(m_addRouteBtn);

    // ---- Progress ----
    m_progress = new QProgressBar(formWidget);
    m_progress->setVisible(false);
    m_progress->setTextVisible(false);
    layout->addWidget(m_progress);

    m_progressLabel = new QLabel(formWidget);
    m_progressLabel->setWordWrap(true);
    layout->addWidget(m_progressLabel);

    // ---- Buttons ----
    auto *btnLayout = new QHBoxLayout();
    m_submitBtn = new QPushButton(QString::fromUtf8("提交上传"), formWidget);
    m_submitBtn->setMinimumHeight(40);
    m_submitBtn->setProperty("primary", true);
    connect(m_submitBtn, &QPushButton::clicked, this, &MainWindow::submitUpload);

    m_resetBtn = new QPushButton(QString::fromUtf8("重置表单"), formWidget);
    m_resetBtn->setMinimumHeight(40);
    m_resetBtn->setProperty("secondary", true);
    connect(m_resetBtn, &QPushButton::clicked, this, &MainWindow::resetForm);

    btnLayout->addStretch();
    btnLayout->addWidget(m_submitBtn);
    btnLayout->addWidget(m_resetBtn);
    btnLayout->addStretch();
    layout->addLayout(btnLayout);

    layout->addStretch();

    scrollArea->setWidget(formWidget);
    outerLayout->addWidget(scrollArea);

    // Setup draft auto-save (debounced: 1 second after any field change)
    m_draftTimer = new QTimer(this);
    m_draftTimer->setSingleShot(true);
    m_draftTimer->setInterval(1000);
    connect(m_draftTimer, &QTimer::timeout, this, [this]() {
        if (!m_suppressDraft) saveDraft();
    });

    // Connect all form fields to trigger auto-save
    auto hookLineEdit = [this](QLineEdit *le) {
        connect(le, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    };
    hookLineEdit(m_plateEdit);
    // Auto-fill region from plate prefix when plate changes
    // Always re-evaluate — plate prefix is more reliable than a stale draft selection
    connect(m_plateEdit, &QLineEdit::editingFinished, this, [this]() {
        QString plate = m_plateEdit->text().trimmed().toUpper();
        if (plate.length() >= 2)
            m_regionPicker->selectByPlateNumber(plate);
    });
    hookLineEdit(m_customNumEdit);
    hookLineEdit(m_engineEdit);
    hookLineEdit(m_motorEdit);
    hookLineEdit(m_transmissionEdit);
    hookLineEdit(m_suspensionEdit);
    hookLineEdit(m_axleEdit);
    hookLineEdit(m_stepTypeEdit);

    connect(m_brandField, &AutocompleteField::valueChanged, this, [this]() { m_draftTimer->start(); });
    connect(m_modelField, &AutocompleteField::valueChanged, this, [this]() { m_draftTimer->start(); });
    connect(m_companyField, &AutocompleteField::valueChanged, this, [this]() { m_draftTimer->start(); });
    connect(m_regionPicker, &RegionPicker::regionSelected, this, [this](qint64, const QString &, const QString &) {
        m_draftTimer->start();
    });
    connect(m_factoryDate, &QDateEdit::dateChanged, this, [this]() { m_draftTimer->start(); });
    connect(m_launchDate, &QDateEdit::dateChanged, this, [this]() { m_draftTimer->start(); });
    connect(m_airConditioned, &QCheckBox::toggled, this, [this]() { m_draftTimer->start(); });
    connect(m_fuelType, QOverload<int>::of(&QComboBox::currentIndexChanged),
            this, [this](int) { m_draftTimer->start(); });
}

void MainWindow::showLogin()
{
    LoginDialog dlg(m_client, this);
    if (dlg.exec() == QDialog::Accepted && dlg.loginSuccess()) {
        onLoginSuccess(LoginResult{dlg.token(), dlg.profile()});
    }
}

void MainWindow::onLoginSuccess(const LoginResult &result)
{
    m_profile = result.profile;
    m_stack->setCurrentIndex(1);
    updateUserInfo();
    loadCatalog();
    statusBar()->showMessage(QString::fromUtf8("已连接到 ") + Config::SERVER_BASE);

    // Restore draft after catalog data is loaded (short delay)
    QTimer::singleShot(800, this, [this]() {
        m_suppressDraft = true;
        loadDraft();
        m_suppressDraft = false;
    });
}

void MainWindow::onLogout()
{
    m_auth->logout();
    m_profile = UserProfile();
    m_stack->setCurrentIndex(0);
    m_statusUser->setText(QString::fromUtf8("未登录"));
    statusBar()->showMessage(QString::fromUtf8("已退出登录"));
}

void MainWindow::onSessionExpired()
{
    m_profile = UserProfile();
    m_stack->setCurrentIndex(0);
    m_statusUser->setText(QString::fromUtf8("未登录"));
    QMessageBox::warning(this, QString::fromUtf8("会话过期"),
                         QString::fromUtf8("登录已过期，请重新登录"));
}

void MainWindow::updateUserInfo()
{
    QString roleText;
    if (m_profile.role == "STATION") roleText = QString::fromUtf8("管理员");
    else if (m_profile.role == "REVIEWER") roleText = QString::fromUtf8("审核员");
    else roleText = QString::fromUtf8("用户");

    m_statusUser->setText(QString("%1 (%2)").arg(m_profile.displayName, roleText));
}

void MainWindow::loadCatalog()
{
    m_regionPicker->setStaticRegions();

    m_catalog->fetchBrands();
    m_catalog->fetchModels();
    m_catalog->fetchCompanies();
    m_catalog->fetchRoutes();  // pre-load routes for autocomplete
    // API regions are supplementary — static data already covers all of China.
    // The backend accepts regionProvince/regionCity strings directly in the
    // upload payload, so API region IDs are not required.
    m_statusConn->setText(QString::fromUtf8("加载目录数据中..."));
    connect(m_catalog, &CatalogApi::companiesReady, this, [this]() {
        m_statusConn->setText(QString::fromUtf8("就绪"));
    }, Qt::SingleShotConnection);
}

bool MainWindow::validateForm()
{
    QStringList errors;

    if (!m_imageDrop->hasImage())
        errors << QString::fromUtf8("请选择车辆图片");
    if (m_plateEdit->text().trimmed().isEmpty())
        errors << QString::fromUtf8("请输入车牌号");
    if (m_modelField->selectedId() <= 0 && m_modelField->text().isEmpty())
        errors << QString::fromUtf8("请选择或输入车型");
    if (m_companyField->selectedId() <= 0 && m_companyField->text().isEmpty())
        errors << QString::fromUtf8("请选择或输入运营公司");
    if (!m_regionPicker->hasSelection())
        errors << QString::fromUtf8("请选择地区（省份/城市）");

    if (!errors.isEmpty()) {
        QMessageBox::warning(this, QString::fromUtf8("表单不完整"), errors.join("\n"));
        return false;
    }
    return true;
}

void MainWindow::submitUpload()
{
    if (!validateForm()) return;

    VehicleUpsertPayload payload;
    payload.plateNumber = m_plateEdit->text().trimmed().toUpper();
    payload.customNumber = m_customNumEdit->text().trimmed();

    payload.brandId = m_brandField->selectedId();
    payload.brandName = m_brandField->text();

    payload.modelId = m_modelField->selectedId();
    payload.modelName = m_modelField->text();

    payload.companyId = m_companyField->selectedId();
    payload.companyName = m_companyField->text();

    payload.regionId = m_regionPicker->regionId();
    payload.regionProvince = m_regionPicker->provinceName();
    payload.regionCity = m_regionPicker->cityName();

    if (m_factoryDateExplicit && m_factoryDate->date().isValid())
        payload.factoryDate = m_factoryDate->date().toString("yyyy-MM") + "-01";
    if (m_launchDateExplicit && m_launchDate->date().isValid())
        payload.launchDate = m_launchDate->date().toString("yyyy-MM") + "-01";

    payload.airConditioned = m_airConditioned->isChecked();

    QString fuel = m_fuelType->currentText().trimmed();
    payload.config.fuelType = fuel;
    payload.config.engine = m_engineEdit->text().trimmed();
    payload.config.motor = m_motorEdit->text().trimmed();
    payload.config.transmissionSystem = m_transmissionEdit->text().trimmed();
    payload.config.suspension = m_suspensionEdit->text().trimmed();
    payload.config.axle = m_axleEdit->text().trimmed();
    payload.config.stepType = m_stepTypeEdit->text().trimmed();

    // Collect route assignments
    for (const auto &row : m_routeRows) {
        QString routeNumber = row.routeField->text();
        if (routeNumber.isEmpty()) continue;
        // Auto-append "路" if ends with digit (e.g. "468" -> "468路")
        if (!routeNumber.isEmpty() && routeNumber.at(routeNumber.size() - 1).isDigit())
            routeNumber += QString::fromUtf8("路");
        RouteAssignment ra;
        ra.routeId = row.routeField->selectedId();
        ra.routeNumber = routeNumber;
        ra.startStop = row.startStopEdit->text().trimmed();
        ra.endStop = row.endStopEdit->text().trimmed();
        ra.isCurrent = row.isCurrentCheck->isChecked();
        // Advanced fields
        ra.subType = row.subTypeCombo->currentData().toString();
        ra.routeType = row.routeTypeCombo->currentData().toString();
        ra.parentRouteId = row.parentRouteCombo->currentData().toLongLong();
        ra.downStartStop = row.downStartStopEdit->text().trimmed();
        ra.downEndStop = row.downEndStopEdit->text().trimmed();
        ra.isLoop = row.isLoopCheck->isChecked();
        ra.isActive = row.isActiveCheck->isChecked();
        bool ok;
        double len = row.lineLengthKmEdit->text().trimmed().toDouble(&ok);
        if (ok && len > 0) ra.lineLengthKm = len;
        ra.ticketType = row.ticketTypeEdit->text().trimmed();
        ra.ticketPrice = row.ticketPriceEdit->text().trimmed();
        ra.operatingHours = row.operatingHoursEdit->text().trimmed();
        ra.remark = row.remarkEdit->text().trimmed();
        if (row.firstOperatedEdit->date().isValid() && row.firstOperatedEdit->date().year() > 2000)
            ra.firstOperated = row.firstOperatedEdit->date().toString("yyyy-MM-dd");
        if (row.lastOperatedEdit->date().isValid() && row.lastOperatedEdit->date().year() > 2000)
            ra.lastOperated = row.lastOperatedEdit->date().toString("yyyy-MM-dd");
        payload.routes.append(ra);
    }

    m_submitBtn->setEnabled(false);
    m_progress->setVisible(true);
    m_progress->setRange(0, 0);
    m_progressLabel->setProperty("error", false);
    m_progressLabel->setStyleSheet(QString("color: %1;")
        .arg(ThemeManager::color(Light::Accent, Dark::Accent)));
    m_progressLabel->setText(QString::fromUtf8("正在上传图片和车辆信息..."));

    // Compress oversized images to meet server limits (24 MP / 8192×8192)
    QString uploadPath = ImageProcessor::compressForUpload(m_imageDrop->selectedFilePath());
    m_compressedFilePath = (uploadPath != m_imageDrop->selectedFilePath()) ? uploadPath : QString();

    m_upload->uploadVehicle(uploadPath, payload);
}

void MainWindow::resetForm()
{
    m_imageDrop->clear();
    m_plateEdit->clear();
    m_customNumEdit->clear();
    m_brandField->clear();
    m_modelField->clear();
    m_companyField->clear();
    m_regionPicker->clear();
    m_factoryDate->clear();
    m_launchDate->clear();
    m_factoryDateExplicit = false;
    m_launchDateExplicit = false;
    m_airConditioned->setChecked(true);
    m_fuelType->setCurrentIndex(0);
    m_engineEdit->clear();
    m_motorEdit->clear();
    m_transmissionEdit->clear();
    m_suspensionEdit->clear();
    m_axleEdit->clear();
    m_stepTypeEdit->clear();
    m_progress->setVisible(false);
    m_progressLabel->clear();

    // Clear route rows
    while (!m_routeRows.isEmpty()) {
        removeRouteRow(0);
    }

    // Delete draft
    QFile::remove(draftPath());
}

void MainWindow::onFuelTypeChanged(int index)
{
    Q_UNUSED(index);
    QString fuel = m_fuelType->currentText();
    bool showEngine = (fuel.contains(QString::fromUtf8("柴油"))
                    || fuel.contains(QString::fromUtf8("天然气")));
    bool showMotor = (fuel.contains(QString::fromUtf8("电"))
                   || fuel.contains(QString::fromUtf8("氢气")));

    m_engineEdit->setEnabled(showEngine);
    if (!showEngine) m_engineEdit->clear();
    m_motorEdit->setEnabled(showMotor);
    if (!showMotor) m_motorEdit->clear();
}

void MainWindow::addRouteRow(const RouteAssignment &ra)
{
    auto *row = new QWidget(m_routesContainer);
    row->setProperty("routeRow", true);
    row->setStyleSheet(QString(
        "QWidget[routeRow=\"true\"] {"
        "  background: %1; border: 1px solid %2; border-radius: 8px; padding: 8px;"
        "}"
    ).arg(ThemeManager::color(Light::AltBg, Dark::AltBg),
          ThemeManager::color(Light::Border, Dark::Border)));

    auto *outerLayout = new QVBoxLayout(row);
    outerLayout->setContentsMargins(0, 0, 0, 0);
    outerLayout->setSpacing(4);

    // Row 1: route number autocomplete + isCurrent + remove
    auto *row1 = new QHBoxLayout();
    row1->setSpacing(6);

    auto *routeField = new AutocompleteField(
        QString::fromUtf8("输入线路号搜索..."), row);
    routeField->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Preferred);
    if (!m_routesList.isEmpty()) {
        routeField->setItems(m_routesList);
    }
    if (!ra.routeNumber.isEmpty()) {
        auto *edit = routeField->findChild<QLineEdit*>();
        if (edit) edit->setText(ra.routeNumber);
    }
    row1->addWidget(routeField, 1);

    auto *isCurrentCheck = new QCheckBox(QString::fromUtf8("当前"), row);
    isCurrentCheck->setChecked(ra.isCurrent);
    row1->addWidget(isCurrentCheck);

    auto *removeBtn = new QPushButton(QString::fromUtf8("×"), row);
    removeBtn->setFixedWidth(30);
    removeBtn->setProperty("secondary", true);
    removeBtn->setToolTip(QString::fromUtf8("移除此线路"));
    connect(removeBtn, &QPushButton::clicked, this, [this, removeBtn]() {
        for (int i = 0; i < m_routeRows.size(); ++i) {
            if (m_routeRows[i].removeBtn == removeBtn) {
                removeRouteRow(i);
                return;
            }
        }
    });
    row1->addWidget(removeBtn);

    row1->addWidget(removeBtn);
    outerLayout->addLayout(row1);

    // Row 2: start stop — end stop
    auto *row3 = new QHBoxLayout();
    row3->setSpacing(8);
    auto *startStopEdit = new QLineEdit(row);
    startStopEdit->setPlaceholderText(QString::fromUtf8("起点站"));
    if (!ra.startStop.isEmpty()) startStopEdit->setText(ra.startStop);
    row3->addWidget(startStopEdit);
    auto *sepLabel = new QLabel(QString::fromUtf8("—"), row);
    sepLabel->setFixedWidth(20);
    sepLabel->setAlignment(Qt::AlignCenter);
    sepLabel->setStyleSheet("color: #94a3b8; font-weight: bold; border: none; background: transparent;");
    row3->addWidget(sepLabel);
    auto *endStopEdit = new QLineEdit(row);
    endStopEdit->setPlaceholderText(QString::fromUtf8("终点站"));
    if (!ra.endStop.isEmpty()) endStopEdit->setText(ra.endStop);
    row3->addWidget(endStopEdit);
    outerLayout->addLayout(row3);

    // ===== Advanced Settings Toggle =====
    auto *advToggle = new QPushButton(QString::fromUtf8("▶ 高级设置"), row);
    advToggle->setFlat(true);
    advToggle->setCursor(Qt::PointingHandCursor);
    advToggle->setStyleSheet(
        "QPushButton { border: none; background: transparent; color: #6366f1; font-size: 12px; font-weight: 500; padding: 2px 0; text-align: left; }"
        "QPushButton:hover { color: #4f46e5; }");
    advToggle->setSizePolicy(QSizePolicy::Fixed, QSizePolicy::Preferred);
    outerLayout->addWidget(advToggle);

    // ===== Advanced Settings Panel (hidden by default) =====
    auto *advPanel = new QWidget(row);
    advPanel->setVisible(false);
    auto *advLayout = new QVBoxLayout(advPanel);
    advLayout->setContentsMargins(0, 4, 0, 0);
    advLayout->setSpacing(6);

    // Row A1: subType | routeType | isLoop | isActive
    auto *advRow1 = new QHBoxLayout();
    advRow1->setSpacing(8);

    auto *subTypeLabel = new QLabel(QString::fromUtf8("形式"), advPanel);
    subTypeLabel->setStyleSheet("font-size:11px; color:#6b7280; border:none;background:transparent;");
    advRow1->addWidget(subTypeLabel);
    auto *subTypeCombo = new QComboBox(advPanel);
    subTypeCombo->addItem(QString::fromUtf8("主线"), QString(""));
    subTypeCombo->addItem(QString::fromUtf8("区间"), QString("INTERVAL"));
    subTypeCombo->addItem(QString::fromUtf8("支线"), QString("BRANCH"));
    subTypeCombo->addItem(QString::fromUtf8("快线"), QString("EXPRESS"));
    subTypeCombo->addItem(QString::fromUtf8("夜班"), QString("NIGHT"));
    subTypeCombo->addItem(QString::fromUtf8("直达"), QString("DIRECT"));
    subTypeCombo->setCurrentIndex(0);
    subTypeCombo->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Preferred);
    advRow1->addWidget(subTypeCombo);

    auto *routeTypeLabel = new QLabel(QString::fromUtf8("类型"), advPanel);
    routeTypeLabel->setStyleSheet("font-size:11px; color:#6b7280; border:none;background:transparent;");
    advRow1->addWidget(routeTypeLabel);
    auto *routeTypeCombo = new QComboBox(advPanel);
    routeTypeCombo->addItem(QString::fromUtf8("常规"), QString("REGULAR"));
    routeTypeCombo->addItem(QString::fromUtf8("BRT"), QString("BRT"));
    routeTypeCombo->addItem(QString::fromUtf8("机场"), QString("AIRPORT"));
    routeTypeCombo->addItem(QString::fromUtf8("旅游"), QString("TOURIST"));
    routeTypeCombo->addItem(QString::fromUtf8("微循环"), QString("COMMUNITY"));
    routeTypeCombo->addItem(QString::fromUtf8("地铁接驳"), QString("SUBWAY"));
    routeTypeCombo->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Preferred);
    advRow1->addWidget(routeTypeCombo);

    auto *isLoopCheck = new QCheckBox(QString::fromUtf8("环线"), advPanel);
    isLoopCheck->setChecked(ra.isLoop);
    advRow1->addWidget(isLoopCheck);

    auto *isActiveCheck = new QCheckBox(QString::fromUtf8("运营中"), advPanel);
    isActiveCheck->setChecked(ra.isActive);
    advRow1->addWidget(isActiveCheck);

    advLayout->addLayout(advRow1);

    // Row A2: parentRoute | downStartStop | downEndStop
    auto *advRow2 = new QHBoxLayout();
    advRow2->setSpacing(8);

    auto *parentLabel = new QLabel(QString::fromUtf8("父线路"), advPanel);
    parentLabel->setStyleSheet("font-size:11px; color:#6b7280; border:none;background:transparent;");
    advRow2->addWidget(parentLabel);
    auto *parentRouteCombo = new QComboBox(advPanel);
    parentRouteCombo->setEditable(true);
    parentRouteCombo->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Preferred);
    advRow2->addWidget(parentRouteCombo);

    auto *downStartEdit = new QLineEdit(advPanel);
    downStartEdit->setPlaceholderText(QString::fromUtf8("下行起点"));
    if (!ra.downStartStop.isEmpty()) downStartEdit->setText(ra.downStartStop);
    advRow2->addWidget(downStartEdit);

    auto *downEndEdit = new QLineEdit(advPanel);
    downEndEdit->setPlaceholderText(QString::fromUtf8("下行终点"));
    if (!ra.downEndStop.isEmpty()) downEndEdit->setText(ra.downEndStop);
    advRow2->addWidget(downEndEdit);

    advLayout->addLayout(advRow2);

    // Row A3: lineLengthKm | ticketType | ticketPrice
    auto *advRow3 = new QHBoxLayout();
    advRow3->setSpacing(8);

    auto *lenEdit = new QLineEdit(advPanel);
    lenEdit->setPlaceholderText(QString::fromUtf8("线路长度(km)"));
    if (ra.lineLengthKm > 0) lenEdit->setText(QString::number(ra.lineLengthKm, 'f', 1));
    advRow3->addWidget(lenEdit);

    auto *ticketTypeEdit = new QLineEdit(advPanel);
    ticketTypeEdit->setPlaceholderText(QString::fromUtf8("票制"));
    if (!ra.ticketType.isEmpty()) ticketTypeEdit->setText(ra.ticketType);
    advRow3->addWidget(ticketTypeEdit);

    auto *ticketPriceEdit = new QLineEdit(advPanel);
    ticketPriceEdit->setPlaceholderText(QString::fromUtf8("票价"));
    if (!ra.ticketPrice.isEmpty()) ticketPriceEdit->setText(ra.ticketPrice);
    advRow3->addWidget(ticketPriceEdit);

    advLayout->addLayout(advRow3);

    // Row A4: operatingHours | firstOperated | lastOperated
    auto *advRow4 = new QHBoxLayout();
    advRow4->setSpacing(8);

    auto *hoursEdit = new QLineEdit(advPanel);
    hoursEdit->setPlaceholderText(QString::fromUtf8("运营时间"));
    if (!ra.operatingHours.isEmpty()) hoursEdit->setText(ra.operatingHours);
    advRow4->addWidget(hoursEdit);

    auto *firstOperEdit = new QDateEdit(advPanel);
    firstOperEdit->setCalendarPopup(true);
    firstOperEdit->setDisplayFormat("yyyy-MM-dd");
    firstOperEdit->setSpecialValueText(QString::fromUtf8("开通日期"));
    firstOperEdit->setDate(QDate(2000, 1, 1));
    firstOperEdit->clear();
    if (!ra.firstOperated.isEmpty()) firstOperEdit->setDate(QDate::fromString(ra.firstOperated, "yyyy-MM-dd"));
    advRow4->addWidget(firstOperEdit);

    auto *lastOperEdit = new QDateEdit(advPanel);
    lastOperEdit->setCalendarPopup(true);
    lastOperEdit->setDisplayFormat("yyyy-MM-dd");
    lastOperEdit->setSpecialValueText(QString::fromUtf8("停运日期"));
    lastOperEdit->setDate(QDate(2000, 1, 1));
    lastOperEdit->clear();
    if (!ra.lastOperated.isEmpty()) lastOperEdit->setDate(QDate::fromString(ra.lastOperated, "yyyy-MM-dd"));
    advRow4->addWidget(lastOperEdit);

    advLayout->addLayout(advRow4);

    // Row A5: remark
    auto *remarkEdit = new QLineEdit(advPanel);
    remarkEdit->setPlaceholderText(QString::fromUtf8("备注"));
    if (!ra.remark.isEmpty()) remarkEdit->setText(ra.remark);
    advLayout->addWidget(remarkEdit);

    outerLayout->addWidget(advPanel);

    // Expand/collapse toggle
    connect(advToggle, &QPushButton::clicked, this, [advPanel, advToggle]() {
        bool visible = !advPanel->isVisible();
        advPanel->setVisible(visible);
        advToggle->setText(visible
            ? QString::fromUtf8("▼ 高级设置")
            : QString::fromUtf8("▶ 高级设置"));
    });

    m_routesLayout->addWidget(row);

    RouteRowWidgets rw;
    rw.container = row;
    rw.routeField = routeField;
    rw.startStopEdit = startStopEdit;
    rw.endStopEdit = endStopEdit;
    rw.isCurrentCheck = isCurrentCheck;
    rw.removeBtn = removeBtn;
    rw.advancedToggle = advToggle;
    rw.advancedPanel = advPanel;
    rw.subTypeCombo = subTypeCombo;
    rw.routeTypeCombo = routeTypeCombo;
    rw.parentRouteCombo = parentRouteCombo;
    rw.downStartStopEdit = downStartEdit;
    rw.downEndStopEdit = downEndEdit;
    rw.isLoopCheck = isLoopCheck;
    rw.isActiveCheck = isActiveCheck;
    rw.lineLengthKmEdit = lenEdit;
    rw.ticketTypeEdit = ticketTypeEdit;
    rw.ticketPriceEdit = ticketPriceEdit;
    rw.operatingHoursEdit = hoursEdit;
    rw.remarkEdit = remarkEdit;
    rw.firstOperatedEdit = firstOperEdit;

    // Fetch route stops button
    auto *fetchBtn = new QPushButton(QString::fromUtf8("🔍"), row);
    fetchBtn->setFixedWidth(32);
    fetchBtn->setToolTip(QString::fromUtf8("抓取线路起讫点"));
    fetchBtn->setStyleSheet("QPushButton { border: 1px solid #cbd5e1; background: #fff; border-radius: 6px; font-size: 13px; } QPushButton:hover { background: #e2e8f0; }");
    connect(fetchBtn, &QPushButton::clicked, this, [this, routeField, startStopEdit, endStopEdit, fetchBtn]() {
        fetchRouteStops(routeField, startStopEdit, endStopEdit, fetchBtn);
    });
    row1->insertWidget(2, fetchBtn);
    rw.fetchRouteBtn = fetchBtn;
    rw.lastOperatedEdit = lastOperEdit;
    m_routeRows.append(rw);

    // Populate parent route combo from routes list
    parentRouteCombo->addItem(QString::fromUtf8("(无)"), QVariant(0));
    for (const auto &ri : m_routesData) {
        parentRouteCombo->addItem(ri.routeNumber, QVariant(ri.id));
    }

    // Auto-fill when a route is selected from the dropdown or matched exactly
    connect(routeField, &AutocompleteField::valueChanged, this,
        [this, routeField, startStopEdit, endStopEdit, subTypeCombo, routeTypeCombo,
         downStartEdit, downEndEdit, isLoopCheck, isActiveCheck, lenEdit,
         ticketTypeEdit, ticketPriceEdit, hoursEdit, remarkEdit,
         firstOperEdit, lastOperEdit, parentRouteCombo]() {
        qint64 routeId = routeField->selectedId();
        if (routeId > 0 && m_routesData.contains(routeId)) {
            const RouteInfo &info = m_routesData[routeId];
            startStopEdit->setText(info.startStop);
            endStopEdit->setText(info.endStop);
            // Advanced fields
            downStartEdit->setText(info.downStartStop);
            downEndEdit->setText(info.downEndStop);
            isLoopCheck->setChecked(info.isLoop);
            isActiveCheck->setChecked(info.isActive);
            if (info.lineLengthKm > 0) lenEdit->setText(QString::number(info.lineLengthKm, 'f', 1));
            ticketTypeEdit->setText(info.ticketType);
            ticketPriceEdit->setText(info.ticketPrice);
            hoursEdit->setText(info.operatingHours);
            remarkEdit->setText(info.remark);
            if (!info.firstOperated.isEmpty())
                firstOperEdit->setDate(QDate::fromString(info.firstOperated, "yyyy-MM-dd"));
            if (!info.lastOperated.isEmpty())
                lastOperEdit->setDate(QDate::fromString(info.lastOperated, "yyyy-MM-dd"));
            // Set subType
            for (int i = 0; i < subTypeCombo->count(); ++i) {
                if (subTypeCombo->itemData(i).toString() == info.subType) {
                    subTypeCombo->setCurrentIndex(i);
                    break;
                }
            }
            // Set routeType
            for (int i = 0; i < routeTypeCombo->count(); ++i) {
                if (routeTypeCombo->itemData(i).toString() == info.routeType) {
                    routeTypeCombo->setCurrentIndex(i);
                    break;
                }
            }
            // Set parentRoute in combo
            if (info.parentRouteId > 0) {
                for (int i = 0; i < parentRouteCombo->count(); ++i) {
                    if (parentRouteCombo->itemData(i).toLongLong() == info.parentRouteId) {
                        parentRouteCombo->setCurrentIndex(i);
                        break;
                    }
                }
            }
        }
    });

    // Hook for draft auto-save (basic + advanced)
    connect(routeField, &AutocompleteField::valueChanged, this, [this]() { m_draftTimer->start(); });
    connect(startStopEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(endStopEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(isCurrentCheck, &QCheckBox::toggled, this, [this]() { m_draftTimer->start(); });
    connect(subTypeCombo, &QComboBox::currentIndexChanged, this, [this]() { m_draftTimer->start(); });
    connect(routeTypeCombo, &QComboBox::currentIndexChanged, this, [this]() { m_draftTimer->start(); });
    connect(parentRouteCombo, &QComboBox::currentIndexChanged, this, [this]() { m_draftTimer->start(); });
    connect(downStartEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(downEndEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(isLoopCheck, &QCheckBox::toggled, this, [this]() { m_draftTimer->start(); });
    connect(isActiveCheck, &QCheckBox::toggled, this, [this]() { m_draftTimer->start(); });
    connect(lenEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(ticketTypeEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(ticketPriceEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(hoursEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(remarkEdit, &QLineEdit::textChanged, this, [this]() { m_draftTimer->start(); });
    connect(firstOperEdit, &QDateEdit::dateChanged, this, [this]() { m_draftTimer->start(); });
    connect(lastOperEdit, &QDateEdit::dateChanged, this, [this]() { m_draftTimer->start(); });
}

void MainWindow::removeRouteRow(int index)
{
    if (index < 0 || index >= m_routeRows.size()) return;
    auto &row = m_routeRows[index];
    m_routesLayout->removeWidget(row.container);
    row.container->deleteLater();
    m_routeRows.removeAt(index);
}

void MainWindow::refreshRouteFields()
{
    for (auto &rw : m_routeRows) {
        rw.routeField->setItems(m_routesList);
    }
}

// ---- Buspedia scraping ----
// Uses api.buspedia.top/search to find the bus, then api.buspedia.top/bus/{id} for detail.
// No browser needed.

void MainWindow::fetchFromBuspedia()
{
    QString plate = m_plateEdit->text().trimmed().toUpper();
    if (plate.isEmpty()) {
        QMessageBox::warning(this, QString::fromUtf8("无车牌"),
                             QString::fromUtf8("请先输入车牌号再爬取参数。"));
        return;
    }

    // Remove middle dot, keep spaces for buspedia search
    QString searchPlate = plate;
    searchPlate.remove(QChar(0x00B7));
    // URL-encode: spaces become + (quote_plus style)
    QByteArray encoded = QUrl::toPercentEncoding(searchPlate, " ");
    encoded.replace(' ', '+');

    m_buspediaBtn->setEnabled(false);
    m_buspediaBtn->setText(QString::fromUtf8("搜索中..."));
    m_progressLabel->setText(QString::fromUtf8("正在 buspedia 搜索 %1...").arg(searchPlate));

    QString searchUrl = QString("https://api.buspedia.top/search?name=%1").arg(QString::fromLatin1(encoded));
    QNetworkRequest req{QUrl(searchUrl)};
    req.setAttribute(QNetworkRequest::CacheLoadControlAttribute,
                     QNetworkRequest::AlwaysNetwork);
    req.setRawHeader("User-Agent", "Mozilla/5.0 BusGalleryDesktop/1.0");
    req.setRawHeader("Accept", "application/json");
    req.setRawHeader("Origin", "https://buspedia.top");
    req.setRawHeader("Referer", "https://buspedia.top/");

    QNetworkReply *reply = m_buspediaNam->get(req);
    connect(reply, &QNetworkReply::finished, this, [this, reply, searchPlate]() {
        reply->deleteLater();
        if (reply->error() != QNetworkReply::NoError) {
            m_buspediaBtn->setEnabled(true);
            m_buspediaBtn->setText(QString::fromUtf8("爬取参数"));
            m_progressLabel->setText(QString::fromUtf8("搜索失败: %1").arg(reply->errorString()));
            return;
        }
        QByteArray raw = reply->readAll();
        // Decompress zlib
        QByteArray json;
        if (raw.size() >= 2 && (unsigned char)raw[0] == 0x78) {
            quint32 estSize = quint32(raw.size()) * 10;
            QByteArray header(4, '\0');
            header[0] = (estSize >> 24) & 0xFF;
            header[1] = (estSize >> 16) & 0xFF;
            header[2] = (estSize >> 8) & 0xFF;
            header[3] = estSize & 0xFF;
            json = qUncompress(header + raw);
        } else {
            json = raw;
        }
        QJsonParseError parseErr;
        QJsonDocument doc = QJsonDocument::fromJson(json, &parseErr);
        if (parseErr.error != QJsonParseError::NoError || !doc.isObject()) {
            m_buspediaBtn->setEnabled(true);
            m_buspediaBtn->setText(QString::fromUtf8("爬取参数"));
            m_progressLabel->setText(QString::fromUtf8("搜索解析失败"));
            return;
        }
        // Search result: {"v": [{id, regist, comp, no, ...}]}
        QJsonArray vehicles = doc.object()["v"].toArray();
        if (vehicles.isEmpty()) {
            m_buspediaBtn->setEnabled(true);
            m_buspediaBtn->setText(QString::fromUtf8("爬取参数"));
            m_progressLabel->setText(QString::fromUtf8("未找到 %1，请手动搜索").arg(searchPlate));
            return;
        }
        QString slug = vehicles[0].toObject()["id"].toString();
        if (slug.isEmpty()) {
            m_buspediaBtn->setEnabled(true);
            m_buspediaBtn->setText(QString::fromUtf8("爬取参数"));
            m_progressLabel->setText(QString::fromUtf8("搜索结果无ID"));
            return;
        }
        // Found — fetch detail via API
        fetchBuspediaDetail(slug);
    });
}

void MainWindow::fetchBuspediaDetail(const QString &detailUrl)
{
    // Extract slug from URL like "https://buspedia.top/bus/7yiwti" or "/bus/7yiwti" or "7yiwti"
    QString slug = detailUrl;
    QRegularExpression slugRx(R"(/bus/([^/\s?]+))");
    auto sm = slugRx.match(slug);
    if (sm.hasMatch())
        slug = sm.captured(1);
    else if (slug.startsWith('/'))
        slug = slug.mid(1);
    if (slug.isEmpty()) {
        m_progressLabel->setText(QString::fromUtf8("无法解析 URL，请粘贴完整的 buspedia 车辆详情链接"));
        return;
    }

    m_buspediaBtn->setEnabled(false);
    m_buspediaBtn->setText(QString::fromUtf8("获取中..."));
    m_progressLabel->setText(QString::fromUtf8("正在从 buspedia API 获取数据..."));

    QString apiUrl = QString("https://api.buspedia.top/bus/%1").arg(slug);
    QNetworkRequest req{QUrl(apiUrl)};
    req.setAttribute(QNetworkRequest::CacheLoadControlAttribute,
                     QNetworkRequest::AlwaysNetwork);
    req.setRawHeader("User-Agent", "Mozilla/5.0 BusGalleryDesktop/1.0");
    req.setRawHeader("Accept", "application/json");
    req.setRawHeader("Origin", "https://buspedia.top");
    req.setRawHeader("Referer", "https://buspedia.top/");

    QNetworkReply *reply = m_buspediaNam->get(req);
    connect(reply, &QNetworkReply::finished, this, [this, reply]() {
        reply->deleteLater();
        m_buspediaBtn->setEnabled(true);
        m_buspediaBtn->setText(QString::fromUtf8("爬取参数"));

        if (reply->error() != QNetworkReply::NoError) {
            m_progressLabel->setText(QString::fromUtf8("获取失败: %1").arg(reply->errorString()));
            return;
        }

        QByteArray raw = reply->readAll();
        // Decompress zlib (magic bytes: 78 9c or 78 01)
        QByteArray json;
        if (raw.size() >= 2 && (unsigned char)raw[0] == 0x78) {
            // Prepend expected uncompressed size in big-endian for Qt's qUncompress
            // Use a generous size estimate
            quint32 estSize = quint32(raw.size()) * 10;
            QByteArray header(4, '\0');
            header[0] = (estSize >> 24) & 0xFF;
            header[1] = (estSize >> 16) & 0xFF;
            header[2] = (estSize >> 8) & 0xFF;
            header[3] = estSize & 0xFF;
            json = qUncompress(header + raw);
        } else {
            json = raw;
        }

        if (json.isEmpty()) {
            m_progressLabel->setText(QString::fromUtf8("获取失败: 返回数据为空"));
            return;
        }

        QJsonParseError parseErr;
        QJsonDocument doc = QJsonDocument::fromJson(json, &parseErr);
        if (parseErr.error != QJsonParseError::NoError) {
            m_progressLabel->setText(QString::fromUtf8("解析失败: %1").arg(parseErr.errorString()));
            return;
        }

        QJsonObject root = doc.object();
        QJsonObject veh = root["veh"].toObject();
        QJsonObject model = root["model"].toObject();
        QJsonArray manufArr = model["manuf"].toArray();

        int filled = 0;
        auto setAcText = [&](AutocompleteField *f, const QString &t) {
            if (t.isEmpty()) return;
            f->setText(t); ++filled;
        };
        auto setLine = [&](QLineEdit *ed, const QString &t) {
            if (!t.isEmpty()) { ed->setText(t); ++filled; }
        };

        // Brand — manuf is an array of {name, slug, router}
        QString brandName;
        for (const auto &mv : manufArr) {
            QJsonObject m = mv.toObject();
            if (m.contains("name") && !m["name"].toString().isEmpty()) {
                brandName = m["name"].toString();
                break;
            }
        }
        if (!brandName.isEmpty())
            setAcText(m_brandField, brandName);

        // Model code
        QString modelCode = model["model"].toString();
        if (!modelCode.isEmpty())
            setAcText(m_modelField, modelCode);

        // Engine / Motor
        if (model.contains("ice"))
            setLine(m_engineEdit, model["ice"].toString());
        if (model.contains("motor"))
            setLine(m_motorEdit, model["motor"].toString());
        if (model.contains("trans"))
            setLine(m_transmissionEdit, model["trans"].toString());
        if (model.contains("suspension"))
            setLine(m_suspensionEdit, model["suspension"].toString());
        if (model.contains("step"))
            setLine(m_stepTypeEdit, model["step"].toString());

        // Axle from meta array (e.g. {"item": "车桥品牌", "value": "前ZF RL-75C/..."})
        QJsonArray metaArr = model["meta"].toArray();
        for (const auto &mv : metaArr) {
            QJsonObject m = mv.toObject();
            if (m["item"].toString() == QString::fromUtf8("车桥品牌")) {
                setLine(m_axleEdit, m["value"].toString());
                break;
            }
        }

        // Fuel type
        if (model.contains("fuel")) {
            QString fuel = model["fuel"].toString();
            fuel.replace(",", "+").replace(QString::fromUtf8("，"), "+").remove(' ');
            for (int i = 0; i < m_fuelType->count(); ++i) {
                if (m_fuelType->itemText(i) == fuel) {
                    m_fuelType->setCurrentIndex(i); ++filled; break;
                }
            }
            if (m_fuelType->currentIndex() == 0 && !fuel.isEmpty()) {
                for (int i = 0; i < m_fuelType->count(); ++i) {
                    QString item = m_fuelType->itemText(i);
                    if (item.isEmpty()) continue;
                    if (fuel.contains(item) || item.contains(fuel)) {
                        m_fuelType->setCurrentIndex(i); ++filled; break;
                    }
                }
            }
        }

        // Vehicle fields
        if (veh.contains("regist") && !veh["regist"].toString().isEmpty())
            m_plateEdit->setText(veh["regist"].toString());

        // Dates: handle "2014-06" and year-only "2016"
        auto applyDateStr = [&](const QString &dateStr, QDateEdit *editor, bool &flag) {
            if (dateStr.length() == 4) {
                QDate d = QDate::fromString(dateStr + "-01-01", "yyyy-MM-dd");
                if (d.isValid()) { editor->setDate(d); ++filled; flag = true; }
            } else if (dateStr.length() >= 7) {
                QDate d = QDate::fromString(dateStr.left(7) + "-01", "yyyy-MM-dd");
                if (d.isValid()) { editor->setDate(d); ++filled; flag = true; }
            }
        };
        if (veh.contains("date_manuf"))
            applyDateStr(veh["date_manuf"].toString(), m_factoryDate, m_factoryDateExplicit);
        if (veh.contains("date_serve"))
            applyDateStr(veh["date_serve"].toString(), m_launchDate, m_launchDateExplicit);

        // Company — also auto-fill region from database
        QString regionNote;
        bool companySetRegion = false;
        QJsonObject comp = veh["comp"].toObject();
        if (comp.contains("name")) {
            QString companyName = comp["name"].toString();
            if (!companyName.isEmpty()) {
                // Use exact match only — fuzzy match can false-positive
                // on similar names from other cities (e.g. "公交五公司"
                // matching Hangzhou's "公交五公司[8-]" instead of Xi'an's)
                m_companyField->setText(companyName);
                ++filled;
                qint64 cid = m_companyField->selectedId();
                if (cid > 0) {
                    QString regionName = m_companyField->selectedExtra();
                    if (!regionName.isEmpty()) {
                        bool ok = m_regionPicker->selectByCityName(regionName);
                        companySetRegion = ok;
                        regionNote = ok
                            ? QString::fromUtf8("，地区已自动填充: %1").arg(m_regionPicker->provinceName() + " / " + m_regionPicker->cityName())
                            : QString::fromUtf8("，⚠ 地区 %1 未在列表中，请手动选择").arg(regionName);
                    } else {
                        // Debug: print what's in the items list
                        QString dbg;
                        QList<CatalogItem> items = m_companyField->items();
                        for (const auto &it : items) {
                            if (it.id == cid)
                                dbg = QString::fromUtf8(" name=[%1] extra=[%2]").arg(it.name, it.extra);
                        }
                        regionNote = QString::fromUtf8("，⚠ 公司无地区数据(id=%1)%2").arg(cid).arg(dbg);
                    }
                } else {
                    regionNote = QString::fromUtf8("，⚠ 公司 %1 未匹配到数据库，请手动选择").arg(companyName);
                }
            }
        }

        // Fallback: if company didn't set region, always try plate prefix.
        // This overrides stale draft selections from previous uploads.
        if (!companySetRegion) {
            QString plate = m_plateEdit->text().trimmed().toUpper();
            if (!plate.isEmpty()) {
                bool ok = m_regionPicker->selectByPlateNumber(plate);
                if (ok) {
                    regionNote = QString::fromUtf8("，地区已从车牌推断: %1")
                        .arg(m_regionPicker->provinceName() + " / " + m_regionPicker->cityName());
                }
            }
        }

        // Fleet number — directly on veh object
        if (veh.contains("no") && !veh["no"].toString().isEmpty())
            setLine(m_customNumEdit, veh["no"].toString());

        m_progressLabel->setStyleSheet(QString("color: %1;")
            .arg(ThemeManager::color(Light::Success, Dark::Success)));
        m_progressLabel->setText(
            QString::fromUtf8("已从 buspedia API 提取 %1 个字段%2，请核对后提交。").arg(filled).arg(regionNote));

        // Show region status persistently in toolbar & status bar
        m_statusConn->setText(regionNote.isEmpty()
            ? QString::fromUtf8("爬取完成")
            : QString::fromUtf8("爬取完成%1").arg(regionNote));
        statusBar()->showMessage(regionNote.isEmpty()
            ? QString::fromUtf8("爬取完成，请核对后提交")
            : QString::fromUtf8("爬取完成%1，请核对后提交").arg(regionNote), 10000);
    });
}

// ---- Fetch Route Stops ----
void MainWindow::fetchRouteStops(AutocompleteField *routeField, QLineEdit *startEdit, QLineEdit *endEdit, QPushButton *btn)
{
    QString routeNumber = routeField->text().trimmed();
    if (routeNumber.isEmpty()) {
        QMessageBox::information(this, QString::fromUtf8("提示"), QString::fromUtf8("请先输入线路号"));
        return;
    }
    // Remove trailing "路" if present
    QString searchName = routeNumber;
    if (searchName.endsWith(QString::fromUtf8("路")))
        searchName.chop(1);
    // Add "路" to help buspedia match bus routes
    if (!searchName.endsWith(QString::fromUtf8("路")))
        searchName += QString::fromUtf8("路");

    btn->setEnabled(false);
    btn->setText(QString::fromUtf8("⏳"));

    QNetworkRequest req{QUrl(QString("https://api.buspedia.top/search?name=%1").arg(searchName))};
    req.setRawHeader("User-Agent", "Mozilla/5.0");
    req.setRawHeader("Accept", "application/json");
    req.setRawHeader("Origin", "https://buspedia.top");
    req.setRawHeader("Referer", "https://buspedia.top/");

    QNetworkReply *reply = m_buspediaNam->get(req);
    connect(reply, &QNetworkReply::finished, this, [this, reply, routeField, startEdit, endEdit, btn, routeNumber]() {
        reply->deleteLater();
        btn->setEnabled(true);
        btn->setText(QString::fromUtf8("🔍"));

        if (reply->error() != QNetworkReply::NoError) {
            QMessageBox::warning(nullptr, QString::fromUtf8("错误"),
                QString::fromUtf8("线路查询失败: %1").arg(reply->errorString()));
            return;
        }
        QByteArray raw = reply->readAll();
        QByteArray json;
        if (raw.size() >= 2 && (unsigned char)raw[0] == 0x78) {
            quint32 estSize = quint32(raw.size()) * 10;
            QByteArray header(4, '\0');
            header[0] = (estSize >> 24) & 0xFF;
            header[1] = (estSize >> 16) & 0xFF;
            header[2] = (estSize >> 8) & 0xFF;
            header[3] = estSize & 0xFF;
            json = qUncompress(header + raw);
        } else {
            json = raw;
        }

        QJsonDocument doc = QJsonDocument::fromJson(json);
        QJsonObject obj = doc.object();
        QJsonArray lines = obj["l"].toArray();
        if (lines.isEmpty()) {
            QMessageBox::information(nullptr, QString::fromUtf8("未找到"),
                QString::fromUtf8("未搜索到线路 \"%1\"，请手动输入。").arg(routeNumber));
            return;
        }

        // Pick first result that matches the route number
        QString companyName = m_companyField->text().trimmed();
        QJsonObject best;
        for (const auto &lv : lines) {
            QJsonObject l = lv.toObject();
            QString name = l["name"].toString();
            if (name.contains(routeNumber) || routeNumber.contains(name)) {
                // Prefer match with same company
                QString lcomp = l["comp"].toString();
                if (!companyName.isEmpty() && lcomp.contains(companyName)) {
                    best = l;
                    break;
                }
                if (best.isEmpty()) best = l;
            }
        }
        if (best.isEmpty()) best = lines[0].toObject();

        QJsonArray terms = best["term"].toArray();
        QString start, end;
        if (!terms.isEmpty()) {
            QStringList parts = terms[0].toString().split(QString::fromUtf8("<>"));
            if (parts.size() == 2) { start = parts[0]; end = parts[1]; }
        }
        if (!start.isEmpty()) startEdit->setText(start);
        if (!end.isEmpty()) endEdit->setText(end);
        if (start.isEmpty() && end.isEmpty()) {
            QMessageBox::information(nullptr, QString::fromUtf8("未找到"),
                QString::fromUtf8("线路 \"%1\" 未找到起讫点，请手动输入。").arg(routeNumber));
        }
        m_draftTimer->start();
    });
}

// ---- AI Plate Recognition ----
void MainWindow::onAIRecognize()
{
    QString imgPath = m_imageDrop->selectedFilePath();
    if (imgPath.isEmpty()) {
        QMessageBox::warning(this, QString::fromUtf8("无图片"),
                             QString::fromUtf8("请先选择车辆图片再使用 AI 识别。"));
        return;
    }

    if (m_aiRecognizeWatcher) {
        // Already running
        return;
    }

    m_aiRecognizeBtn->setEnabled(false);
    m_aiRecognizeBtn->setText(QString::fromUtf8("识别中"));
    m_progressLabel->setText(QString::fromUtf8("AI 正在识别车牌..."));

    auto *watcher = new QFutureWatcher<PlateRecognizeResult>(this);
    m_aiRecognizeWatcher = watcher;
    connect(watcher, &QFutureWatcher<PlateRecognizeResult>::finished, this, [this, watcher]() {
        m_aiRecognizeBtn->setEnabled(true);
        m_aiRecognizeBtn->setText(QString::fromUtf8("AI识别"));
        m_aiRecognizeWatcher = nullptr;

        PlateRecognizeResult result = watcher->result();
        watcher->deleteLater();

        if (result.ok && !result.plate.isEmpty()) {
            // Normalize plate format: insert space after province+letter, e.g. "浙A8J401" -> "浙A 8J401"
            QString plate = result.plate.trimmed().toUpper();
            plate.replace(QRegularExpression("\\s+"), "");
            if (plate.length() >= 3)
                plate.insert(2, ' ');
            m_plateEdit->setText(plate);

            // Auto-fill region from plate prefix
            if (!m_regionPicker->hasSelection())
                m_regionPicker->selectByPlateNumber(plate);

            m_progressLabel->setStyleSheet(QString("color: %1;")
                .arg(ThemeManager::color(Light::Success, Dark::Success)));
            m_progressLabel->setText(
                QString::fromUtf8("识别成功: %1（置信度 %.0f%%），正在爬取参数...")
                    .arg(plate).arg(result.confidence * 100));

            // Auto-trigger buspedia scraping after recognition
            fetchFromBuspedia();
        } else {
            m_progressLabel->setStyleSheet(QString("color: %1;")
                .arg(ThemeManager::color(Light::Error, Dark::Error)));
            m_progressLabel->setText(
                QString::fromUtf8("识别失败: %1").arg(
                    result.error.isEmpty() ? QString::fromUtf8("未能识别车牌") : result.error));
        }
    });

    QFuture<PlateRecognizeResult> future = QtConcurrent::run([imgPath]() {
        return PlateAssistantBridge::recognizeFromImage(imgPath, 45000);
    });
    watcher->setFuture(future);
}

static QString draftPath()
{
    return QCoreApplication::applicationDirPath() + "/draft.json";
}

QJsonObject MainWindow::formToJson() const
{
    QJsonObject o;
    o["plateNumber"] = m_plateEdit->text().trimmed().toUpper();
    o["customNumber"] = m_customNumEdit->text();
    o["brandText"] = m_brandField->displayText();
    o["brandId"] = m_brandField->selectedId();
    o["modelText"] = m_modelField->displayText();
    o["modelId"] = m_modelField->selectedId();
    o["companyText"] = m_companyField->displayText();
    o["companyId"] = m_companyField->selectedId();
    o["regionProvince"] = m_regionPicker->provinceName();
    o["regionCity"] = m_regionPicker->cityName();
    if (m_factoryDateExplicit && m_factoryDate->date().isValid())
        o["factoryDate"] = m_factoryDate->date().toString("yyyy-MM");
    if (m_launchDateExplicit && m_launchDate->date().isValid())
        o["launchDate"] = m_launchDate->date().toString("yyyy-MM");
    o["airConditioned"] = m_airConditioned->isChecked();
    o["fuelType"] = m_fuelType->currentIndex();
    o["engine"] = m_engineEdit->text();
    o["motor"] = m_motorEdit->text();
    o["transmission"] = m_transmissionEdit->text();
    o["suspension"] = m_suspensionEdit->text();
    o["axle"] = m_axleEdit->text();
    o["stepType"] = m_stepTypeEdit->text();

    // Save route assignments
    QJsonArray routesArr;
    for (const auto &row : m_routeRows) {
        QString rn = row.routeField->text();
        if (rn.isEmpty()) continue;
        if (!rn.isEmpty() && rn.at(rn.size() - 1).isDigit())
            rn += QString::fromUtf8("路");
        QJsonObject ro;
        ro["routeId"] = row.routeField->selectedId();
        ro["routeNumber"] = rn;
        ro["startStop"] = row.startStopEdit->text().trimmed();
        ro["endStop"] = row.endStopEdit->text().trimmed();
        ro["isCurrent"] = row.isCurrentCheck->isChecked();
        // Advanced fields
        ro["subType"] = row.subTypeCombo->currentData().toString();
        ro["routeType"] = row.routeTypeCombo->currentData().toString();
        ro["parentRouteId"] = row.parentRouteCombo->currentData().toLongLong();
        ro["downStartStop"] = row.downStartStopEdit->text().trimmed();
        ro["downEndStop"] = row.downEndStopEdit->text().trimmed();
        ro["isLoop"] = row.isLoopCheck->isChecked();
        ro["isActive"] = row.isActiveCheck->isChecked();
        ro["lineLengthKm"] = row.lineLengthKmEdit->text().trimmed();
        ro["ticketType"] = row.ticketTypeEdit->text().trimmed();
        ro["ticketPrice"] = row.ticketPriceEdit->text().trimmed();
        ro["operatingHours"] = row.operatingHoursEdit->text().trimmed();
        ro["remark"] = row.remarkEdit->text().trimmed();
        if (row.firstOperatedEdit->date().isValid() && row.firstOperatedEdit->date().year() > 2000)
            ro["firstOperated"] = row.firstOperatedEdit->date().toString("yyyy-MM-dd");
        if (row.lastOperatedEdit->date().isValid() && row.lastOperatedEdit->date().year() > 2000)
            ro["lastOperated"] = row.lastOperatedEdit->date().toString("yyyy-MM-dd");
        routesArr.append(ro);
    }
    o["routes"] = routesArr;

    o["savedAt"] = QDateTime::currentDateTime().toString(Qt::ISODate);
    return o;
}

void MainWindow::formFromJson(const QJsonObject &o)
{
    if (o.isEmpty()) return;
    m_plateEdit->setText(o["plateNumber"].toString());
    m_customNumEdit->setText(o["customNumber"].toString());

    // Don't restore autocomplete text directly — catalog data may not be loaded yet.
    // We'll set the text and let the completer match if possible.
    m_brandField->clear();
    m_modelField->clear();
    m_companyField->clear();

    // Restore region (re-select in picker)
    QString prov = o["regionProvince"].toString();
    QString city = o["regionCity"].toString();
    if (!city.isEmpty()) {
        // Just set the button text; the picker data will be populated
        m_regionPicker->setStaticRegions(); // ensure data loaded
    }

    QString fd = o["factoryDate"].toString();
    if (!fd.isEmpty())
        m_factoryDate->setDate(QDate::fromString(fd + "-01", "yyyy-MM-dd"));
    QString ld = o["launchDate"].toString();
    if (!ld.isEmpty())
        m_launchDate->setDate(QDate::fromString(ld + "-01", "yyyy-MM-dd"));

    m_airConditioned->setChecked(o["airConditioned"].toBool(true));
    int ftIdx = o["fuelType"].toInt(-1);
    if (ftIdx >= 0 && ftIdx < m_fuelType->count())
        m_fuelType->setCurrentIndex(ftIdx);

    m_engineEdit->setText(o["engine"].toString());
    m_motorEdit->setText(o["motor"].toString());
    m_transmissionEdit->setText(o["transmission"].toString());
    m_suspensionEdit->setText(o["suspension"].toString());
    m_axleEdit->setText(o["axle"].toString());
    m_stepTypeEdit->setText(o["stepType"].toString());

    // Restore route assignments
    while (!m_routeRows.isEmpty()) {
        removeRouteRow(0);
    }
    QJsonArray routesArr = o["routes"].toArray();
    for (int i = 0; i < routesArr.size(); ++i) {
        QJsonObject ro = routesArr[i].toObject();
        RouteAssignment ra;
        ra.routeId = static_cast<qint64>(ro["routeId"].toDouble());
        ra.routeNumber = ro["routeNumber"].toString();
        ra.startStop = ro["startStop"].toString();
        ra.endStop = ro["endStop"].toString();
        ra.isCurrent = ro["isCurrent"].toBool(true);
        // Advanced fields
        ra.subType = ro["subType"].toString();
        ra.routeType = ro["routeType"].toString("REGULAR");
        ra.parentRouteId = static_cast<qint64>(ro["parentRouteId"].toDouble());
        ra.downStartStop = ro["downStartStop"].toString();
        ra.downEndStop = ro["downEndStop"].toString();
        ra.isLoop = ro["isLoop"].toBool(false);
        ra.isActive = ro["isActive"].toBool(true);
        ra.lineLengthKm = ro["lineLengthKm"].toString().toDouble();
        ra.ticketType = ro["ticketType"].toString();
        ra.ticketPrice = ro["ticketPrice"].toString();
        ra.operatingHours = ro["operatingHours"].toString();
        ra.remark = ro["remark"].toString();
        ra.firstOperated = ro["firstOperated"].toString();
        ra.lastOperated = ro["lastOperated"].toString();
        addRouteRow(ra);
    }
}

void MainWindow::saveDraft()
{
    QJsonObject obj = formToJson();
    QJsonDocument doc(obj);
    QFile file(draftPath());
    if (file.open(QIODevice::WriteOnly | QIODevice::Truncate)) {
        file.write(doc.toJson(QJsonDocument::Indented));
        file.close();
    }
}

void MainWindow::loadDraft()
{
    QFile file(draftPath());
    if (!file.exists()) return;
    if (file.open(QIODevice::ReadOnly)) {
        QByteArray data = file.readAll();
        file.close();
        QJsonDocument doc = QJsonDocument::fromJson(data);
        if (doc.isObject()) {
            formFromJson(doc.object());

            // Show a notification
            QString savedAt = doc.object()["savedAt"].toString();
            m_progressLabel->setStyleSheet("");
            m_progressLabel->setText(QString::fromUtf8("已恢复草稿 (%1)").arg(savedAt));
            // Auto-clear the message after 5 seconds
            QTimer::singleShot(5000, this, [this]() {
                if (m_progressLabel->text().contains(QString::fromUtf8("草稿")))
                    m_progressLabel->clear();
            });
        }
    }
}
