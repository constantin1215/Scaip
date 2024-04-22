#ifndef EVENTHANDLER_H
#define EVENTHANDLER_H

#include "UI_UpdateTypes.h"
#include "qobject.h"

#include <mainwindow.h>

class EventHandler : public QObject {
    Q_OBJECT
public:
    explicit EventHandler(MainWindow &ui, bool debug = false, QObject *parent = nullptr);
Q_SIGNALS:
    void fetchProfile(QString JWT);
    void updateUI(UI_UpdateType type, QJsonObject eventData);
public slots:
    void handleEvent(QString jsonString);
private:
    MainWindow *ui;
    bool debug;
    QString JWT;

    void handleLogInSuccess(QJsonObject eventData);
    void handleLogInFail(QJsonObject eventData);
    void handleRegisterSuccess(QJsonObject eventData);
    void handleRegisterFail(QJsonObject eventData);
    void handleFetchedProfile(QJsonObject eventData);
    void handleFetchedMessages(QJsonObject eventData);
};

#endif // EVENTHANDLER_H
