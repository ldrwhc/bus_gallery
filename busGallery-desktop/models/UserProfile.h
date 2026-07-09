#ifndef USERPROFILE_H
#define USERPROFILE_H

#include <QString>
#include <QJsonObject>

struct UserProfile {
    qint64 id = 0;
    QString username;
    QString displayName;
    QString avatarUrl;
    QString bio;
    QString emailMasked;
    bool emailVerified = false;
    qint64 balanceCents = 0;
    QString role; // USER, STATION, REVIEWER
    qint64 reviewRegionId = 0;
    qint64 uploadsCount = 0;

    static UserProfile fromJson(const QJsonObject &obj) {
        UserProfile p;
        p.id = obj["id"].toVariant().toLongLong();
        p.username = obj["username"].toString();
        p.displayName = obj["displayName"].toString();
        p.avatarUrl = obj["avatarUrl"].toString();
        p.bio = obj["bio"].toString();
        p.emailMasked = obj["emailMasked"].toString();
        p.emailVerified = obj["emailVerified"].toBool();
        p.balanceCents = obj["balanceCents"].toVariant().toLongLong();
        p.role = obj["role"].toString();
        p.reviewRegionId = obj["reviewRegionId"].toVariant().toLongLong();
        p.uploadsCount = obj["uploadsCount"].toVariant().toLongLong();
        return p;
    }
};

#endif // USERPROFILE_H
