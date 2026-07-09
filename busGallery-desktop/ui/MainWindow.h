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
#include "api/ApiClient.h"
#include "api/AuthApi.h"
#include "api/CatalogApi.h"
#include "api/UploadApi.h"
#include "models/UserProfile.h"
#include "models/CatalogItem.h"
#include "ui/ImageDropZone.h"
#include "ui/RegionPicker.h"
#include "ui/AutocompleteField.h"

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
    QLineEdit *m_customNumEdit;
    AutocompleteField *m_brandField;
    AutocompleteField *m_modelField;
    AutocompleteField *m_companyField;
    RegionPicker *m_regionPicker;
    QDateEdit *m_factoryDate;
    QDateEdit *m_launchDate;
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

    QTimer *m_draftTimer;
    bool m_suppressDraft = true; // don't save while loading draft
};

#endif // MAINWINDOW_H
