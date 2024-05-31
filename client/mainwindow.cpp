#include "mainwindow.h"
#include "NewGroupDialog.h"
#include "ui_mainwindow.h"

#include <MembersListDialog.h>
#include <QJsonArray>
#include <QThread>
#include <QTimer>
#include <UserData.h>
#include <UserSearchDialog.h>
#include <calldialog.h>
#include <callwindow.h>
#include <groupwidget.h>
#include <messagewidget.h>

enum class Pages {
    MAIN_PAGE = 0,
    LOG_IN_PAGE = 1,
    REGISTER_PAGE = 2,
    DASHBOARD_PAGE = 3
};

enum class Tabs {
    PROFILE_TAB = 0,
    CHATS_TAB = 1,
};

enum class GroupStatus {
    NO_GROUP_SELECTED = 0,
    GROUP_SELECTED = 1
};

enum class EventsUI {
    LOG_IN,
    REGISTER,
    NEW_MESSAGE,
    NEW_CALL,
    JOIN_CALL,
    ADD_MEMBERS
};

enum class CallTypes {
    INSTANT,
    SCHEDULED
};

QMap<QString, QList<QJsonObject>> groupConversations;
QMap<QString, int> groupLastMessagesFetchedCount;

QString eventToString(EventsUI event) {
    switch(event) {
        case EventsUI::LOG_IN:
            return "LOG_IN";
        case EventsUI::REGISTER:
            return "REGISTER";
        case EventsUI::NEW_MESSAGE:
            return "NEW_MESSAGE";
        case EventsUI::NEW_CALL:
            return "NEW_CALL";
        case EventsUI::JOIN_CALL:
            return "JOIN_CALL";
        case EventsUI::ADD_MEMBERS:
            return "ADD_MEMBERS";
        }
}

QString callTypeToString(CallTypes type) {
    switch(type) {
    case CallTypes::INSTANT:
        return "INSTANT";
    case CallTypes::SCHEDULED:
        return "SCHEDULED";
    }
}

MainWindow::MainWindow(QWidget *parent, WSClient *client)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    ui->setupUi(this);

    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::MAIN_PAGE));
    ui->stackedWidget_dashboard->setCurrentIndex(static_cast<int>(Tabs::PROFILE_TAB));
    ui->stackedWidget_groups->setCurrentIndex(static_cast<int>(GroupStatus::NO_GROUP_SELECTED));
    this->client = client;

    ui->input_username_log_in->setText("costi1");
    ui->input_password_log_in->setText("1234");

    ui->input_username_register->setText("costix");
    ui->input_password_register->setText("1234");
    ui->input_email_register->setText("costix@mail.com");
    ui->input_first_name_register->setText("costi");
    ui->input_last_name_register->setText("ciobanu");

    ui->logoMain->setPixmap(this->logo->copy().scaled(ui->logoMain->width(), ui->logoMain->height(), Qt::KeepAspectRatio));
}

MainWindow::~MainWindow()
{
    delete ui;
}

inline void swap(QJsonValueRef v1, QJsonValueRef v2)
{
    QJsonValue temp(v1);
    v1 = QJsonValue(v2);
    v2 = temp;
}

