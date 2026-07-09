/****************************************************************************
** Meta object code from reading C++ file 'AuthApi.h'
**
** Created by: The Qt Meta Object Compiler version 69 (Qt 6.10.1)
**
** WARNING! All changes made in this file will be lost!
*****************************************************************************/

#include "../../../api/AuthApi.h"
#include <QtNetwork/QSslError>
#include <QtCore/qmetatype.h>

#include <QtCore/qtmochelpers.h>

#include <memory>


#include <QtCore/qxptype_traits.h>
#if !defined(Q_MOC_OUTPUT_REVISION)
#error "The header file 'AuthApi.h' doesn't include <QObject>."
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
struct qt_meta_tag_ZN7AuthApiE_t {};
} // unnamed namespace

template <> constexpr inline auto AuthApi::qt_create_metaobjectdata<qt_meta_tag_ZN7AuthApiE_t>()
{
    namespace QMC = QtMocConstants;
    QtMocHelpers::StringRefStorage qt_stringData {
        "AuthApi",
        "loginSuccess",
        "",
        "LoginResult",
        "result",
        "loginError",
        "code",
        "message",
        "captchaReady",
        "captchaId",
        "QPixmap",
        "image",
        "captchaError",
        "loggedOut"
    };

    QtMocHelpers::UintData qt_methods {
        // Signal 'loginSuccess'
        QtMocHelpers::SignalData<void(const LoginResult &)>(1, 2, QMC::AccessPublic, QMetaType::Void, {{
            { 0x80000000 | 3, 4 },
        }}),
        // Signal 'loginError'
        QtMocHelpers::SignalData<void(const QString &, const QString &)>(5, 2, QMC::AccessPublic, QMetaType::Void, {{
            { QMetaType::QString, 6 }, { QMetaType::QString, 7 },
        }}),
        // Signal 'captchaReady'
        QtMocHelpers::SignalData<void(const QString &, const QPixmap &)>(8, 2, QMC::AccessPublic, QMetaType::Void, {{
            { QMetaType::QString, 9 }, { 0x80000000 | 10, 11 },
        }}),
        // Signal 'captchaError'
        QtMocHelpers::SignalData<void(const QString &)>(12, 2, QMC::AccessPublic, QMetaType::Void, {{
            { QMetaType::QString, 7 },
        }}),
        // Signal 'loggedOut'
        QtMocHelpers::SignalData<void()>(13, 2, QMC::AccessPublic, QMetaType::Void),
    };
    QtMocHelpers::UintData qt_properties {
    };
    QtMocHelpers::UintData qt_enums {
    };
    return QtMocHelpers::metaObjectData<AuthApi, qt_meta_tag_ZN7AuthApiE_t>(QMC::MetaObjectFlag{}, qt_stringData,
            qt_methods, qt_properties, qt_enums);
}
Q_CONSTINIT const QMetaObject AuthApi::staticMetaObject = { {
    QMetaObject::SuperData::link<QObject::staticMetaObject>(),
    qt_staticMetaObjectStaticContent<qt_meta_tag_ZN7AuthApiE_t>.stringdata,
    qt_staticMetaObjectStaticContent<qt_meta_tag_ZN7AuthApiE_t>.data,
    qt_static_metacall,
    nullptr,
    qt_staticMetaObjectRelocatingContent<qt_meta_tag_ZN7AuthApiE_t>.metaTypes,
    nullptr
} };

void AuthApi::qt_static_metacall(QObject *_o, QMetaObject::Call _c, int _id, void **_a)
{
    auto *_t = static_cast<AuthApi *>(_o);
    if (_c == QMetaObject::InvokeMetaMethod) {
        switch (_id) {
        case 0: _t->loginSuccess((*reinterpret_cast<std::add_pointer_t<LoginResult>>(_a[1]))); break;
        case 1: _t->loginError((*reinterpret_cast<std::add_pointer_t<QString>>(_a[1])),(*reinterpret_cast<std::add_pointer_t<QString>>(_a[2]))); break;
        case 2: _t->captchaReady((*reinterpret_cast<std::add_pointer_t<QString>>(_a[1])),(*reinterpret_cast<std::add_pointer_t<QPixmap>>(_a[2]))); break;
        case 3: _t->captchaError((*reinterpret_cast<std::add_pointer_t<QString>>(_a[1]))); break;
        case 4: _t->loggedOut(); break;
        default: ;
        }
    }
    if (_c == QMetaObject::IndexOfMethod) {
        if (QtMocHelpers::indexOfMethod<void (AuthApi::*)(const LoginResult & )>(_a, &AuthApi::loginSuccess, 0))
            return;
        if (QtMocHelpers::indexOfMethod<void (AuthApi::*)(const QString & , const QString & )>(_a, &AuthApi::loginError, 1))
            return;
        if (QtMocHelpers::indexOfMethod<void (AuthApi::*)(const QString & , const QPixmap & )>(_a, &AuthApi::captchaReady, 2))
            return;
        if (QtMocHelpers::indexOfMethod<void (AuthApi::*)(const QString & )>(_a, &AuthApi::captchaError, 3))
            return;
        if (QtMocHelpers::indexOfMethod<void (AuthApi::*)()>(_a, &AuthApi::loggedOut, 4))
            return;
    }
}

const QMetaObject *AuthApi::metaObject() const
{
    return QObject::d_ptr->metaObject ? QObject::d_ptr->dynamicMetaObject() : &staticMetaObject;
}

void *AuthApi::qt_metacast(const char *_clname)
{
    if (!_clname) return nullptr;
    if (!strcmp(_clname, qt_staticMetaObjectStaticContent<qt_meta_tag_ZN7AuthApiE_t>.strings))
        return static_cast<void*>(this);
    return QObject::qt_metacast(_clname);
}

int AuthApi::qt_metacall(QMetaObject::Call _c, int _id, void **_a)
{
    _id = QObject::qt_metacall(_c, _id, _a);
    if (_id < 0)
        return _id;
    if (_c == QMetaObject::InvokeMetaMethod) {
        if (_id < 5)
            qt_static_metacall(this, _c, _id, _a);
        _id -= 5;
    }
    if (_c == QMetaObject::RegisterMethodArgumentMetaType) {
        if (_id < 5)
            *reinterpret_cast<QMetaType *>(_a[0]) = QMetaType();
        _id -= 5;
    }
    return _id;
}

// SIGNAL 0
void AuthApi::loginSuccess(const LoginResult & _t1)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 0, nullptr, _t1);
}

// SIGNAL 1
void AuthApi::loginError(const QString & _t1, const QString & _t2)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 1, nullptr, _t1, _t2);
}

// SIGNAL 2
void AuthApi::captchaReady(const QString & _t1, const QPixmap & _t2)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 2, nullptr, _t1, _t2);
}

// SIGNAL 3
void AuthApi::captchaError(const QString & _t1)
{
    QMetaObject::activate<void>(this, &staticMetaObject, 3, nullptr, _t1);
}

// SIGNAL 4
void AuthApi::loggedOut()
{
    QMetaObject::activate(this, &staticMetaObject, 4, nullptr);
}
QT_WARNING_POP
