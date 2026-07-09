#include "MainWindow.h"
#include "ui/LoginDialog.h"
#include "utils/Config.h"
#include "utils/ThemeManager.h"
#include "utils/ChinaRegions.h"
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

    // Auto-fill config when model is selected
    connect(m_modelField, &AutocompleteField::valueChanged, this, [this]() {
        qint64 modelId = m_modelField->selectedId();
        if (modelId > 0) {
            m_catalog->fetchModelVehicles(modelId);
        }
    });

    connect(m_catalog, &CatalogApi::modelVehiclesReady, this,
        [this](qint64 modelId, const QJsonArray &vehicles) {
            if (vehicles.isEmpty()) return;

            QJsonObject first = vehicles[0].toObject();
            QJsonObject vehicle = first["vehicle"].toObject();
            QJsonObject config = first["vehicleConfig"].toObject();

            if (config.isEmpty()) return;

            // Auto-fill config fields
            QString engine = config["engine"].toString();
            QString motor = config["motor"].toString();
            QString fuelType = config["fuelType"].toString();
            QString stepType = config["stepType"].toString();
            QString trans = config["transmissionSystem"].toString();
            QString susp = config["suspension"].toString();
            QString axle = config["axle"].toString();

            if (!engine.isEmpty()) m_engineEdit->setText(engine);
            if (!motor.isEmpty()) m_motorEdit->setText(motor);
            if (!stepType.isEmpty()) m_stepTypeEdit->setText(stepType);
            if (!trans.isEmpty()) m_transmissionEdit->setText(trans);
            if (!susp.isEmpty()) m_suspensionEdit->setText(susp);
            if (!axle.isEmpty()) m_axleEdit->setText(axle);

            // Match fuel type dropdown
            if (!fuelType.isEmpty()) {
                for (int i = 0; i < m_fuelType->count(); ++i) {
                    if (m_fuelType->itemText(i) == fuelType) {
                        m_fuelType->setCurrentIndex(i);
                        break;
                    }
                }
            }

            // Auto-fill dates (format: yyyy-MM-dd or yyyy-MM)
            QString fd = vehicle["factoryDate"].toString();
            QString ld = vehicle["launchDate"].toString();
            auto parseDate = [](const QString &s) -> QDate {
                if (s.length() >= 10) return QDate::fromString(s.left(10), "yyyy-MM-dd");
                if (s.length() >= 7)  return QDate::fromString(s.left(7) + "-01", "yyyy-MM-dd");
                return QDate();
            };
            QDate fDate = parseDate(fd);
            QDate lDate = parseDate(ld);
            if (fDate.isValid()) m_factoryDate->setDate(fDate);
            if (lDate.isValid()) m_launchDate->setDate(lDate);

            m_progressLabel->setStyleSheet(
                QString("color: %1;").arg(ThemeManager::color(Light::Success, Dark::Success)));
            m_progressLabel->setText(
                QString::fromUtf8("已从服务器获取 %1 的配置信息").arg(m_modelField->text()));
            QTimer::singleShot(4000, this, [this]() {
                if (m_progressLabel->text().contains(QString::fromUtf8("服务器获取")))
                    m_progressLabel->clear();
            });
        });

    connect(m_upload, &UploadApi::uploadSuccess, this, [this](const UploadResult &result) {
        m_submitBtn->setEnabled(true);
        m_progress->setVisible(false);
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

        // Refresh catalog so newly created brands/models/companies appear immediately
        m_catalog->fetchBrands();
        m_catalog->fetchModels();
        m_catalog->fetchCompanies();
    });

    connect(m_upload, &UploadApi::uploadError, this, [this](const QString &code, const QString &msg) {
        m_submitBtn->setEnabled(true);
        m_progress->setVisible(false);
        m_progressLabel->setProperty("error", true);
        m_progressLabel->setStyleSheet({});
        bool d = ThemeManager::instance().isDark();
        m_progressLabel->setStyleSheet(QString("color: %1;").arg(d ? Dark::Error : Light::Error));
        m_progressLabel->setText(QString::fromUtf8("上传失败: ") + msg);
        QMessageBox::warning(this, QString::fromUtf8("上传失败"), msg);
    });

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

    m_plateEdit = new QLineEdit(formWidget);
    m_plateEdit->setPlaceholderText(QString::fromUtf8("请输入车牌号（必填）"));
    auto *plateLabel = new QLabel(QString::fromUtf8("车牌号 <span style='color:red'>*</span>"));
    plateLabel->setTextFormat(Qt::RichText);
    basicGrid->addRow(plateLabel, m_plateEdit);

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

    m_factoryDate = new QDateEdit(formWidget);
    m_factoryDate->setCalendarPopup(true);
    m_factoryDate->setDisplayFormat("yyyy-MM");
    m_factoryDate->setDate(QDate::currentDate());
    m_factoryDate->setSpecialValueText(" ");
    m_factoryDate->setMinimumDate(QDate(1950, 1, 1));
    detailGrid->addRow(QString::fromUtf8("出厂日期"), m_factoryDate);

    m_launchDate = new QDateEdit(formWidget);
    m_launchDate->setCalendarPopup(true);
    m_launchDate->setDisplayFormat("yyyy-MM");
    m_launchDate->setDate(QDate::currentDate());
    m_launchDate->setSpecialValueText(" ");
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
    payload.plateNumber = m_plateEdit->text().trimmed();
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

    if (m_factoryDate->date().isValid())
        payload.factoryDate = m_factoryDate->date().toString("yyyy-MM") + "-01";
    if (m_launchDate->date().isValid())
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

    m_submitBtn->setEnabled(false);
    m_progress->setVisible(true);
    m_progress->setRange(0, 0);
    m_progressLabel->setProperty("error", false);
    m_progressLabel->setStyleSheet(QString("color: %1;")
        .arg(ThemeManager::color(Light::Accent, Dark::Accent)));
    m_progressLabel->setText(QString::fromUtf8("正在上传图片和车辆信息..."));

    m_upload->uploadVehicle(m_imageDrop->selectedFilePath(), payload);
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
    m_factoryDate->setDate(QDate::currentDate());
    m_launchDate->setDate(QDate::currentDate());
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

static QString draftPath()
{
    return QCoreApplication::applicationDirPath() + "/draft.json";
}

QJsonObject MainWindow::formToJson() const
{
    QJsonObject o;
    o["plateNumber"] = m_plateEdit->text();
    o["customNumber"] = m_customNumEdit->text();
    o["brandText"] = m_brandField->displayText();
    o["brandId"] = m_brandField->selectedId();
    o["modelText"] = m_modelField->displayText();
    o["modelId"] = m_modelField->selectedId();
    o["companyText"] = m_companyField->displayText();
    o["companyId"] = m_companyField->selectedId();
    o["regionProvince"] = m_regionPicker->provinceName();
    o["regionCity"] = m_regionPicker->cityName();
    if (m_factoryDate->date().isValid())
        o["factoryDate"] = m_factoryDate->date().toString("yyyy-MM");
    if (m_launchDate->date().isValid())
        o["launchDate"] = m_launchDate->date().toString("yyyy-MM");
    o["airConditioned"] = m_airConditioned->isChecked();
    o["fuelType"] = m_fuelType->currentIndex();
    o["engine"] = m_engineEdit->text();
    o["motor"] = m_motorEdit->text();
    o["transmission"] = m_transmissionEdit->text();
    o["suspension"] = m_suspensionEdit->text();
    o["axle"] = m_axleEdit->text();
    o["stepType"] = m_stepTypeEdit->text();
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
