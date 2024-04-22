#ifndef USERDATA_H
#define USERDATA_H

#include "qjsonobject.h"
#include "qobject.h"

#include <qjsonarray.h>
class UserData : public QObject {
    Q_OBJECT
private:
    QString JWT;
    QString username;
    QString firstName;
    QString lastName;
    QString email;
    QJsonArray groups;
    QJsonObject rawJson;

    UserData() {}

    static UserData* userData_;

public:
    void setJWT(QString JWT);
    void setUsername(QString username);
    void setFirstName(QString firstName);
    void setLastName(QString lastName);
    void setEmail(QString email);
    void setJSON(QJsonObject json);

    QString getJWT();
    QString getUsername();
    QString getFirstName();
    QString getLastName();
    QString getEmail();
    QJsonArray getGroups();
    QJsonObject getJSON();
    void printData();

    UserData(UserData &other) = delete;
    void operator=(const UserData &) = delete;
    static UserData *getInstance();
};

#endif // USERDATA_H
