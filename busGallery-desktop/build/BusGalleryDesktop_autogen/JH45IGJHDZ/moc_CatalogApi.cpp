/****************************************************************************
** Meta object code from reading C++ file 'CatalogApi.h'
**
** Created by: The Qt Meta Object Compiler version 69 (Qt 6.10.1)
**
** WARNING! All changes made in this file will be lost!
*****************************************************************************/

#include "../../../api/CatalogApi.h"
#include <QtNetwork/QSslError>
#include <QtCore/qmetatype.h>
#include <QtCore/QList>

#include <QtCore/qtmochelpers.h>

#include <memory>


#include <QtCore/qxptype_traits.h>
#if !defined(Q_MOC_OUTPUT_REVISION)
#error "The header file 'CatalogApi.h' doesn't include <QObject>."
#elif Q_MOC_OUTPUT_REVISION != 69
#error "This file was generated using the moc from 6.10.1. It"
#error "cannot be used with the include files from this version of Qt."
#error "(The moc has changed too much.)"
#endif

#ifndef Q_CONSTINIT
#define Q_CONSTINIT
#endif

QT_WARNING_PUSH
QT_WARNING_DISABLE_DEPRECATED
QT_WARNING_DISABLE_GCC("-Wuseless-cast")
namespace {
struct qt_meta_tag_ZN10CatalogApiE_t {};
} // unnamed namespace

template <> constexpr inline auto CatalogApi::qt_create_metaobjectdata<qt_meta_tag_ZN10CatalogApiE_t>()
{
    namespace QMC = QtMocConstants;
    QtMocHelpers::StringRefStorage qt_stringData {
        "CatalogApi",
        "regionsReady",
        "",
        "QList<RegionNode>",
        "regions",
        "brandsReady",
        "QList<CatalogItem>",
        "brands",
        "modelsReady",
        "models",
        "companiesReady",
        "companies",
        "modelVehiclesReady",
        "modelId",
        "QJsonArray",
        "vehicles",
        "catalogError",
        "endpoint",
        "message"
    };

    QtMocHelpers::UintData qt_methods {
        // Signal 'regionsReady'
        QtMocHelpers::SignalData<void(const QList<RegionNode> &)>(1, 2, QMC::AccessPublic, QMetaType::Void, {{
            { 0x80000000 | 3, 4 },
        }}),
        // Signal 'brandsReady'
        QtMocHelpers::SignalData<void(const QList<CatalogItem> &)>(5, 2, QMC::AccessPublic, QMetaType::Void, {{
            { 0x80000000 | 6, 7 },
        }}),
        // Signal 'modelsReady'
        QtMocHelpers::SignalData<void(const QList<CatalogItem> &)>(8, 2, QMC::AccessPublic, QMetaType::Void, {{
            { 0x80000000 | 6, 9 },
        }}),
        // Signal 'companiesReady'
        QtMocHelpers::SignalData<void(const QList<CatalogItem> &)>(10, 2, QMC::AccessPublic, QMetaType::Void, {{
            { 0x80000000 | 6, 11 },
        }}),
        // Signal 'modelVehiclesReady'
        QtMocHelpers::SignalData<void(qint64, const QJsonArray &)>(12, 2, QMC::AccessPublic, QMetaType::Void, {{
            { QMetaType::LongLong, 13 }, { 0x80000000 | 14, 15 },
        }}),
        // Signal 'catalogError'
        QtMocHelpers::SignalData<void(const QString &, const QString &)>(16, 2, QMC::AccessPublic, QMetaType::Void, {{
            { QMetaType::QString, 17 }, { QMetaType::QString, 18 },
        }}),
    };
    QtMocHelpers::UintData qt_properties {
    };
    QtMocHelpers::UintData qt_enums {
    };
    return QtMocHelpers::metaObjectData<CatalogApi, qt_meta_tag_ZN10CatalogApiE_t>(QMC::MetaObjectFlag{}, qt_stringData,
            qt_methods, qt_properties, qt_enums);
}
Q_CONSTINIT const QMetaObject CatalogApi::staticMetaObject = { {
    QMetaObject::SuperData::link<QObject::staticMetaObject>(),
    qt_staticMetaObjectStaticContent<qt_meta_tag_ZN10CatalogApiE_t>.stringdata,
    qt_staticMetaObjectStaticContent<qt_meta_tag_ZN10CatalogApiE_t>.data,
    qt_static_metacall,
    nullptr,
    qt_staticMetaObjectRelocatingContent<qt_meta_tag_ZN10CatalogApiE_t>.metaTypes,
    nullptr
} };