void MainWindow::handleProfileFetchUpdate()
{
    ui->loading_gif_login->setMovie(nullptr);
    this->loadingGif->stop();
    ui->loading_gif_login->setStyleSheet("color: #11AA11");
    ui->loading_gif_login->setText("Logged in successfully!");

    UserData* instance = UserData::getInstance();

    ui->input_username_profile->setText(instance->getUsername());
    ui->input_email_profile->setText(instance->getEmail());
    ui->input_first_name_profile->setText(instance->getFirstName());
    ui->input_last_name_profile->setText(instance->getLastName());

    QJsonArray groups = instance->getGroups();
    std::sort(groups.begin(), groups.end(), [](const QJsonValue &v1, const QJsonValue &v2) {
        return v1.toObject()
                    ["lastMessage"].toObject()
                    ["timestamp$delegate"].toObject()
                    ["value"].toDouble() > v2.toObject()
                                                ["lastMessage"].toObject()
                                                ["timestamp$delegate"].toObject()
                                                ["value"].toDouble();
    });
    ui->groupListWidget->setResizeMode(QListView::Adjust);

    for(int i = 0;i < groups.size(); i++) {
        QJsonObject currentGroup = groups.at(i).toObject();

        QListWidgetItem *item = new QListWidgetItem();

        QString lastMessage;
        qint64 secondsLong;
        bool isLastMessageNull = currentGroup["lastMessage"].isNull();
        if(!isLastMessageNull) {
            QJsonObject lastMessageObject = currentGroup["lastMessage"].toObject();
            lastMessage = lastMessageObject["content"].toString();

            QJsonObject timestampObject = lastMessageObject["timestamp$delegate"].toObject();
            double seconds = timestampObject["value"].toDouble();

            secondsLong = static_cast<qint64>(seconds);
        }

        QString ownerId = currentGroup["owner"].toObject()["id"].toString();

        GroupWidget *widget = new GroupWidget(
            this,
            currentGroup["id"].toString(),
            currentGroup["title"].toString(),
            isLastMessageNull ? "No messages yet." : lastMessage,
            isLastMessageNull ? 0 : secondsLong,
            ownerId
            );

        item->setSizeHint(widget->sizeHint());

        ui->groupListWidget->addItem(item);
        ui->groupListWidget->setItemWidget(item, widget);
    }

    QTimer::singleShot(2000, this, [&]() {
        ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::DASHBOARD_PAGE));
        ui->backButton_2->setEnabled(true);
        ui->log_in_Button->setEnabled(true);
    });
}

void MainWindow::prependNewMessages(QJsonArray recentMessages, QString groupId)
{
    delete ui->messagesListWidget->takeItem(0);

    for(int i = recentMessages.count() - 1; i >= 0; i--) {
        QJsonObject messageObject = recentMessages.at(i).toObject();
        groupConversations[groupId].prepend(messageObject);
    }

    ui->messagesListWidget->clear();

    for (int i = 0; i < groupConversations[groupId].count(); i++) {
        MessageWidget *widget = new MessageWidget(
            this,
            groupConversations[groupId].at(i)["_id"].toString(),
            groupConversations[groupId].at(i)["userId"].toString(),
            groupConversations[groupId].at(i)["content"].toString(),
            groupConversations[groupId].at(i)["timestamp"].toInteger()
            );


        QListWidgetItem *item = new QListWidgetItem(ui->messagesListWidget);

        item->setSizeHint(widget->sizeHint());
        ui->messagesListWidget->setItemWidget(item, widget);
    }

    if (recentMessages.size() == 20) {
        QPushButton *loadMoreMessagesButton = new QPushButton("Load more", this);
        QListWidgetItem *item = new QListWidgetItem();

        item->setSizeHint(loadMoreMessagesButton->sizeHint());
        ui->messagesListWidget->insertItem(0, item);
        ui->messagesListWidget->setItemWidget(item, loadMoreMessagesButton);

        connect(loadMoreMessagesButton, &QPushButton::clicked, this, [this, groupId, recentMessages]() {
            emit fetchMessages(groupId, recentMessages.at(0).toObject()["timestamp"].toInteger());
        });
    }

    ui->messagesListWidget->scrollToTop();
}

