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
    void updateSearchResult(QJsonObject eventData);
    void updateMembersList(QJsonObject eventData);
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
    void handleNewMessage(QJsonObject eventData);
    void handleNewCall(QJsonObject eventData);
    void handleJoinCall(QJsonObject eventData);
    void handleFetchedUsers(QJsonObject eventData);
    void handleNewGroup(QJsonObject eventData);
    void handleFetchedMembers(QJsonObject eventData);
    void handleNewMembers(QJsonObject eventData);
    void handleMemberRemoval(QJsonObject eventData);
    void handleFetchedGroup(QJsonObject eventData);
    void handleFetchedCalls(QJsonObject eventData);
    void handleFinishedCall(QJsonObject eventData);
};

#endif // EVENTHANDLER_H