void CatalogApi::qt_static_metacall(QObject *_o, QMetaObject::Call _c, int _id, void **_a)
{
    auto *_t = static_cast<CatalogApi *>(_o);
    if (_c == QMetaObject::InvokeMetaMethod) {
        switch (_id) {
        case 0: _t->regionsReady((*reinterpret_cast<std::add_pointer_t<QList<RegionNode>>>(_a[1]))); break;
        case 1: _t->brandsReady((*reinterpret_cast<std::add_pointer_t<QList<CatalogItem>>>(_a[1]))); break;
        case 2: _t->modelsReady((*reinterpret_cast<std::add_pointer_t<QList<CatalogItem>>>(_a[1]))); break;
        case 3: _t->companiesReady((*reinterpret_cast<std::add_pointer_t<QList<CatalogItem>>>(_a[1]))); break;
        case 4: _t->modelVehiclesReady((*reinterpret_cast<std::add_pointer_t<qint64>>(_a[1])),(*reinterpret_cast<std::add_pointer_t<QJsonArray>>(_a[2]))); break;
        case 5: _t->catalogError((*reinterpret_cast<std::add_pointer_t<QString>>(_a[1])),(*reinterpret_cast<std::add_pointer_t<QString>>(_a[2]))); break;
        default: ;
        }
    }
    if (_c == QMetaObject::IndexOfMethod) {
        if (QtMocHelpers::indexOfMethod<void (CatalogApi::*)(const QList<RegionNode> & )>(_a, &CatalogApi::regionsReady, 0))
            return;
        if (QtMocHelpers::indexOfMethod<void (CatalogApi::*)(const QList<CatalogItem> & )>(_a, &CatalogApi::brandsReady, 1))
            return;
        if (QtMocHelpers::indexOfMethod<void (CatalogApi::*)(const QList<CatalogItem> & )>(_a, &CatalogApi::modelsReady, 2))
            return;
        if (QtMocHelpers::indexOfMethod<void (CatalogApi::*)(const QList<CatalogItem> & )>(_a, &CatalogApi::companiesReady, 3))
            return;
        if (QtMocHelpers::indexOfMethod<void (CatalogApi::*)(qint64 , const QJsonArray & )>(_a, &CatalogApi::modelVehiclesReady, 4))
            return;
        if (QtMocHelpers::indexOfMethod<void (CatalogApi::*)(const QString & , const QString & )>(_a, &CatalogApi::catalogError, 5))
            return;
    }
}

const QMetaObject *CatalogApi::metaObject() const
{
    return QObject::d_ptr->metaObject ? QObject::d_ptr->dynamicMetaObject() : &staticMetaObject;
}

void *CatalogApi::qt_metacast(const char *_clname)
{
    if (!_clname) return nullptr;
    if (!strcmp(_clname, qt_staticMetaObjectStaticContent<qt_meta_tag_ZN10CatalogApiE_t>.strings))
        return static_cast<void*>(this);
    return QObject::qt_metacast(_clname);
}

int CatalogApi::qt_metacall(QMetaObject::Call _c, int _id, void **_a)
{
    _id = QObject::qt_metacall(_c, _id, _a);
    if (_id < 0)
        return _id;
    if (_c == QMetaObject::InvokeMetaMethod) {
        if (_id < 6)
            qt_static_metacall(this, _c, _id, _a);
        _id -= 6;
    }
    if (_c == QMetaObject::RegisterMethodArgumentMetaType) {
        if (_id < 6)
            *reinterpret_cast<QMetaType *>(_a[0]) = QMetaType();
        _id -= 6;
    }
    return _id;
}

// SIGNAL 0
void CatalogApi::regionsReady(const QList<RegionNode> & _t1)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 0, nullptr, _t1);
}

// SIGNAL 1
void CatalogApi::brandsReady(const QList<CatalogItem> & _t1)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 1, nullptr, _t1);
}

// SIGNAL 2
void CatalogApi::modelsReady(const QList<CatalogItem> & _t1)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 2, nullptr, _t1);
}

// SIGNAL 3
void CatalogApi::companiesReady(const QList<CatalogItem> & _t1)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 3, nullptr, _t1);
}

// SIGNAL 4
void CatalogApi::modelVehiclesReady(qint64 _t1, const QJsonArray & _t2)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 4, nullptr, _t1, _t2);
}

// SIGNAL 5
void CatalogApi::catalogError(const QString & _t1, const QString & _t2)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 5, nullptr, _t1, _t2);
}
QT_WARNING_POP