void MainWindow::handleGroupChatConversationUpdate(QJsonObject eventData)
{
    if (eventData["_id"].isNull() || eventData["recentMessages"].isNull()) {
        qDebug() << "Missing mandatory fields in updating conversations";
        return;
    }

    QString groupId = eventData["_id"].toString();
    QJsonArray recentMessages = eventData["recentMessages"].toArray();

    groupLastMessagesFetchedCount[groupId] = recentMessages.size();

    if (recentMessages.empty())
        return;

    if (!groupConversations[groupId].empty()) {
        prependNewMessages(recentMessages, groupId);
        return;
    }

    if (recentMessages.size() == 20) {
        QPushButton *loadMoreMessagesButton = new QPushButton("Load more", this);
        QListWidgetItem *item = new QListWidgetItem(ui->messagesListWidget);

        item->setSizeHint(loadMoreMessagesButton->sizeHint());
        ui->messagesListWidget->setItemWidget(item, loadMoreMessagesButton);

        connect(loadMoreMessagesButton, &QPushButton::clicked, this, [this, groupId, recentMessages]() {
            emit fetchMessages(groupId, recentMessages.at(0).toObject()["timestamp"].toInteger());
        });
    }

    for(int i = 0; i < recentMessages.count(); i++) {
        QJsonObject messageObject = recentMessages.at(i).toObject();

        MessageWidget *widget = new MessageWidget(
            this,
            messageObject["_id"].toString(),
            messageObject["userId"].toString(),
            messageObject["content"].toString(),
            messageObject["timestamp"].toInteger()
            );

        groupConversations[groupId] << messageObject;

        QListWidgetItem *item = new QListWidgetItem(ui->messagesListWidget);

        item->setSizeHint(widget->sizeHint());
        ui->messagesListWidget->setItemWidget(item, widget);
    }

    ui->messagesListWidget->scrollToBottom();
}

void MainWindow::handleGroupChatNewMessage(QJsonObject eventData)
{
    if (eventData["groupId"].isNull() || eventData["timestamp"].isNull() ||
        eventData["timestamp"].toObject()["$numberLong"].isNull() || eventData["_id"].isNull() ||
        eventData["content"].isNull() || eventData["userId"].isNull()) {
        qDebug() << "Missing mandatory fields on new message!";
        return;
    }

    QString groupId = eventData["groupId"].toString();
    qint64 timestamp = eventData["timestamp"].toObject()["$numberLong"].toString().toLongLong();

    QJsonObject newMessage;

    newMessage.insert("_id", eventData["_id"].toString());
    newMessage.insert("content", eventData["content"].toString());
    newMessage.insert("timestamp", timestamp);
    newMessage.insert("userId", eventData["userId"].toString());

    groupConversations[groupId] << newMessage;

    for(int i = 0; i < ui->groupListWidget->count(); i++) {
        GroupWidget *groupWidget = qobject_cast<GroupWidget*>(ui->groupListWidget->itemWidget(ui->groupListWidget->item(i)));

        if (groupWidget->getId() == groupId) {
            groupWidget->setLastMessage(eventData["content"].toString());
            groupWidget->setTimestamp(timestamp);
            break;
        }
    }

    if (this->selectedGroupId == groupId) {
        QListWidgetItem *item = new QListWidgetItem(ui->messagesListWidget);

        MessageWidget *widget = new MessageWidget(
            this,
            eventData["_id"].toString(),
            eventData["userId"].toString(),
            eventData["content"].toString(),
            timestamp
            );

        item->setSizeHint(widget->sizeHint());
        ui->messagesListWidget->setItemWidget(item, widget);
        ui->messagesListWidget->scrollToBottom();
    }
}

QJsonDocument createJoinCallEvent(QJsonObject eventData) {
    QJsonObject event;

    event.insert("EVENT", eventToString(EventsUI::JOIN_CALL));
    event.insert("groupId", eventData["groupId"].toString());
    event.insert("callId", eventData["_id"].toString());
    event.insert("JWT", UserData::getInstance()->getJWT());

    QJsonDocument json(event);

    return json;
}

