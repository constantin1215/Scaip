#include "EventHandler.h"
#include <QtCore/QDebug>
#include <QMetaEnum>
#include <UserData.h>

QT_USE_NAMESPACE

enum class EventsReceived {
    REGISTRATION_SUCCESS,
    REGISTRATION_FAIL,
    LOG_IN_SUCCESS,
    LOG_IN_FAIL,
    FETCH_PROFILE,
    FETCH_MESSAGES
};

static const  QMap<QString, EventsReceived> events {
    {"REGISTRATION_SUCCESS", EventsReceived::REGISTRATION_SUCCESS},
    {"REGISTRATION_FAIL", EventsReceived::REGISTRATION_FAIL},
    {"LOG_IN_SUCCESS", EventsReceived::LOG_IN_SUCCESS},
    {"LOG_IN_FAIL", EventsReceived::LOG_IN_FAIL},
    {"FETCH_PROFILE", EventsReceived::FETCH_PROFILE},
    {"FETCH_MESSAGES", EventsReceived::FETCH_MESSAGES}
};

EventHandler::EventHandler(MainWindow &ui, bool debug, QObject *parent) :
    QObject(parent),
    debug(debug)
{
    this->ui = &ui;
}

void EventHandler::handleEvent(QString jsonString)
{
    qDebug() << "Handling event\n";

    QJsonDocument jsonDocEvent = QJsonDocument::fromJson(jsonString.toUtf8());

    if (jsonDocEvent.isNull()) {
        qDebug() << "Invalid JSON\n";
        return;
    }

    if (!jsonDocEvent.isObject()) {
        qDebug() << "JSON is not object\n";
        return;
    }

    QJsonObject jsonObjEvent = jsonDocEvent.object();

    if (jsonObjEvent["EVENT"].isNull()) {
        qDebug() << "Event not specified\n";
        return;
    }

    QString eventString = jsonObjEvent["EVENT"].toString();

    qDebug() << "Event string: " << eventString << "\n";

    EventsReceived event = events[eventString];

    switch(event){
        case EventsReceived::LOG_IN_SUCCESS:
            handleLogInSuccess(jsonObjEvent);
            break;
        case EventsReceived::LOG_IN_FAIL:
            handleLogInFail(jsonObjEvent);
            break;
        case EventsReceived::REGISTRATION_SUCCESS:
            handleRegisterSuccess(jsonObjEvent);
            break;
        case EventsReceived::REGISTRATION_FAIL:
            handleRegisterFail(jsonObjEvent);
            break;
        case EventsReceived::FETCH_PROFILE:
            handleFetchedProfile(jsonObjEvent);
            break;
        case EventsReceived::FETCH_MESSAGES:
            handleFetchedMessages(jsonObjEvent);
            break;
        }
}

void EventHandler::handleLogInSuccess(QJsonObject eventData)
{
    qDebug() << "Handling LOG_IN_SUCCESS\n";

    UserData::getInstance()->setJWT(eventData["JWT"].toString());

    emit fetchProfile(UserData::getInstance()->getJWT());
}

void EventHandler::handleLogInFail(QJsonObject eventData)
{
    qDebug() << "Handling LOG_IN_FAIL\n";
}

void EventHandler::handleRegisterSuccess(QJsonObject eventData)
{
    qDebug() << "Handling REGISTRATION_SUCCESS\n";
}

void EventHandler::handleRegisterFail(QJsonObject eventData)
{
    qDebug() << "Handling REGISTRATION_FAIL\n";
}

void EventHandler::handleFetchedMessages(QJsonObject eventData)
{
    qDebug() << "Handling FETCH_MESSAGES\n";

    emit updateUI(UI_UpdateType::UPDATE_GROUP_CHAT, eventData);
}

void EventHandler::handleFetchedProfile(QJsonObject eventData)
{
    qDebug() << "Handling FETCH_PROFILE\n";

    UserData* instance = UserData::getInstance();

    instance->setUsername(eventData["username"].toString());
    instance->setEmail(eventData["email"].toString());
    instance->setLastName(eventData["lastName"].toString());
    instance->setFirstName(eventData["firstName"].toString());
    instance->setJSON(eventData);

    instance->printData();

    emit updateUI(UI_UpdateType::AFTER_FETCH_SUCCESS, eventData);
}
