#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <QStackedWidget>
#include <QLabel>
#include <QProgressBar>
#include <QPushButton>
#include <QLineEdit>
#include <QComboBox>
#include <QCheckBox>
#include <QDateEdit>
#include "ui/ClearableDateEdit.h"
#include <QVBoxLayout>
#include <QHBoxLayout>
#include "api/ApiClient.h"
#include "api/AuthApi.h"
#include "api/CatalogApi.h"
#include "api/UploadApi.h"
#include "models/UserProfile.h"
#include "models/CatalogItem.h"
#include "ui/ImageDropZone.h"
#include "ui/RegionPicker.h"
#include "ui/AutocompleteField.h"

class QFutureWatcherBase;
struct PlateRecognizeResult;

class MainWindow : public QMainWindow
{
    Q_OBJECT
public:
    explicit MainWindow(ApiClient *client, QWidget *parent = nullptr);

private slots:
    void showLogin();
    void onLoginSuccess(const LoginResult &result);
    void onLogout();
    void onSessionExpired();
    void submitUpload();
    void resetForm();
    void onFuelTypeChanged(int index);
    void saveDraft();
    void loadDraft();
    void addRouteRow(const RouteAssignment &ra = RouteAssignment());
    void removeRouteRow(int index);
    void refreshRouteFields();
    void fetchFromBuspedia();
    void fetchBuspediaDetail(const QString &detailUrl);
    void onAIRecognize();

private:
    void setupUi();
    void setupUploadForm(QWidget *page);
    void loadCatalog();
    void updateUserInfo();
    bool validateForm();
    QJsonObject formToJson() const;
    void formFromJson(const QJsonObject &obj);

    ApiClient *m_client;
    AuthApi *m_auth;
    CatalogApi *m_catalog;
    UploadApi *m_upload;
    UserProfile m_profile;

    QStackedWidget *m_stack;
    QWidget *m_loginPrompt;
    QWidget *m_uploadPage;

    // Status bar
    QLabel *m_statusUser;
    QLabel *m_statusConn;

    // Upload form widgets
    ImageDropZone *m_imageDrop;
    QLineEdit *m_plateEdit;
    QPushButton *m_buspediaBtn;
    QPushButton *m_aiRecognizeBtn;
    QLineEdit *m_customNumEdit;
    AutocompleteField *m_brandField;
    AutocompleteField *m_modelField;
    AutocompleteField *m_companyField;
    RegionPicker *m_regionPicker;
    ClearableDateEdit *m_factoryDate;
    ClearableDateEdit *m_launchDate;
    QCheckBox *m_airConditioned;
    QComboBox *m_fuelType;
    QLineEdit *m_engineEdit;
    QLineEdit *m_motorEdit;
    QLineEdit *m_transmissionEdit;
    QLineEdit *m_suspensionEdit;
    QLineEdit *m_axleEdit;
    QLineEdit *m_stepTypeEdit;

    // Progress
    QProgressBar *m_progress;
    QLabel *m_progressLabel;
    QPushButton *m_submitBtn;
    QPushButton *m_resetBtn;

    // Compressed temp file (cleaned after upload)
    QString m_compressedFilePath;

    // Route rows
    QWidget *m_routesContainer;
    QVBoxLayout *m_routesLayout;
    QPushButton *m_addRouteBtn;
    QList<CatalogItem> m_routesList;        // pre-loaded active routes for autocomplete
    QMap<qint64, RouteInfo> m_routesData;   // full route details keyed by id, for auto-fill
    struct RouteRowWidgets {
        QWidget *container;
        AutocompleteField *routeField;
        QLineEdit *startStopEdit;
        QLineEdit *endStopEdit;
        QCheckBox *isCurrentCheck;
        QPushButton *removeBtn;
        // Advanced settings widgets
        QPushButton *advancedToggle;
        QWidget *advancedPanel;
        QComboBox *subTypeCombo;
        QComboBox *routeTypeCombo;
        QComboBox *parentRouteCombo;
        QLineEdit *downStartStopEdit;
        QLineEdit *downEndStopEdit;
        QCheckBox *isLoopCheck;
        QCheckBox *isActiveCheck;
        QLineEdit *lineLengthKmEdit;
        QLineEdit *ticketTypeEdit;
        QLineEdit *ticketPriceEdit;
        QLineEdit *operatingHoursEdit;
        QLineEdit *remarkEdit;
        QDateEdit *firstOperatedEdit;
        QDateEdit *lastOperatedEdit;
    };
    QList<RouteRowWidgets> m_routeRows;

    QNetworkAccessManager *m_buspediaNam;  // for buspedia.top scraping
    QTimer *m_draftTimer;
    bool m_suppressDraft = true; // don't save while loading draft

    // AI plate recognition
    QFutureWatcherBase *m_aiRecognizeWatcher = nullptr;
};

#endif // MAINWINDOW_H
