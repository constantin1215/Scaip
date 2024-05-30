#include "MembersListDialog.h"
#include "qjsondocument.h"
#include "qjsonobject.h"
#include "ui_MembersListDialog.h"

#include <UserData.h>
#include <UserWidget.h>
#include <mainwindow.h>

MembersListDialog::MembersListDialog(QWidget *parent, QString groupName, QString groupId, QString type)
    : QDialog(parent)
    , ui(new Ui::MembersListDialog)
{
    ui->setupUi(this);

    this->type = type;
    this->groupId = groupId;

    qDebug() << this->type;

    ui->labelTitle->setText(groupName + "'s members");
}

MembersListDialog::~MembersListDialog()
{
    QObject::disconnect(this);
    delete ui;
}

void MembersListDialog::fetchMembers()
{
    QJsonObject event;
    event.insert("EVENT", "FETCH_GROUP_MEMBERS");
    event.insert("groupId", groupId);
    event.insert("JWT", UserData::getInstance()->getJWT());

    QJsonDocument json(event);

    emit sendEvent(json);
}

void MembersListDialog::updateMembersList(QJsonObject eventData)
{
    qDebug() << "Loading members list!";

    QJsonArray members = eventData["members"].toArray();

    for(int i = 0; i < members.count(); i++) {
        QJsonObject member = members.at(i).toObject();

        UserWidget *widget = new UserWidget(this,
                                            member["_id"].toString(),
                                            member["username"].toString(),
                                            member["firstName"].toString(),
                                            member["lastName"].toString(),
                                            this->type == "OWNER" ? UserWidgetType::KICK : UserWidgetType::SIMPLE
                                            );

        QListWidgetItem *item = new QListWidgetItem(ui->listMembers);

        item->setSizeHint(widget->sizeHint());
        ui->listMembers->setItemWidget(item, widget);

        QObject::connect(widget, &UserWidget::kick, this, &MembersListDialog::handleKick);
    }
}

void MembersListDialog::handleKick(QString id)
{
    QJsonObject member;
    member.insert("id", id);

    QJsonArray members;
    members.append(member);

    QJsonObject event;
    event.insert("EVENT", "REMOVE_MEMBERS");
    event.insert("groupId", this->groupId);
    event.insert("members", members);
    event.insert("JWT", UserData::getInstance()->getJWT());

    qDebug() << event;

    QJsonDocument json(event);

    emit sendEvent(json);
}