void MainWindow::handleInstantCall(QJsonObject eventData)
{
    if (eventData["groupId"].isNull() || eventData["_id"].isNull()) {
        qDebug() << "Missing mandatory fields on new instant call!";
        return;
    }

    if (!eventData["leaderId"].isNull() && eventData["leaderId"].toString() == UserData::getInstance()->getId() ) {
        qDebug() << "Leader joining the call immediately";

        QJsonDocument event = createJoinCallEvent(eventData);
        this->client->sendEvent(QString::fromUtf8(event.toJson(QJsonDocument::Indented)));
        return;
    }

    CallDialog* callDialog = new CallDialog(this);
    callDialog->exec();

    if(callDialog->result() == QDialog::Accepted) {
        qDebug() << "Call accepted!";

        QJsonDocument event = createJoinCallEvent(eventData);
        this->client->sendEvent(QString::fromUtf8(event.toJson(QJsonDocument::Indented)));
    }
    else {
        qDebug() << "Call declined!";
    }
}

void MainWindow::handleJoinCall(QJsonObject eventData)
{
    if (eventData["_id"].isNull() || eventData["channel"].isNull() || eventData["channel"].toString().isEmpty()) {
        qDebug() << "Missing mandatory fields on joining call!";
        return;
    }

    CallWindow* callWindow = new CallWindow(nullptr, &eventData);
    callWindow->exec();
}

void MainWindow::handleLogInFail(QJsonObject eventData)
{
    ui->backButton_2->setEnabled(true);
    ui->log_in_Button->setEnabled(true);

    ui->loading_gif_login->setMovie(nullptr);
    this->loadingGif->stop();

    ui->loading_gif_login->setStyleSheet("color: #AA1111");
    ui->loading_gif_login->setText(eventData["message"].toString());
}

void MainWindow::handleRegisterFail(QJsonObject eventData)
{
    ui->backButton->setEnabled(true);
    ui->register_Button->setEnabled(true);

    ui->loading_gif_register->setMovie(nullptr);
    this->loadingGif->stop();

    ui->loading_gif_register->setStyleSheet("color: #AA1111");
    ui->loading_gif_register->setText(eventData["message"].toString());
}

void MainWindow::handleRegisterSuccess(QJsonObject eventData)
{
    ui->backButton->setEnabled(true);
    ui->register_Button->setEnabled(true);

    ui->loading_gif_register->setMovie(nullptr);
    this->loadingGif->stop();

    ui->loading_gif_register->setStyleSheet("color: #11AA11");
    ui->loading_gif_register->setText(eventData["message"].toString());
}

void MainWindow::handleNewGroup(QJsonObject eventData)
{
    QListWidgetItem *item = new QListWidgetItem();

    GroupWidget *widget = new GroupWidget(
        this,
        eventData["id"].toString(),
        eventData["title"].toString(),
        "No messages yet.",
        0,
        eventData["owner"].toObject()["id"].toString()
        );

    item->setSizeHint(widget->sizeHint());

    ui->groupListWidget->insertItem(0, item);
    ui->groupListWidget->setItemWidget(item, widget);

    groupLastMessagesFetchedCount[eventData["id"].toString()] = 0;
}

void MainWindow::handleNewMembers(QJsonObject eventData)
{

}

void MainWindow::handleMemberRemoval(QJsonObject eventData)
{
    QJsonArray members = eventData["members"].toArray();

    if (members.empty())
        return;

    QString groupId = eventData["groupId"].toString();

    for (int i = 0; i < members.count(); ++i) {
        QString id = members[i].toObject()["id"].toString();

        if (id == UserData::getInstance()->getId()) {
            for (int j = 0; j < ui->groupListWidget->count(); ++j) {
                QListWidgetItem *item = ui->groupListWidget->item(j);
                GroupWidget *widget = qobject_cast<GroupWidget*>(ui->groupListWidget->itemWidget(item));

                if (widget->getId() == groupId) {
                    ui->groupListWidget->takeItem(j);
                    groupConversations.remove(groupId);
                }
            }
        }
    }

    emit updateMembersList(eventData);
}

