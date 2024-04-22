#include "mainwindow.h"
#include "ui_mainwindow.h"

#include <QJsonArray>
#include <QThread>
#include <QTimer>
#include <UserData.h>
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
    REGISTER
};

QHash<QString, QList<MessageWidget*>> groupConversations;

QString eventToString(EventsUI event) {
    switch(event) {
        case EventsUI::LOG_IN:
            return "LOG_IN";
        case EventsUI::REGISTER:
            return "REGISTER";
    }
}

MainWindow::MainWindow(QWidget *parent, WSClient *client)
    : QMainWindow(parent)
    , ui(new Ui::MainWindow)
{
    ui->setupUi(this);

    ui->hiddenGroupId->setVisible(false);

    ui->stackedWidget->setCurrentIndex(static_cast<int>(Pages::MAIN_PAGE));
    ui->stackedWidget_dashboard->setCurrentIndex(static_cast<int>(Tabs::PROFILE_TAB));
    ui->stackedWidget_groups->setCurrentIndex(static_cast<int>(GroupStatus::NO_GROUP_SELECTED));
    this->client = client;

    ui->input_username_log_in->setText("costi1");
    ui->input_password_log_in->setText("1234");
}

MainWindow::~MainWindow()
{
    delete ui;
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

        GroupWidget *widget = new GroupWidget(
            this,
            currentGroup["id"].toString(),
            currentGroup["title"].toString(),
            isLastMessageNull ? "No messages yet." : lastMessage,
            isLastMessageNull ? 0 : secondsLong
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

void MainWindow::handleGroupChatConversationUpdate(QJsonObject eventData)
{
    QString groupId = eventData["_id"].toString();
    QJsonArray recentMessages = eventData["recentMessages"].toArray();

    for(int i = 0; i < recentMessages.count(); i++) {
        QJsonObject messageObject = recentMessages.at(i).toObject();

        MessageWidget *widget = new MessageWidget(
            this,
            messageObject["_id"].toString(),
            messageObject["userId"].toString(),
            messageObject["content"].toString(),
            messageObject["timestamp"].toInteger()
            );

        groupConversations[groupId] << widget;

        QListWidgetItem *item = new QListWidgetItem(ui->messagesListWidget);

        item->setSizeHint(widget->sizeHint());
        ui->messagesListWidget->setItemWidget(item, widget);
    }
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
    }
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

    QJsonObject event;
    event.insert("EVENT", eventToString(EventsUI::LOG_IN));
    event.insert("username", username);
    event.insert("password", password);

    QJsonDocument json(event);

    this->client->sendEvent(QString::fromUtf8(json.toJson(QJsonDocument::Indented)));
}


void MainWindow::on_register_Button_clicked()
{
    QString username = ui->input_username_register->text();
    QString password = ui->input_password_register->text();
    QString email = ui->input_email_register->text();
    QString firstName = ui->input_first_name_register->text();
    QString lastName = ui->input_last_name_register->text();

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

        if(widget) {
            GroupWidget *groupWidget = qobject_cast<GroupWidget*>(widget);

            if (groupWidget) {
                ui->stackedWidget_groups->setCurrentIndex(static_cast<int>(GroupStatus::GROUP_SELECTED));
                ui->hiddenGroupId->setText(groupWidget->getId());

                ui->messagesListWidget->clear();

                QList<MessageWidget*> messages = groupConversations[groupWidget->getId()];

                for(int i = 0;i < messages.count(); i++) {
                    QListWidgetItem *item = new QListWidgetItem(ui->messagesListWidget);

                    item->setSizeHint(messages[i]->sizeHint());
                    ui->messagesListWidget->setItemWidget(item, messages[i]);
                }

                if(!groupWidget->getOpenedStatus()) {
                    groupWidget->setOpenedStatus(true);
                    emit fetchMessages(groupWidget->getId(), groupWidget->getTimestmap());
                }
            }
        }
    }
}