void MainWindow::handleUpdateUI(UI_UpdateType type, QJsonObject eventData)
{
    qDebug() << "Updating UI";
    switch(type){
        case UI_UpdateType::AFTER_FETCH_SUCCESS:
            qDebug() << "Type: AFTER_FETCH_SUCCESS";
            handleProfileFetchUpdate();
            break;
        case UI_UpdateType::UPDATE_GROUP_CHAT:
            qDebug() << "Type: UPDATE_GROUP_CHAT";
            handleGroupChatConversationUpdate(eventData);
            break;
        case UI_UpdateType::NEW_MESSAGE_SUCCESS:
            qDebug() << "Type: NEW_MESSAGE_SUCCESS";
            handleGroupChatNewMessage(eventData);
            break;
        case UI_UpdateType::NEW_INSTANT_CALL:
            qDebug() << "Type: NEW_INSTANT_CALL";
            handleInstantCall(eventData);
            break;
        case UI_UpdateType::NEW_SCHEDULED_CALL:
            qDebug() << "Type: NEW_SCHEDULED_CALL";
            break;
        case UI_UpdateType::JOIN_CALL:
            qDebug() << "Type: JOIN_CALL";
            handleJoinCall(eventData);
            break;
        case UI_UpdateType::LOG_IN_FAILED:
            qDebug() << "Type: LOG_IN_FAILED";
            handleLogInFail(eventData);
            break;
        case UI_UpdateType::REGISTRATION_FAILED:
            qDebug() << "Type: REGISTRATION_FAILED";
            handleRegisterFail(eventData);
            break;
        case UI_UpdateType::REGISTRATION_SUCCEEDED:
            qDebug() << "Type: REGISTRATION_SUCCEEDED";
            handleRegisterSuccess(eventData);
            break;
        case UI_UpdateType::NEW_GROUP:
            qDebug() << "Type: NEW_GROUP";
            handleNewGroup(eventData);
            break;
        case UI_UpdateType::ADD_NEW_MEMBERS:
            qDebug() << "Type: ADD_NEW_MEMBERS";
            handleNewMembers(eventData);
            break;
        case UI_UpdateType::REMOVE_GROUP_MEMBERS:
            qDebug() << "Type: REMOVE_GROUP_MEMBERS";
            handleMemberRemoval(eventData);
            break;
        }
}

void MainWindow::sendEvent(QJsonDocument eventData)
{
    this->client->sendEvent(QString::fromUtf8(eventData.toJson(QJsonDocument::Indented)));
}

void MainWindow::triggerPassSearchResultDialog(QJsonObject eventData)
{
    emit passToSearchDialog(eventData);
}

void MainWindow::triggerPassToMembersDialog(QJsonObject eventData)
{
    emit passToMembersDialog(eventData);
}

void MainWindow::on_loginButton_clicked()
{
    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::LOG_IN_PAGE));
}


void MainWindow::on_registerButton_clicked()
{
    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::REGISTER_PAGE));
}


void MainWindow::on_backButton_clicked()
{
    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::MAIN_PAGE));
}


void MainWindow::on_backButton_2_clicked()
{
    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::MAIN_PAGE));
}


void MainWindow::on_log_in_Button_clicked()
{
    ui->backButton_2->setEnabled(false);
    ui->log_in_Button->setEnabled(false);
    ui->loading_gif_login->setMovie(this->loadingGif);
    this->loadingGif->start();

    QString username = ui->input_username_log_in->text();
    QString password = ui->input_password_log_in->text();

    if (username.length() < 4 || password.length() < 4 || username.length() > 16 || password.length() > 16) {
        ui->backButton_2->setEnabled(true);
        ui->log_in_Button->setEnabled(true);

        ui->loading_gif_login->setMovie(nullptr);
        this->loadingGif->stop();

        ui->loading_gif_login->setStyleSheet("color: #AA1111");
        ui->loading_gif_login->setText("Invalid username or password!");

        return;
    }

    QJsonObject event;
    event.insert("EVENT", eventToString(EventsUI::LOG_IN));
    event.insert("username", username);
    event.insert("password", password);

    QJsonDocument json(event);

    this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
}


void MainWindow::on_register_Button_clicked()
{
    ui->backButton->setEnabled(false);
    ui->register_Button->setEnabled(false);
    ui->loading_gif_register->setMovie(this->loadingGif);
    this->loadingGif->start();

    QString username = ui->input_username_register->text();
    QString password = ui->input_password_register->text();
    QString email = ui->input_email_register->text();
    QString firstName = ui->input_first_name_register->text();
    QString lastName = ui->input_last_name_register->text();

    if (username.length() < 4 || password.length() < 4 || email.length() < 4 || firstName.length() < 1 || lastName.length() < 1 ||
        username.length() > 16 || password.length() > 16 || email.length() > 32 || firstName.length() > 24 || lastName.length() > 24) {
        ui->backButton->setEnabled(true);
        ui->register_Button->setEnabled(true);

        ui->loading_gif_register->setMovie(nullptr);
        this->loadingGif->stop();

        ui->loading_gif_register->setStyleSheet("color: #AA1111");
        ui->loading_gif_register->setText("Invalid inputs!");

        return;
    }

    QJsonObject event;

    event.insert("EVENT", eventToString(EventsUI::REGISTER));
    event.insert("username", username);
    event.insert("password", password);
    event.insert("email", email);
    event.insert("firstName", firstName);
    event.insert("lastName", lastName);

    QJsonDocument json(event);

    this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
}


void MainWindow::on_profileButton_clicked()
{
    ui->stackedWidget_dashboard->setCurrentIndex(static_cast<int>(Tabs::PROFILE_TAB));
}


void MainWindow::on_chatsButton_clicked()
{
    ui->stackedWidget_dashboard->setCurrentIndex(static_cast<int>(Tabs::CHATS_TAB));
}


void MainWindow::on_groupListWidget_itemClicked(QListWidgetItem *item)
{
    if (item) {
        QWidget *widget = item->listWidget()->itemWidget(item);

        if (widget) {
            GroupWidget *groupWidget = qobject_cast<GroupWidget*>(widget);

            if (groupWidget) {
                ui->stackedWidget_groups->setCurrentIndex(static_cast<int>(GroupStatus::GROUP_SELECTED));
                this->selectedGroupId = groupWidget->getId();
                this->selectedGroupOwnerId = groupWidget->getOwnerId();
                ui->selectedGroupName_label->setText(groupWidget->getGroupName());

                if (ui->messagesListWidget->count() > 0)
                    ui->messagesListWidget->clear();

                QList<QJsonObject> messages = groupConversations[groupWidget->getId()];

                if (!messages.empty() && groupLastMessagesFetchedCount[groupWidget->getId()] == 20) {
                    QPushButton *loadMoreMessagesButton = new QPushButton("Load more", this);
                    QListWidgetItem *item = new QListWidgetItem(ui->messagesListWidget);

                    item->setSizeHint(loadMoreMessagesButton->sizeHint());
                    ui->messagesListWidget->setItemWidget(item, loadMoreMessagesButton);

                    connect(loadMoreMessagesButton, &QPushButton::clicked, this, [this, groupWidget, messages]() {
                        emit fetchMessages(groupWidget->getId(), messages.at(0)["timestamp"].toInteger());
                    });
                }

                for (int i = 0;i < messages.count(); i++) {
                    QListWidgetItem *item = new QListWidgetItem(ui->messagesListWidget);

                    MessageWidget *widget = new MessageWidget(
                        this,
                        messages[i]["_id"].toString(),
                        messages[i]["userId"].toString(),
                        messages[i]["content"].toString(),
                        messages[i]["timestamp"].toInteger()
                        );

                    item->setSizeHint(widget->sizeHint());
                    ui->messagesListWidget->setItemWidget(item, widget);
                }

                ui->messagesListWidget->scrollToBottom();

                if (!groupWidget->getOpenedStatus()) {
                    groupWidget->setOpenedStatus(true);
                    emit fetchMessages(groupWidget->getId(), groupWidget->getTimestmap());
                }

                if (UserData::getInstance()->getId() != groupWidget->getOwnerId()) {
                    ui->addMembersButton->hide();
                    this->roleInSelectedGroup = "REGULAR";
                }
                else {
                    ui->addMembersButton->show();
                    this->roleInSelectedGroup = "OWNER";
                }
            }
        }
    }
}


void MainWindow::on_sendMessageButton_clicked()
{
    QString messageContent = ui->messageTextEdit->toPlainText();

    if (messageContent == "")
        return;

    QJsonObject event;
    event.insert("EVENT", eventToString(EventsUI::NEW_MESSAGE));
    event.insert("content", messageContent);
    event.insert("groupId", this->selectedGroupId);
    event.insert("JWT", UserData::getInstance()->getJWT());

    QJsonDocument json(event);

    this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));

    ui->messageTextEdit->setText("");
}


void MainWindow::on_callButton_clicked()
{
    QJsonObject event;
    event.insert("EVENT", eventToString(EventsUI::NEW_CALL));
    event.insert("groupId", this->selectedGroupId);
    event.insert("type", callTypeToString(CallTypes::INSTANT));
    event.insert("JWT", UserData::getInstance()->getJWT());

    QJsonDocument json(event);

    this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
}


void MainWindow::on_createGroupButton_clicked()
{
    NewGroupDialog *newGroupDialog = new NewGroupDialog(this);
    QDialog::connect(this, &MainWindow::passToSearchDialog, newGroupDialog, &NewGroupDialog::triggerPassToSearchDialog);
    newGroupDialog->exec();

    if (newGroupDialog->result() == QDialog::Accepted) {
        qDebug() << "Creating group!";

        this->client->sendEvent(QString::fromUtf8(newGroupDialog->getEvent().toJson(QJsonDocument::Indented)));
    }
}


void MainWindow::on_membersButton_clicked()
{
    MembersListDialog *membersListDialog = new MembersListDialog(this,
                                                                 ui->selectedGroupName_label->text(),
                                                                 this->selectedGroupId,
                                                                 this->roleInSelectedGroup,
                                                                 this->selectedGroupOwnerId);
    QObject::connect(membersListDialog, &MembersListDialog::sendEvent, this, &MainWindow::sendEvent);
    QDialog::connect(this, &MainWindow::passToMembersDialog, membersListDialog, &MembersListDialog::updateMembersList);
    QDialog::connect(this, &MainWindow::updateMembersList, membersListDialog, &MembersListDialog::handleKickResult);
    membersListDialog->fetchMembers();
    membersListDialog->exec();
}


void MainWindow::on_addMembersButton_clicked()
{
    UserSearchDialog *userSearchDialog = new UserSearchDialog(this);
    QDialog::connect(this, &MainWindow::passToSearchDialog, userSearchDialog, &UserSearchDialog::updateResultsList);
    QObject::connect(userSearchDialog, &UserSearchDialog::sendEvent, this, &MainWindow::sendEvent);
    userSearchDialog->exec();

    if (userSearchDialog->result() == QDialog::Accepted) {
        QJsonArray members = userSearchDialog->getChosenUsers();

        QJsonObject event;
        event.insert("EVENT", eventToString(EventsUI::ADD_MEMBERS));
        event.insert("groupId", this->selectedGroupId);
        event.insert("members", members);
        event.insert("JWT", UserData::getInstance()->getJWT());

        QJsonDocument json(event);

        this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
    }
}

